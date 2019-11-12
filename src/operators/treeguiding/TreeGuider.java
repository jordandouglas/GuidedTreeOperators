package operators.treeguiding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import beast.evolution.alignment.Alignment;
import beast.evolution.tree.Tree;
import operators.TreeGuideUtils;

public class TreeGuider {
	
	
	final int[] dummy = new int[1];
	List<String> neighbours = new ArrayList<String>();
	List<Double> neighbourScores = new ArrayList<Double>();
	
	protected static final int MAX_CACHE_SIZE = 10000;
	
	
	final long N_STATES_UNTIL_OPTIMISATION_CEASES = 100000000;
	long totalNStates = 0;
	LinkedHashMap<String, Double> scoreCache;
		
	Alignment alignment;
	Tree currentTree;
	
	
	
	public TreeGuider(Alignment alignment) {
		this.alignment = alignment;
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
	
	
	
	public void initAndValidate() {
		
		
	     scoreCache = new LinkedHashMap<String, Double>() {
	         protected boolean removeEldestEntry(Map.Entry<String, Double> eldest) {
	        	 if (size() > MAX_CACHE_SIZE) {
	        		 //System.out.println("kicking");
	        	 }
	            return size() > MAX_CACHE_SIZE;
	         }
	      };
	      
	      
		
	}
	
	
	// Resets the list of neighbours of the current tree
	protected void resetNeighbours() {
		neighbours.clear();
		neighbourScores.clear();
	}
	
	// Adds the neighbouring tree to the list of neighbours and computes its score
	public void addNeighbouringTree(Tree neighbour) {
		
		// Add newick to list
		String newick = TreeGuideUtils.serialiseNode(neighbour.getRoot());
		neighbours.add(newick);
		
		computeUnnormalisedScore(neighbour, newick);
		
	}
	
	
	// Iterates through all neighbours, and normalises their scores to return a cumulative
	// probability array
	public double[] getProposalProbabilities() {
		if (neighbours.size() == 0) return null;
		double[] probabilities = new double[neighbours.size()];
		for (int i = 0; i < neighbours.size(); i ++) {
			probabilities[i] = (i + 1.0) / neighbours.size();
		}
		return probabilities;
	}
	
	
	
	// Sums up the probability of transiting to this tree across all neighbouring states
	// Some neighbours may be the same so it is important to keep searching 
	// even after finding the first match
	public double getProposalDensity(String treeNewick, double[] proposalProbabilities) {
		
		if (neighbours.size() == 0) return Double.NEGATIVE_INFINITY;
		
		
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
		
		if (numMatches == 0) return Double.NEGATIVE_INFINITY;
		return proposalDensity;
	}
	
	
	
	
	// Calculates and caches the unnormalised score of a single tree
	protected void computeUnnormalisedScore(Tree tree, String newick) {
		
		
		// Check cache for score
		if (!scoreCache.containsKey(newick)) {
			
			// Compute non-normalised score and add to cache
			double score = getScore(tree, newick);
			neighbourScores.add(score);
			if (scoreCache.size() <= MAX_CACHE_SIZE) scoreCache.put(newick, score);
		}
		
	}
	
	
	protected double getScore(Tree tree, String newick) {
		return 1;
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
