package inference;


import beast.app.treeannotator.TreeAnnotator;
import beast.app.treeannotator.TreeAnnotator.MemoryFriendlyTreeSet;
import beast.core.*;
import beast.core.Input.Validate;
import beast.core.Logger.LOGMODE;
import beast.core.util.Log;
import beast.evolution.tree.CladeSet;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import beast.util.XMLParser;
import beast.util.XMLProducer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



@Description("Coupled MCMC. Two independent MCMC chains which will only stop after their clade posteriors have converged" +
		"" +
		"Note that log file names should have $(seed) in their name so " +
		"that the first chain uses the actual seed in the file name and all subsequent chains add one to it." +
		"Furthermore, the log and tree log should have the same sample frequency.")
public class IndependentMCMC extends MCMC {
	
	
	public final Input<Integer> checkForConvergenceEveryInput = new Input<Integer>("checkEvery", "number of samples in between checking for convergence", 100000);
	public final Input<Double> thresholdInput = new Input<Double>("threshold", "maximum difference in clade probability required to declare convergence", 0.1);
	public final Input<Integer> convergenceLengthInput = new Input<Integer>("convergedFor", "maximum difference in clade probability must be less than threshold "
			+ "										this many checks in a row before convergence is declared and the chains are stopped", 3);
	public final Input<String> tempDirInput = new Input<>("tempDir","directory where temporary files are written", "/tmp/");
	
	public final Input<List<TreeStoreLogger>> treeStorersInput = new Input<>("treeStorer", "list of tree loggers to check convergence with", new ArrayList<TreeStoreLogger>());
	
	public final Input<Integer> storeTreeEveryInput = new Input<Integer>("storeTreeEvery", "how often to store the current tree for checking convergence", 20000);
	
	
	
	
	
	// How often to check for convergence
	int checkForConvergenceEvery;
	
	// Maximum difference in clade probability must be less than threshold to converge
	double threshold;
	
	// If the maximum difference in clade probability must be less than threshold this many checks 
	// in a row, then the chain is ended
	int convergenceLength;
	
	// How often to save the current tree
	int storeTreeEvery;
	
	// Threads
	Thread [] threads;
	
	// Keep track of time taken between logs to estimate speed
    long startLogTime;
    
    // MCMC chains
    ConvergableMCMC[] chains;
    
    // Trees
    List<TreeStoreLogger> treeStorers;
    int numTreesTotal;
	int numTrees;
	List<LinkedHashMap<String, Integer>> cladeMapList1 = new ArrayList<LinkedHashMap<String, Integer>>();
	List<LinkedHashMap<String, Integer>> cladeMapList2 = new ArrayList<LinkedHashMap<String, Integer>>();
	List<LinkedHashMap<String, Boolean>> allClades = new ArrayList<LinkedHashMap<String, Boolean>>();
	

	// keep track of when threads finish in order to optimise thread usage
	long [] finishTimes;

	List<StateNode> tmpStateNodes;

	@Override
	public void initAndValidate() {
		
		
		// Do not allow the user to specify chain length
		//chainLengthInput.setRule(Validate.FORBIDDEN);
		if (this.getID() == null) throw new RuntimeException("Please provide an ID for IndependentMCMC");
		
		
		// Parse user settings
		checkForConvergenceEvery = checkForConvergenceEveryInput.get();
		if (checkForConvergenceEvery < 1) throw new RuntimeException("checkEvery must be at least 1");
		threshold = thresholdInput.get();
		convergenceLength = convergenceLengthInput.get();
		if (convergenceLength < 0) throw new RuntimeException("convergedFor must be at least 1");
		
		
		storeTreeEvery = storeTreeEveryInput.get();
		if (storeTreeEvery > checkForConvergenceEvery)  throw new RuntimeException("storeTreeEvery must be less than or equal to checkEvery");
		numTrees = 0;
		numTreesTotal = 0;
		
		// Create the MCMC objects
		chains = new ConvergableMCMC[2];

		
		// Tree storers
		treeStorers = treeStorersInput.get();
		if (treeStorers.size() == 0) Log.warning("WARNING: no tree storers have been specifed. Provide one or more"
				+ "tree storers so that clade posterior convergence can be detected.");
		
		
		cladeMapList1 = new ArrayList<LinkedHashMap<String, Integer>>();
		cladeMapList2 = new ArrayList<LinkedHashMap<String, Integer>>();
		allClades = new ArrayList<LinkedHashMap<String, Boolean>>();
		for (int c = 0; c < treeStorers.size(); c++) {
			cladeMapList1.add(new LinkedHashMap<String, Integer>());
			cladeMapList2.add(new LinkedHashMap<String, Integer>());
			allClades.add(new LinkedHashMap<String, Boolean>());
		}
		
		
		
		
		// the difference between the various chains is
		// 1. it runs an MCMC, not a  CoupledMCMC
		// 2. remove chains attribute
		// 3. output logs change for every chain
		// 4. log to stdout is removed to prevent clutter on stdout
		String sXML = new XMLProducer().toXML(this);
		
		//sXML = sXML.replaceAll("[<] .+id=['\"]" + this.getID() + "['\"].*[>]", "<HELLO />");
		//String runLine = "<run id=\"" + this.getID() + "\" spec=\"MCMC\" >"; 
		//sXML = sXML.replaceAll("<run .*id=['\"]" + this.getID() + "['\"].*>", runLine);
		

		
        String sMCMCMC = this.getClass().getName();
		while (sMCMCMC.length() > 0) {
			sXML = sXML.replaceAll("\\b"+IndependentMCMC.class.getName()+"\\b", ConvergableMCMC.class.getName());
			if (sMCMCMC.indexOf('.') >= 0) {
				sMCMCMC = sMCMCMC.substring(sMCMCMC.indexOf('.')+1);
			} else {
				sMCMCMC = "";
			}
		}
		
		
		
		long nSeed = Randomizer.getSeed();
		
		// Create new chains		
		for (int i = 0; i < chains.length; i++) {
			XMLParser parser = new XMLParser();
			String sXML2 = sXML;
			sXML2 = sXML2.replaceAll("\\$\\(seed\\)", nSeed+i+"");
			sXML2 = sXML2.replaceAll("\\$\\(chain\\)", "chain" + (i+1));
			
			
			// Add the tree storage loggers
			for (Logger logger : treeStorers) {
				String id = logger.getID();
				if (id == null) throw new RuntimeException("Please provide an ID for every treeStorer");
				sXML2 = sXML2.replaceAll("id=['\"]" + id + "['\"]", "id=\"" + id + "\" name=\"logger\"");
			}

			
			System.out.println(sXML2);
			
			try {
		        FileWriter outfile = new FileWriter(new File(tempDirInput.get() + IndependentMCMC.class.getName() + ".xml"));
		        outfile.write(sXML2);
		        outfile.close();
		        
		        
				
				chains[i] = (ConvergableMCMC) parser.parseFragment(sXML2, true);

	
				// Only allow chain 1 to screen log
				chains[i].allowScreenLogging(i == 0);
				
				// Initialise the MCMC
				chains[i].setChainNr(i+1);
				chains[i].setStateFile(stateFileName + "."  + (i+1), restoreFromFile);
				chains[i].run();
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	
		// reopen log files for main chain, which were closed at the end of run(); 
		//Logger.FILE_MODE = LogFileMode.resume;
		//for (Logger logger : m_chains[0].loggersInput.get()) {
		//	logger.init();
		//}
		
		// get a copy of the list of state nodes to facilitate swapping states
		tmpStateNodes = startStateInput.get().stateNodeInput.get();

		chainLength = chainLengthInput.get();
		finishTimes = new long[chains.length];
	} // initAndValidate
	
	
	
	class CoupledChainThread extends Thread {
		final int chainNr;
		CoupledChainThread(int chainNr) {
			this.chainNr = chainNr;
		}
		public void run() {
			try {
				finishTimes[chainNr] = chains[chainNr].runForNSteps(checkForConvergenceEvery);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override 
	public void run() throws IOException {
		
		int numberOfConvergedChecks = 0;
		
		long sampleNr = 0;
		while (numberOfConvergedChecks < convergenceLength) {
			long startTime = System.currentTimeMillis();
			
			// Start threads with individual chains here.
			threads = new Thread[chains.length];
			
			for (int k = 0; k < chains.length; k++) {
				threads[k] = new CoupledChainThread(k);
				threads[k].start();
			}
			
			sampleNr += checkForConvergenceEvery;

			// Wait for the chains to finish
	        startLogTime = System.currentTimeMillis();
			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			
			// Check for convergence
			double maximumCladePosteriorDelta = 0;
			String maxDeltaClade = "";
			for (int i = 0; i < treeStorers.size(); i ++) {
				
				// Get the ith tree file for either chain
				LinkedHashMap<String, Integer> cladeMap1 = cladeMapList1.get(i);
				LinkedHashMap<String, Integer> cladeMap2 = cladeMapList2.get(i);
				LinkedHashMap<String, Boolean> cladeList = allClades.get(i);
				
				
				// Chain 1
				TreeStoreLogger logger1 = chains[0].getTreeStoreLoggers(i);
				String fileName = logger1.getFileName();
				CladeSet cladeSet1 = getCladeSet(fileName);
				
				// Chain 2
				TreeStoreLogger logger2 = chains[1].getTreeStoreLoggers(i);
				fileName = logger2.getFileName();
				CladeSet cladeSet2 = getCladeSet(fileName);

				numTreesTotal += numTrees;
				

				// Update map of clades to support values in each set
				for (int c = 0; c < cladeSet1.getCladeCount(); c++) {
					String clade = cladeSet1.getClade(c);
					int support = cladeSet1.getFrequency(c);
					if (cladeMap1.containsKey(clade)) {
						cladeMap1.replace(clade, cladeMap1.get(clade) + support);
					}
					else cladeMap1.put(clade, support);
					
					if (!cladeList.containsKey(clade)) cladeList.put(clade, true);
					
				}
				
				
				for (int c = 0; c < cladeSet2.getCladeCount(); c++) {
					String clade = cladeSet2.getClade(c);
					int support = cladeSet2.getFrequency(c);
					if (cladeMap2.containsKey(clade)) {
						cladeMap2.replace(clade, cladeMap2.get(clade) + support);
					}
					else cladeMap2.put(clade, support);
					
					if (!cladeList.containsKey(clade)) cladeList.put(clade, true);
				}
				
				
				
				
				// Find maximum difference
				for (String clade : cladeList.keySet()) {
					
					double delta = 0;
					
					// Clade only in chain 2
					if (!cladeMap1.containsKey(clade)) {
						double support = 1.0 * cladeMap2.get(clade)/numTreesTotal;
						delta = support;
					}
					
					// Clade only in chain 1
					else if (!cladeMap2.containsKey(clade)) {
						double support = 1.0 * cladeMap1.get(clade)/numTreesTotal;
						delta = support;
					}
					
					// Clade in both chains
					else {
						double support1 = 1.0 * cladeMap1.get(clade)/numTreesTotal;
						double support2 = 1.0 * cladeMap2.get(clade)/numTreesTotal;
						delta = Math.abs(support1 - support2);
					}

					if (delta > maximumCladePosteriorDelta) {
						maximumCladePosteriorDelta = delta;
						maxDeltaClade = clade;
					}
				}
				
				
				// Reset the loggers 
				logger1.init();
				logger2.init();
				
			}
			
			Log.warning("\tIndependentMCMC (" + sampleNr + "): the maximum clade probability difference is " + maximumCladePosteriorDelta);
			//Log.warning("\t maxDeltaClade: " + maxDeltaClade + " numTreesTotal " + numTreesTotal + " numClades " + allClades.get(0).keySet().size());
			
			boolean thresholdMet = false;
			if (thresholdMet) {
				numberOfConvergedChecks ++;
			}else {
				numberOfConvergedChecks = 0;
			}

		}
		
		Log.warning("\tIndependentMCMC: convergence detected, chain stopping. Total number of states: " + sampleNr );
		

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// ignore
		}
	} // run
	
	
	
	
	protected CladeSet getCladeSet(String path) throws IOException {
		//Log.warning("Processing " + path);
		MemoryFriendlyTreeSet srcTreeSet = new TreeAnnotator().new MemoryFriendlyTreeSet(path, 0);
		srcTreeSet.reset();
		Tree tree = srcTreeSet.next();
		CladeSet cladeSet1 = new CladeSet(tree);
		numTrees = 0;
		while (srcTreeSet.hasNext()) {
			tree = srcTreeSet.next();
			cladeSet1.add(tree);
			numTrees++;
		}
		return cladeSet1;
	}

	
	
} // class MCMCMC



