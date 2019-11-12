package operators.treeguiding;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import operators.TreeGuideUtils;

public class CCPTreeGuider extends TreeGuider {

	
	LinkedHashMap<String, Integer> treePosteriorDistributionMap;
	double scoreSum = 0;
	double maxLogProb = Double.NEGATIVE_INFINITY;
	final long burnin = 0;
	long totalNClades = 0;
	boolean hasConverged = false;
	double cachedEntropy = Double.POSITIVE_INFINITY;
	
	
	public CCPTreeGuider(Alignment alignment) {
		super(alignment);
	}
	
	

    @Override
	public void initAndValidate() {
    	
    	
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
    	scoreSum = 0;
    	maxLogProb = Double.NEGATIVE_INFINITY;
    	//if (totalNStates <= N_STATES_UNTIL_OPTIMISATION_CEASES) scoreCache.clear(); 
    	super.resetNeighbours();
    }
    
    
    @Override
	public void cacheCurrentStateTree(Tree tree) {
    	
    	super.cacheCurrentStateTree(tree);
    	if (hasConverged || totalNStates < burnin || totalNStates > N_STATES_UNTIL_OPTIMISATION_CEASES) return;
    	
    	if (totalNStates % 1000 == 0) {
    		
    		double entropy = computeEntropy();
    		if (Math.abs(entropy - cachedEntropy) < 0.0001) {
    			cachedEntropy = entropy;
    			hasConverged = true;
    			return;
    		}
    		cachedEntropy = entropy;
    	}
    	
    	
    	
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
    
    
    private double computeEntropy() {
    	
    	double entropy = 0;
    	for (String name: treePosteriorDistributionMap.keySet()){
            String key = name.toString();
            double prob = 1.0 * treePosteriorDistributionMap.get(key) / totalNClades;
            entropy += prob * Math.log(prob);
    	} 
    	
    	return -entropy;
    }
	
    
    
    // Calculates and caches conditional clade probability (CCP) of the tree
    @Override
    protected void computeUnnormalisedScore(Tree tree, String newick) {
    	
    	
    	double score = 0;
		
    	// Check cache for score (if the training process has been stopped)
		if (totalNStates <= N_STATES_UNTIL_OPTIMISATION_CEASES || !scoreCache.containsKey(newick)) {
			
			if (totalNStates < burnin) {
				score = 1;
			}
			
			else {
				
				
				// Compute non-normalised score and add to cache
		    	String[] clades = TreeGuideUtils.getClades(tree);
		    	for (int i = 0; i < clades.length; i ++) {
		    		
		    		// Count the number of times we have observed this conditional clade
		    		String clade = clades[i];
		    		if (clade == "" || clade == null) continue;
		    		
		    		int cladeCount =    treePosteriorDistributionMap.containsKey(clade) ?
		    							treePosteriorDistributionMap.get(clade) + 1:
		    							1;
		    		
		    		//score += Math.log(cladeCount) - Math.log(totalNStates);
		    		//score += Math.log(1.0 * cladeCount / totalNClades);
		    		score += Math.log(1.0 * cladeCount) - Math.log(totalNClades);
		    		//scoreSum *= 1.0 * cladeCount / totalNClades;
		    		
		
		    		if (totalNStates > N_STATES_UNTIL_OPTIMISATION_CEASES) scoreCache.put(newick, score);
		    		
		    	}
		    	
			}
	    	
	    	
		} 
		
		else {
			score = scoreCache.get(newick);
			
			// Remove and replace from hashmap so that it is at the end of the queue
			scoreCache.remove(newick);
			scoreCache.put(newick, score);
		}
    	
		maxLogProb = Math.max(maxLogProb, score);
		neighbourScores.add(score);
		scoreSum += score;
		
		
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
	public double getCacheSize() {
    	return cachedEntropy;
		//return treePosteriorDistributionMap.size();
	}
	
    
    
    @Override
    public double[] getProposalProbabilities() {
    	
    	
    	if (neighbours.size() == 0) return null;

    	// Normalise
    	double[] probabilities = new double[neighbours.size()];
    	double logSum = Math.log(scoreSum);
    	double scoreSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		double score = neighbourScores.get(i);
			probabilities[i] = Math.exp(score - maxLogProb);
			scoreSum += probabilities[i];
    	}
    	
    	
    	
    	
    	// Convert CCP counts into a cumulative probability array
    	double cumSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		cumSum += probabilities[i] / scoreSum;
			probabilities[i] = cumSum;
    	}
    	
    	
    	return probabilities;
    	
    }
	

	

}
