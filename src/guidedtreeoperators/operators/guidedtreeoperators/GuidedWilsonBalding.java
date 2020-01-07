package guidedtreeoperators.operators.guidedtreeoperators;

import java.util.ArrayList;
import java.util.List;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.TreeOperator;
import beast.evolution.operators.WilsonBalding;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import guidedtreeoperators.operators.treeguiding.TreeGuider;
import guidedtreeoperators.tools.TreeGuideUtils;

public class GuidedWilsonBalding extends GuidedTreeOperator {


	
	public final  Input<RealParameter> rateInput = new Input<>("rates", "the rates associated with nodes in the tree for sampling of individual rates among branches.");
    
	
	RealParameter rates;
	private Node i, p, CiP, j, k, pP;
	private double tp, tC, tj, tk, tpP, rp, rC, rj;

	@Override
	public void initAndValidate() {
		rates = rateInput.get();
		super.initAndValidate();
	}




	@Override
	public double proposal(int treeIndex) {
		
		
		// Make the operation to get the specified tree number
		// The change is topological only
		// This method will also set nodes (i, p, CiP, j, k, pP) to their appropriate values
		getNeighbouringTrees(null, treeIndex);
		
        
		// Sample time of the subtree being moved
        double newMinAge = Math.max(i.getHeight(), j.getHeight());
        double newRange = k.getHeight() - newMinAge;
        double newAge = newMinAge + (Randomizer.nextDouble() * newRange);
        double oldMinAge = Math.max(i.getHeight(), CiP.getHeight());
        double oldRange = pP.getHeight() - oldMinAge;
        double logHR = Math.log(newRange) - Math.log(Math.abs(oldRange));
        
        // This happens when some branch lengths are zero.
        if (oldRange == 0 || newRange == 0) {
            return Double.NEGATIVE_INFINITY;
        }
       
        
        
        // Recalculate rates
        double logJD = 0;
		if (rates != null) {
			
			// Get node times
	        tp = p.getHeight(); // Free
	        tC = CiP.getHeight(); // Fixed
	        tj = j.getHeight(); // Fixed
	        tk = k.getHeight(); // Fixed
	        tpP = pP.getHeight(); // Fixed
	        
	        
	        // Get node rates 
	        rp = rates.getValue(p.getNr()); // Fixed
	        rC = rates.getValue(CiP.getNr()); // Free
	        rj = rates.getValue(j.getNr()); // Free
	        
	        
	        // Propose new rates
	        double rCprime = (rp*(tpP - tp) + rC*(tp - tC)) / (tpP - tC); 
	        double rjprime = (rj*(tk - tj) - rp*(tk - newAge)) / (newAge - tj); 
	        
	        
			// Jacobian determinant
			double JD = ((tp - tC) * (tk - tj)) / ((tpP - tC) * (newAge - tj));
			
			// Constraint checker
			if (JD <= 0 || rCprime <= 0 || rjprime <= 0) {
				return Double.NEGATIVE_INFINITY;
			}
			
			// Set new times and rates
			if (rCprime != rC) rates.setValue(CiP.getNr(), rCprime);
			if (rjprime != rj) rates.setValue(j.getNr(), rjprime);
			
			
			logJD = Math.log(JD);
			
			
		}
		
		 p.setHeight(newAge);
        
        
        return logHR + logJD;

	}

	
	@Override
	public int getNeighbouringTrees(TreeGuider treeGuider, int treeIndex) {
		
		
		// Assumes that treeIndices is sorted
		// Is a proposal being made or are we generating all neighbours?
		boolean makingProposal = treeIndex >= 0 && treeGuider == null;
		boolean studyingTrees = treeGuider != null;
		
		
		// Copy the tree (topology only) if finding neighbours
		// Use the original tree if making a proposal
		Tree trevor = makingProposal ? tree : tree; // new Tree(tree.getRoot().toNewick(true));
		if (studyingTrees) treeGuider.setTree(trevor);
		
		//String newick1 = TreeGuideUtils.serialiseNode(trevor.getRoot());
    	
    	
        int currentTreeIndex = 0;
        
        // Get all internal/root nodes which are applicable
        for (int index = 0; index < trevor.getNodeCount(); index++) {
        	
        	// i is the destination of the subtree being moved (i must not be root)
        	i = trevor.getNode(index);
        	if (i.isRoot()) continue;
        	
        	
        	// p is the parent of i (p must not be root)
        	p = i.getParent();
        	if (p.isRoot()) continue;
        	
        	
        	// Find all branches <j,k> which <i,p> can be moved to
        	for (int jindex = 0; jindex < trevor.getNodeCount(); jindex++) {
        		
        		// Ensure that j != i and j != p
        		if (index == jindex || p.getNr() == jindex) continue;
        		
        		// Ensure that j and k are not the root
        		j = trevor.getNode(jindex);
        		if (j.isRoot()) continue;
        		k = j.getParent();
        		if (k.isRoot()) continue;
        		
        		// Ensure that k != p
        		if (p.getNr() == k.getNr()) continue;
        		
        		// Ensure that the height of k is greater than that of i
        		if (k.getHeight() <= i.getHeight()) continue;
        		
        		
        		
        		if (makingProposal) {
        			
        			
        			// Found the tree to move to
      	        	if (treeIndex == currentTreeIndex) {
      	        		
      	        		pP = p.getParent();
      	        		CiP = getOtherChild(p, i);
     	                replace(pP, p, CiP);
     	                replace(p, CiP, j);
     	                replace(k, j, p);
      	        		return -1;
      	        		
      	        	}
        			
        			
        		} else {
        		
        			
        			// Are we studying this tree?
        			if (studyingTrees) {
        			
        				
        				// Make the topological change (do not adjust times yet)
        				
		                // Disconnect p
	        			CiP = getOtherChild(p, i);
		                pP = p.getParent();
		                replace(pP, p, CiP);
		                
		                // Re-attach first child node to p
		                replace(p, CiP, j);
		                
		                // Re-attach k to p
		                replace(k, j, p);
		                
		                // Make note of the new tree
		                treeGuider.addNeighbouringTree(trevor, i.getNr(), j.getNr());
		                
		                //String newick2 = TreeGuideUtils.serialiseNode(trevor.getRoot());
		                
		                // Restore the changes
		                replace(k, p, j);
		                replace(p, j, CiP);
		                replace(pP, CiP, p);
		                
		                
		                //String newick3 = TreeGuideUtils.serialiseNode(trevor.getRoot());
		                
		                /*
		                System.out.println("i = " + index + "; j = " + jindex);
		                System.out.println(newick1);
		                System.out.println(newick2);
		                System.out.println(newick3);
		                System.out.println("------------------------------------------");
		                */

	                
        			}
                
        		}
        		
        		currentTreeIndex ++;
        		
        		
        		
        	}
        	
        	
        }
        
        return currentTreeIndex;
        
	}



}
