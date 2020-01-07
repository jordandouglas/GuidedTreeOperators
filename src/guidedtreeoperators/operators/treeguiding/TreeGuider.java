package guidedtreeoperators.operators.treeguiding;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import beast.core.BEASTObject;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import guidedtreeoperators.tools.TreeGuideUtils;

@Description("A class for sampling neighbouring trees, based off some tree scoring criterion. Can be used by MetaGuidedTreeOperator")
public class TreeGuider extends BEASTObject {
	
	final public Input<Double> neighbourSampleProbInput = new Input<>("prob", "Probability of a given neighbour being selected as a possible proposal (defaults to 1.0, all neighbours)", 1.0);
	final public Input<Integer> neighbourSampleCountInput = new Input<>("count", "Average number of neighbours in neighbourhood to consider (defaults to -1, all neighbours)", -1);
	
	
	
	final int[] dummy = new int[1];
	
	List<Integer> neighbourhoodToConsider = new ArrayList<Integer>();
	List<String> neighbours = new ArrayList<String>();
	List<Double> neighbourScores = new ArrayList<Double>();
	
	
	protected static final int MAX_CACHE_SIZE = 100000;
	
	
	final long N_STATES_UNTIL_OPTIMISATION_CEASES = 1000000;
	long totalNStates = 0;
	LinkedHashMap<String, Double> scoreCache;
		
	
	Tree currentTree;
	
	double neighbourSampleProb;
	double neighbourSampleCount;
	
	public TreeGuider() {
		
	}
	
	
	
	
	@Override
	public void initAndValidate() {
		
		neighbourSampleProb = neighbourSampleProbInput.get();
		
		assert neighbourSampleProb >= 0;
		assert neighbourSampleProb <= 1;
		
		neighbourSampleCount = neighbourSampleCountInput.get();
		
		
		scoreCache = new LinkedHashMap<String, Double>() {
		     protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
		    	 if (size() > MAX_CACHE_SIZE) {
		    		 //System.out.println("kicking");
		    	 }
		        return size() > MAX_CACHE_SIZE;
		     }
		};
	      
		
	}
	
	
	
	public void optimize(double delta) {
		
	}
	
	
	
	public void cacheCurrentStateTree(Tree tree) {
		totalNStates++;
	}
	
	
	public void setTree(Tree tree) {
		this.currentTree = tree;
		this.resetNeighbours();
	}
	
	
	public String getNewick() {
		if (this.currentTree == null) return "";
		return TreeGuideUtils.serialiseNode(this.currentTree.getRoot());
	}
	
	
	public List<String> getNeighbours(){
		return neighbours;
	}
	

	
	// Resets the list of neighbours of the current tree
	protected void resetNeighbours() {
		neighbours.clear();
		neighbourScores.clear();
	}
	
	
	

	// Decided which neighbours to sample by index
	public void sampleNeighbourhood(int numberOfNeighbours) {
		
		neighbourhoodToConsider = new ArrayList<>();
		
		
		// Case 1: all neighbours are considered
		if (neighbourSampleProb == 1 && neighbourSampleCount < 0) {
			for (int i = 0; i < numberOfNeighbours; i ++) neighbourhoodToConsider.add(i);
		}
		
		
		// Case 2: binomially sample each neighbour with probability p
		else if (neighbourSampleProb < 1) {
			for (int i = 0; i < numberOfNeighbours; i ++) {
				if (Randomizer.nextFloat() < neighbourSampleProb) neighbourhoodToConsider.add(i);
			}
		}
		
		
		// Case 3: binomially sample each neighbour with probability p = n/numberOfNeighbours
		else {
			double p = neighbourSampleCount*1.0 / numberOfNeighbours;
			for (int i = 0; i < numberOfNeighbours; i ++) {
				if (Randomizer.nextFloat() < p) neighbourhoodToConsider.add(i);
			}
			
		}
		
		//System.out.println("Sampled " + neighbourhoodToConsider.size() + " out of " + numberOfNeighbours);
		
		
	}
	

	
	
	// Adds the neighbouring tree to the list of neighbours and computes its score
	public void addNeighbouringTree(Tree neighbour, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
		
		
		
		// Add newick to list
		String newick = TreeGuideUtils.serialiseNode(neighbour.getRoot());
		neighbours.add(newick);
		
		// Compute score
		double score = computeUnnormalisedScore(neighbour, newick, nodeBeingMovedNr, nodeBeingMoveToNr);
		neighbourScores.add(score);
		
	
		
		
	}
	
	
	
	// Calculates and caches the unnormalised score of a single tree
	protected double computeUnnormalisedScore(Tree tree, String newick, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
		return 1;
	}
	
	
	
	// Iterates through all neighbours, and normalises their scores to return a cumulative
	// probability array
	public double[] getProposalProbabilities() {
		
		if (neighbours.size() == 0) return null;
		
		double[] probabilities = new double[neighbours.size()];
		
		
		double scoreSum = 0;
		for (int i = 0; i < neighbours.size(); i ++) {
			
			// Check if the neighbour is within the subset of the neighbourhood that we are considering for the proposal
			boolean goodNeighbour = false;
			for (int j = 0; j < neighbourhoodToConsider.size(); j ++) {
				if (i == neighbourhoodToConsider.get(j)) {
					goodNeighbour = true;
					break;
				}
			}
			
			if (goodNeighbour) scoreSum += neighbourScores.get(i);
			
		}

		
		// Convert scores into a cumulative probability array
		double cumSum = 0;
		for (int i = 0; i < neighbours.size(); i ++) {
			
			// Check if the neighbour is within the subset of the neighbourhood that we are considering for the proposal
			boolean goodNeighbour = false;
			for (int j = 0; j < neighbourhoodToConsider.size(); j ++) {
				if (i == neighbourhoodToConsider.get(j)) {
					goodNeighbour = true;
					break;
				}
			}
			
			
			if (!goodNeighbour) probabilities[i] = 0;
			else if (scoreSum <= 0) probabilities[i] = 1.0 / neighbours.size();
			else {
				cumSum += neighbourScores.get(i) / scoreSum;
				probabilities[i] = cumSum;
			}
		}
		
		
		return probabilities;
	}
	
	
	
	// Sums up the probability of transiting to this tree across all neighbouring states
	// Some neighbours may be the same so it is important to keep searching 
	// even after finding the first match
	public double getProposalDensity(String treeNewick, double[] proposalProbabilities) {
		
		if (neighbours.size() == 0) return Double.NEGATIVE_INFINITY;
		
		
		
		// Probability of sampling neighbour given that it is in the 'good neighbourhood'
		double proposalDensity = 0;
		int numMatches = 0;
		for (int i = 0; i < neighbours.size(); i ++) {
			
			if (treeNewick.contentEquals(neighbours.get(i))){
				
				double  probability = i == 0 ?
						proposalProbabilities[i] :
						proposalProbabilities[i] - proposalProbabilities[i-1];
				proposalDensity += Math.log(probability);
				numMatches++;
			}
			
		}
		
		
		// Probability of the tree being in the good neighbourhood
		
		
		if (numMatches == 0) return Double.NEGATIVE_INFINITY;
		return proposalDensity;
	}
	
	
	


	public double getCacheSize() {
		return scoreCache.size();
	}



	
	/**
	 * Returns the sorted newick of a tree
	 * This newick is topology specific and independent of branch lengths and other annotations
	 * If serialising repeatedly, serialise the tree first using serialiseTree to avoid allocating
	 * memory on each call of this function
	 * @param tree
	 * @return
	 */
	
	/*
	public String getTreeSerial(Tree tree) {
		Node root = tree.getRoot();
		SerialisedNode serialRoot;
		if (root instanceof SerialisedNode) serialRoot = (SerialisedNode) root;
		else serialRoot = new SerialisedNode(root);
		String newick = serialRoot.toSortedNewick(dummy, false);
		return newick;
	}
	
	
	public Tree serialiseTree(Tree tree) {
		SerialisedNode newRoot = new SerialisedNode(tree.getRoot()); //new SerialisedNode(tree.getRoot());
		Tree serialisedTree = new Tree(newRoot);
		serialisedTree.initAndValidate();
		return serialisedTree;
	}
	*/
	
	
	

}
