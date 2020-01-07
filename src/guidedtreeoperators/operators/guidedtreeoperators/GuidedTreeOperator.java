package guidedtreeoperators.operators.guidedtreeoperators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.Input;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Tree;
import guidedtreeoperators.operators.treeguiding.TreeGuider;

public class GuidedTreeOperator extends TreeOperator {

	
	
	
	public GuidedTreeOperator() {
		
		// The tree will be inherited from the meta-operator
		treeInput.setRule(Input.Validate.FORBIDDEN);
	}
	
	protected Tree tree;
	
	protected Map<Tree, List<Tree>> neighbourCache = new HashMap<>();
	
	
	
	
	/*
		Generates neighbours
			- If treeGuider is null and treeIndices >= 0, the specified tree will be proposed
			- If treeGuider is null and treeIndices < 0, nothing will change
			- If treeGuider is not null, all neighbours are generated added to the treeGuider 
		Returns the number of neighbours generated
	*/
	public int getNeighbouringTrees(TreeGuider treeGuider, int treeIndex){
		return 0;
	}
	
	
	// Counts the number of neighbours
	public int sampleNeighbourhood(TreeGuider treeGuider){
		int numberOfNeighbours = getNeighbouringTrees(null, -1);
		if (treeGuider != null) treeGuider.sampleNeighbourhood(numberOfNeighbours);
		return numberOfNeighbours;
	}

	
	public void setTree(Tree metaTree) {
		this.tree = metaTree;
	}
	

	@Override
	public void initAndValidate() {
		

		
	}


	@Override
	public double proposal() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	// Perform operator to acquire the given tree index, and return the log JD
	public double proposal(int treeIndex) {
		return 0;
	}
	


}
