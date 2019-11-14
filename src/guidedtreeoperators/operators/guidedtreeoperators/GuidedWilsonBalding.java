package guidedtreeoperators.operators.guidedtreeoperators;

import java.util.List;

import beast.evolution.operators.TreeOperator;
import beast.evolution.operators.WilsonBalding;
import beast.evolution.tree.Tree;
import guidedtreeoperators.operators.treeguiding.TreeGuider;

public class GuidedWilsonBalding extends GuidedTreeOperator {


	@Override
	public void initAndValidate() {
		// TODO Auto-generated method stub
		super.initAndValidate();
		
	}


	@Override
	public double proposal() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	@Override
	public void getNeighbouringTrees(TreeGuider treeGuider, int treeIndex) {
		// TODO Auto-generated method stub
	}

	
	
	@Override
	public void setProposal(Tree proposedTree) {
		// TODO Auto-generated method stub
		
	}



}
