package guidedtreeoperators.operators.treeguiding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.core.util.ESS;
import beast.core.util.Log;
import beast.core.Input.Validate;
import beast.evolution.tree.Tree;
import guidedtreeoperators.tools.TreeGuideUtils;

@Description("A class for sampling neighbouring trees using conditional clade probability (CCP)")
public class CCPTreeGuider extends TreeGuider {

	final public Input<List<Function>> paramsInput = new Input<>("param",
            "Parameters which much have an ESS above some threshold before this operator begins optimisation.",
            new ArrayList<>(), Validate.OPTIONAL);
	
	final public Input<Double> ESSminInput = new Input<>("ESS", "The ESS of each of the provided parameters must"
			+ "exceed this threshold before training begins", 50.0);
	
	final public Input<Boolean> moderateProbsInput = new Input<>("moderate",
            "Use the clade posterior probabilities [false], or 0.5 - abs(p - 0.5) [true], to target clades which have intermediate values",  false);
	
	
	List<List<Double>> traces;
	
	
	
	ESS ESSutil = new ESS();
	
	LinkedHashMap<String, Integer> treePosteriorDistributionMap;
	double maxLogProb = Double.NEGATIVE_INFINITY;
	long totalNClades = 0;
	boolean hasConverged = false;
	boolean stopTraining = false;
	boolean moderateProbs = false;
	
	long numStatesSinceConvergence = 0;
	
	List<Function> parameters;
	double ESSmin;
	
	
	
	
	public CCPTreeGuider() {
		
	}
	
	@Override
	public double getCacheSize() {
		return treePosteriorDistributionMap.size();
	}
	
	

    @Override
	public void initAndValidate() {
    	
    	parameters = paramsInput.get();
    	ESSmin = ESSminInput.get();
    	moderateProbs = moderateProbsInput.get();
    	
    	if (parameters.size() > 0) {
    		traces = new ArrayList(parameters.size());
	    	for (int i = 0; i < parameters.size(); i ++) {
	    		assert parameters.get(i).getDimension() == 1;
	    		List<Double> trace = new ArrayList();
	    		traces.add(trace);
	    	}
    	}
    	
    	treePosteriorDistributionMap = new LinkedHashMap<String, Integer>() {
	         protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
	            return size() > MAX_CACHE_SIZE;
	         }
	      };
	      super.initAndValidate();
	}
    

    @Override
    protected void resetNeighbours() {
    	
    	/*
    	if (totalNStates == N_STATES_UNTIL_OPTIMISATION_CEASES) {
	    	for (String name: treePosteriorDistributionMap.keySet()){
	            String key = name.toString();
	            String value = treePosteriorDistributionMap.get(name).toString();  
	            System.out.println(key + " " + value);  
	    	} 
    	}
    	*/
    	// Clear the score cache every time during the training process
    	maxLogProb = Double.NEGATIVE_INFINITY;
    	//if (totalNStates <= N_STATES_UNTIL_OPTIMISATION_CEASES) scoreCache.clear(); 
    	super.resetNeighbours();
    }
    
    
    @Override
	public void cacheCurrentStateTree(Tree tree) {
    	
    	
    	super.cacheCurrentStateTree(tree);
    	
    	if (hasConverged) numStatesSinceConvergence ++;
    	if (!stopTraining && numStatesSinceConvergence > N_STATES_UNTIL_OPTIMISATION_CEASES) {
    		Log.warning("CCPTreeGuider: finished CCP training");
    		stopTraining = true;
    	}
    	if (stopTraining) return;
    	
    	
    	if (!hasConverged) {
	    	
	    	// Update all relevant ESSes
	    	for (int i = 0; i < parameters.size(); i ++) {
	    		
	    		List<Double> trace = traces.get(i);
	    		final double value = parameters.get(i).getArrayValue();
	    		trace.add(value);
	    	}
	    	
	    	
	    	
	    	if (totalNStates % 1000 == 0) {
	
	    		
	    		// Check that the ESS of all parameters has exceeded the threshold
	    		boolean aboveThreshold = true;
		    	for (int i = 0; i < parameters.size(); i ++) {
		    		
		    		List<Double> trace = traces.get(i);
		    		double ESS = ESSutil.calcESS(trace);
		    		if (ESS < ESSmin) {
		    			aboveThreshold = false;
		    			break;
		    		}
		    		
		    	}
	    		
	    		
		    	if (aboveThreshold) {
		    		hasConverged = true;
		    		numStatesSinceConvergence = 0;
		    		Log.warning("CCPTreeGuider: starting CCP training");
		    	}
		    	
	    	}
    	
    	}
    	
    	if (!hasConverged) return;
    	
    	
    	
    	String[] clades = TreeGuideUtils.getClades(tree);
		
    	// Clade probabilities (not conditional clade probabilities at this stage)
    	for (int i = 0; i < clades.length; i ++) {
    		String clade = clades[i];
    		if (clade == "" || clade == null) continue;
    		totalNClades ++;
    		if (treePosteriorDistributionMap.containsKey(clade)) {
    			int count = treePosteriorDistributionMap.get(clade);
    			treePosteriorDistributionMap.remove(clade);
    			treePosteriorDistributionMap.put(clade, count+1);
        	}
        	else {
        		treePosteriorDistributionMap.put(clade, 1);
        	}
    		
    	}
    	
    	// Increment the posterior distribution of trees by the state number difference since last caching
    	//String newick = TreeGuideUtils.serialiseNode(tree.getRoot());
    	
	}

    
    // Calculates and caches conditional clade probability (CCP) of the tree
    @Override
    protected double computeUnnormalisedScore(Tree tree, String newick, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
    	
    	
    	double score = 0;
		
    	
    	// Before convergence, set all scores to 1
    	if (!hasConverged) {
    		score = 0;
    	}
    	
    	// Calculate the CCP during/after the training process
    	else if (!stopTraining || !scoreCache.containsKey(newick)) {
			
				
			// Compute non-normalised score and add to cache
	    	String[] clades = TreeGuideUtils.getClades(tree);
	    	for (int i = 0; i < clades.length; i ++) {
	    		
	    		// Count the number of times we have observed this conditional clade
	    		String clade = clades[i];
	    		if (clade == "" || clade == null) continue;
	    		
	    		double cladeCount =  treePosteriorDistributionMap.containsKey(clade) ?
	    							 treePosteriorDistributionMap.get(clade) + 1:
	    							 1;
	    		
	    		if (moderateProbs) {
	    			cladeCount = 0.5 - Math.abs(cladeCount/totalNClades - 0.5);
	    		}
	    		
	    		
	    		//score += Math.log(cladeCount) - Math.log(totalNStates);
	    		//score += Math.log(1.0 * cladeCount / totalNClades);
	    		double CCP = Math.log(cladeCount);
	    		//scoreSum *= 1.0 * cladeCount / totalNClades;
	    		
	    		

	    		
	    		score += CCP;
	    		
	
	    		if (stopTraining) scoreCache.put(newick, score);
	    		
	    	}
	    	
    	
	    	
		} 
		
    	// After the training process and the score is already in the cache
		else {
			score = scoreCache.get(newick);
			
			// Remove and replace from hashmap so that it is at the end of the queue
			scoreCache.remove(newick);
			scoreCache.put(newick, score);
		}
    	
		maxLogProb = Math.max(maxLogProb, score);
		
		return score;
		
		
	}
    
    /*
    private double calculateCCP(Tree tree, String newick) {
    	
    	int nobs = 1; // Pseudocount
    	if (treePosteriorDistributionMap.containsKey(newick)) {
    		nobs += treePosteriorDistributionMap.get(newick);
    	}
    	
    	return nobs;
    	
    }
    */
    
    

    
    
    @Override
    public double[] getProposalProbabilities() {
    	
    	
    	if (neighbours.size() == 0) return null;

    	// Normalise
    	double[] probabilities = new double[neighbours.size()];
    	double scoreSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		double score = neighbourScores.get(i);
			probabilities[i] = Math.exp(score - maxLogProb);
			scoreSum += probabilities[i];
    	}
    	
    	
    	// Convert CCP counts into a cumulative probability array
    	double cumSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		if (scoreSum <= 0) probabilities[i] = 1.0 / neighbours.size();
			else {
	    		cumSum += probabilities[i] / scoreSum;
	    		probabilities[i] = cumSum;
			}
    	}
    	
    	
    	return probabilities;
    	
    }
	

	

}
