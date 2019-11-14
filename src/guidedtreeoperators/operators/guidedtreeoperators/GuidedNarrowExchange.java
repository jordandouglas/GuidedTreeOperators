package guidedtreeoperators.operators.guidedtreeoperators;


import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import guidedtreeoperators.operators.treeguiding.TreeGuider;

public class GuidedNarrowExchange extends GuidedTreeOperator {

	
	public final  Input<RealParameter> rateInput = new Input<>("rates", "the rates associated with nodes in the tree for sampling of individual rates among branches.");
    
	RealParameter rates;
	private Node A, B, C, D, E;
	private double ta, tb, tc, td, te, ra, rb, rc, rd;

	@Override
	public void initAndValidate() {
		rates = rateInput.get();
		super.initAndValidate();
	}



	@Override
	public double proposal() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public double proposal(int treeIndex) {
		
		// Make the operation to get the specified tree number
		// The change is topological only
		// This method will also set nodes A-E to their appropriate value
		getNeighbouringTrees(null, treeIndex);
		
		
		double logJD = 0;
		if (rates != null) {
			
			
			// Get node times
	        ta = A.getHeight(); // Fixed
	        tb = B.getHeight(); // Fixed
	        tc = C.getHeight(); // Fixed
	        td = D.getHeight(); // Free
	        te = E.getHeight(); // Free
	        
	        
	        // Get node rates 
	        ra = rates.getValue(A.getNr()); // Free
	        rb = rates.getValue(B.getNr()); // Free
	        rc = rates.getValue(C.getNr()); // Free
	        rd = rates.getValue(D.getNr()); // Free
	        
			
			double rdp = rd;
			double tdp = td;
			
			// Recalculate times and rates
			double rap = (ra*ta - ra*td + rd*td - rd*te)/(ta - te);
			double rbp = (rb*(td - tb) + rdp*(tdp - te) + rd*(te - td))/(tdp - tb);
			double rcp = -(te*(rdp) + rc*tc - rc*te - (rdp)*(tdp))/(tdp - tc);
			
			// Jacobian determinant
			double JD = ((ta - td)*(tb - td)*(tc - te))/((ta - te)*(tdp - tb)*(tdp - tc));

			// Constraint checker
			if (JD <= 0 || rap <= 0 || rbp <= 0 || rcp <= 0 || rdp <= 0 ||
			   tdp <= ta || tdp <= tc || tdp <= tb || te <= tdp) {
				return Double.NEGATIVE_INFINITY;
			}
			
			// Set new times and rates
	        if (rap != ra) rates.setValue(A.getNr(), rap);
	        if (rbp != rb) rates.setValue(B.getNr(), rbp);
	        if (rcp != rc) rates.setValue(C.getNr(), rcp);
			
			
			logJD = Math.log(JD);
			
		}
		
		return logJD;
	}
	
	
	
	@Override
	public void getNeighbouringTrees(TreeGuider treeGuider, int treeIndex) {

		boolean makingProposal = treeIndex >= 0 || treeGuider == null;
		
		// If this tree already has its neighbours cached, then return them
		if (!makingProposal && false && neighbourCache.containsKey(tree)) {
			return;
		}
		
		
		// Copy the tree (topology only) if finding neighbours
		// Use the original tree if making a proposal
		Tree trevor = makingProposal ? tree : tree; // new Tree(tree.getRoot().toNewick(true));
		if (!makingProposal) treeGuider.setTree(trevor);
    	
    	// Get all internal/root nodes which have grandchildren
        int currentTreeIndex = 0;
        for (int i = trevor.getLeafNodeCount(); i < trevor.getNodeCount(); i++) {
        	
        	E = trevor.getNode(i);
        	
        	// Ensure that at least 1 child is not a leaf
        	if (E.getChildCount() == 2 && (!E.getChild(0).isLeaf() || !E.getChild(1).isLeaf())) {
        		
        		
        		// Get nodes C and D
				D = E.getChild(0);
				C = E.getChild(1);
				if (D.getHeight() < C.getHeight()) {
				    D = E.getChild(1);
				    C = E.getChild(0);
				}
      	        if (D.isLeaf()) continue;
      	        
      	      
				// If we are here to make a proposal to a specified tree, then just find the tree number and make the proposal
      	        if (makingProposal) {
      	        	
      	        	
      	        	// Found the tree to move to
      	        	if (treeIndex == currentTreeIndex || treeIndex == currentTreeIndex + 1) {
      	        		
      	        		// Operation is on the original tree not the copied tree
      					A = treeIndex == currentTreeIndex ? D.getChild(0) : D.getChild(1);
      					B = treeIndex == currentTreeIndex ? D.getChild(1) : D.getChild(0);
      	        		
      	        		exchangeNodes(A, C, D, E);
      	        		//System.out.println("Sampled " + getTreeSerial(tree));
      	        		return;
      	        	}

      	        	
      	        }
      	        
      	        
      	        // We are here to enumerate the neighbours not to make a proposal
      	        else {
      	        	
      	        
					// Case 1: A is the first child of D
					A = D.getChild(0);
					B = D.getChild(1);
					
					// Temporarily rearrange the copied tree to get the neighbour
	    			exchangeNodes(A, C, D, E);
	    			treeGuider.addNeighbouringTree(trevor, B.getNr(), D.getNr());

					// Restore the copied tree
					exchangeNodes(A, C, E, D);
					
					
					// Case 2: A is the second child of D
					int originalANr = A.getNr();
					A = D.getChild(0);
					B = D.getChild(1);
					if (A.getNr() == originalANr) {
						System.out.println("Unexpected node positioning");
						A = D.getChild(1);
					}
					exchangeNodes(A, C, D, E);
					treeGuider.addNeighbouringTree(trevor, B.getNr(), D.getNr());
					
					// Restore the tree
					exchangeNodes(A, C, E, D);
					
					
      	        }
				
				currentTreeIndex += 2;
				
        	}
        	
        	
        }
    	
        //if (!makingProposal) neighbourCache.put(tree, neighbours);
	    
	}
	

    
    private void exchangeNodes(Node c1, Node c2, Node p1, Node p2) {
        replace(p1, c1, c2);
        replace(p2, c2, c1);
    }
	
	
	@Override
	public void setProposal(Tree proposedTree) {
		// TODO Auto-generated method stub
		
	}



}
