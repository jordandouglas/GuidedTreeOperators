package guidedtreeoperators.operators.treeguiding;


import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import beast.parsimony.FitchParsimony;


@Description("A class for sampling neighbouring trees, based off parsimony scores")
public class ParsimonyTreeGuider extends TreeGuider {

	
	public final Input<Double> warpFactorInput = new Input<>("warpFactor", "Warp factor", 0.5);
	public final Input<Double> pseudocountInput = new Input<>("pseudocount", "Parsimony pseudocount", 0.1);
	public final Input<Alignment> dataInput = new Input<>("data", "Multiple sequence alignment for computing parsimony scores", Input.Validate.REQUIRED);
	
	
    public ParsimonyTreeGuider() {

	}


    double minScore;
    double maxScore;
    double baseFactor = 0.9;
    double warpfactor;
    
    
	FitchParsimony fitch;
    Tree tree;
    final int NUM_TREE_SCORES_TO_CACHE = 100;
    double pseudocount;
    Alignment alignment;
    
    @Override
	public void initAndValidate() {
    	alignment = dataInput.get();
    	pseudocount = pseudocountInput.get();
    	fitch = new FitchParsimony(alignment, false);
    	warpfactor = warpFactorInput.get();
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
    protected double computeUnnormalisedScore(Tree tree, String newick, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
    	
    	double score = 0;
    	
		// Check cache for score
		if (!scoreCache.containsKey(newick)) {
			
			// Compute non-normalised score and add to cache
			
			fitch.reset();
	    	score = fitch.getScore(tree);

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
		
		return score;
	}
    
    
    @Override
	public void optimize(double delta) {
    	
    	// Larger warp factors correspond to greater weight behind parsimony scores
    	delta += Math.log(warpfactor);
    	warpfactor = Math.exp(delta);
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
			// if (maxScore != minScore) newScore = 1 - (score - minScore) / (maxScore - minScore) + pseudocount;
			if (maxScore != minScore) newScore = (score - minScore) / (maxScore - minScore) + pseudocount;
			newScore = Math.pow(baseFactor, warpfactor*newScore);
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
