package operators.guidedtreeoperators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import beast.core.Input;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Tree;
import operators.treeguiding.TreeGuider;

public class GuidedTreeOperator extends TreeOperator {

	
	
	
	public GuidedTreeOperator() {
		
		// The tree will be inherited from the meta-operator
		treeInput.setRule(Input.Validate.FORBIDDEN);
	}
	
	protected Tree tree;
	
	protected Map<Tree, List<Tree>> neighbourCache = new HashMap<>();
	
	
	
	
	
	// A TreeGuider is required for computing tree scores
	// If 'treeIndex' is -1, returns a list of trees which may be reached from the current tree
	// If 'treeIndex' is >= 0, makes a proposal to move to the specified tree number
	public void getNeighbouringTrees(TreeGuider treeGuider, int treeIndex){
		
	}
	
	
	
	
	// Set the tree to move to
	public void setProposal(Tree proposedTree) {
		
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
