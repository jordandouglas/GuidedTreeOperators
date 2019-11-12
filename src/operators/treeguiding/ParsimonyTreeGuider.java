package operators.treeguiding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import beast.parsimony.FitchParsimony;

public class ParsimonyTreeGuider extends TreeGuider {

	
    public ParsimonyTreeGuider(Alignment alignment) {
		super(alignment);
		fitch = new FitchParsimony(alignment, false);
	}


    double minScore;
    double maxScore;
    
	FitchParsimony fitch;
    Tree tree;
    final int NUM_TREE_SCORES_TO_CACHE = 100;
    final double PSEUDOCOUNT = 0.1;
    
    @Override
	public void initAndValidate() {
    	super.initAndValidate();
	}
    
    
    @Override
	public void cacheCurrentStateTree(Tree tree) {
    	
    	// Reset the parsimony score cache after a burn-in period
    	if (totalNStates == N_STATES_UNTIL_OPTIMISATION_CEASES) {
    		//System.out.println("Resetting parsimony cache");
    		scoreCache.clear();
    	}

    	super.cacheCurrentStateTree(tree);
    	
	}
    

    @Override
    protected void resetNeighbours() {
    	minScore = Double.POSITIVE_INFINITY;
    	maxScore = 0;
    	super.resetNeighbours();
    }
    
    
    // Calculates Fitch parsimony score of the tree
    @Override
    protected void computeUnnormalisedScore(Tree tree, String newick) {
    	
    	double score = 0;
    	
		// Check cache for score
		if (!scoreCache.containsKey(newick)) {
			
			// Compute non-normalised score and add to cache
			
			score = getScore(tree, newick);

			if (scoreCache.size() <= MAX_CACHE_SIZE) scoreCache.put(newick, score);
			else System.out.println("Parsimony cache full");
		} 
		
		else {
			score = scoreCache.get(newick);
			
			// Remove and replace from hashmap so that it is at the end of the queue
			scoreCache.remove(newick);
			scoreCache.put(newick, score);
			
		}
    	
		minScore = Math.min(minScore, score);
		maxScore = Math.max(maxScore, score);
		
		neighbourScores.add(score);
		
	}
    
    @Override
	protected double getScore(Tree tree, String newick) {
    	fitch.reset();
    	return fitch.getScore(tree);
	}
    
    
    @Override
    public double[] getProposalProbabilities() {
    	
    	
    	if (neighbours.size() == 0) return null;
    	
    	double[] probabilities = new double[neighbours.size()];
    	
    	// Normalise scores by subtracting minimum score
    	double scoreSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		double score = neighbourScores.get(i);
			double newScore = score;
			if (maxScore != minScore) newScore = 1 - (score - minScore) / (maxScore - minScore) + PSEUDOCOUNT;
			scoreSum += newScore;
			probabilities[i] = newScore;
    	}
    	
    	
    	// Convert scores into a cumulative probability array
    	double cumSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		cumSum += probabilities[i] / scoreSum;
    		probabilities[i] = cumSum;
    	}
    	
    	
    	return probabilities;
    	
    }
	

	
	
	
}
