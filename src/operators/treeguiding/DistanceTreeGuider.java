package operators.treeguiding;



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
	
	int scoreSum = 0;
	private DistanceProvider weightProvider;
    int leafNodeCount = 0;
    DistanceProvider.Data weights[];
    

	public DistanceTreeGuider() {
	}
	

    @Override
	public void initAndValidate() {
    	
    	weightProvider = weightsInput.get();
        if( weightProvider == null ) {
            weightProvider = DistanceProvider.uniform;
        }

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
    

    
    
    // Calculates and caches MDS distance to another tree tree
    @Override
    protected void computeUnnormalisedScore(Tree tree, String newick, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
        final double d = weightProvider.dist(weights[nodeBeingMovedNr], weights[nodeBeingMoveToNr]);
        scoreSum += 1 / d;
        neighbourScores.add(1 / d);
	}
    
    
    
    @Override
    public double[] getProposalProbabilities() {
    	
    	if (neighbours.size() == 0) return null;
    	double[] probabilities = new double[neighbours.size()];

    	// Convert scores into a cumulative probability array
    	double cumSum = 0;
    	for (int i = 0; i < neighbours.size(); i ++) {
    		cumSum += neighbourScores.get(i) / scoreSum;
    		probabilities[i] = cumSum;
    	}
    	
    	return probabilities;
    	
    }
	


}
