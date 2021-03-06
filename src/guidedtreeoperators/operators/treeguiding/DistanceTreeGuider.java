package guidedtreeoperators.operators.treeguiding;



import java.util.HashSet;
import java.util.Map;

import beast.core.Description;
import beast.core.Input;
import beast.evolution.alignment.Alignment;
import beast.evolution.operators.DistanceProvider;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;


@Description("A class for sampling neighbouring trees, based off distances between trees")
public class DistanceTreeGuider extends TreeGuider {
	
	
	public Input<DistanceProvider> weightsInput = new Input<>("weights", "Provide distances between clades (data, not tree based)", null, Input.Validate.OPTIONAL);
	public final Input<Double> warpFactorInput = new Input<>("warpFactor", "Warp factor", 1.0);
	
	
	
	int scoreSum = 0;
	private DistanceProvider weightProvider;
    int leafNodeCount = 0;
    double warpfactor;
    DistanceProvider.Data weights[];
    

	public DistanceTreeGuider() {
	}
	

    @Override
	public void initAndValidate() {
    	
    	weightProvider = weightsInput.get();
        if( weightProvider == null ) {
            weightProvider = DistanceProvider.uniform;
        }
        warpfactor = warpFactorInput.get();
		super.initAndValidate();
	}
    

    @Override
    protected void resetNeighbours() {
    	scoreSum = 0;
    	super.resetNeighbours();
    }
    
    
    @Override
	public void cacheCurrentStateTree(Tree tree) {
    	super.cacheCurrentStateTree(tree);

    	
    	// Recompute MDS only if the tree dimensions have changed
    	if (tree.getLeafNodeCount() == leafNodeCount) return;
    	leafNodeCount = tree.getLeafNodeCount();
    	
    	
        final int nc = tree.getNodeCount();
        weights = new DistanceProvider.Data[nc];
        final Map<String, DistanceProvider.Data> init = weightProvider.init(new HashSet<String>(tree.getTaxonset().asStringList()));
        for( Node tip : tree.getExternalNodes() ) {
            weights[tip.getNr()] = init.get(tip.getID());
        }
        for(int i = 0; i < nc; ++i) {
            final Node n = tree.getNode(i);
            if( !n.isLeaf() ) {
              weights[n.getNr()] = weightProvider.empty();
            }
        }
    	
    	
	}
    
    
    @Override
	public void optimize(double delta) {
    	
    	// Larger warp factors correspond to greater weights behind distance scores
    	delta += Math.log(warpfactor);
    	//System.out.println(" warp " + warpfactor + "  ->  " + Math.exp(delta));
    	warpfactor = Math.exp(delta);
    	
	}


    
    
    // Calculates and caches MDS distance to another tree tree
    @Override
    protected double computeUnnormalisedScore(Tree tree, String newick, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
        final double d = weightProvider.dist(weights[nodeBeingMovedNr], weights[nodeBeingMoveToNr]);
        double score = 1 / d;
        score = Math.exp(warpfactor*score);
        score = Math.min(score, 10E4); // Avoid infinity scores
        scoreSum += score;
        return score;
	}
    
    
    
    @Override
    public double[] getProposalProbabilities() {
    	
    	if (neighbours.size() == 0) return null;
    	double[] probabilities = new double[neighbours.size()];

    	// Convert scores into a cumulative probability array
    	double cumSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		if (scoreSum <= 0) probabilities[i] = 1.0 / neighbours.size();
			else {
	    		cumSum += neighbourScores.get(i) / scoreSum;
	    		probabilities[i] = cumSum;
			}
    	}
    	
    	return probabilities;
    	
    }
	


}
