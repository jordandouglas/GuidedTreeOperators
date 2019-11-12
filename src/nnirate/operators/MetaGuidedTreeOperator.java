package nnirate.operators;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import beast.core.Input;
import beast.core.Operator;
import beast.core.OperatorSchedule;
import beast.core.StateNode;
import beast.core.util.Log;
import beast.evolution.alignment.Alignment;
import beast.evolution.operators.TreeOperator;
import beast.evolution.tree.Tree;
import beast.util.Randomizer;
import guidedtreeoperators.GuidedTreeOperator;
import treeguiding.CCPTreeGuider;
import treeguiding.ParsimonyTreeGuider;
import treeguiding.TreeGuider;

public class MetaGuidedTreeOperator extends TreeOperator  {


	
	final public Input<List<TreeOperator>> operatorsInput = new Input<>("op", "operators to sample", new ArrayList<>());
	final public Input<Alignment> alignmentInput = new Input<>("alignment", "tree alignment for computing parsimony score", Input.Validate.REQUIRED);
    final public Input<String> treeGuiderNameInput = new Input<>("guider", "select a tree guiding algorithm (parsimony, posterior, none)", "parsimony");
	
    Tree tree;
    
    TreeGuider treeGuider;
    
    
	
	
	List<TreeOperator> operators;
	double weightSum;
	TreeOperator lastOperator;
	
	
	@Override
	public void initAndValidate() {
		operators = operatorsInput.get();
		tree =  treeInput.get();
		
		
		switch (treeGuiderNameInput.get()) {
		
			case "parsimony":{
				treeGuider = new ParsimonyTreeGuider(alignmentInput.get());
				break;
			}
			
			case "posterior":{
				treeGuider = new CCPTreeGuider(alignmentInput.get());
				break;
			}
		
			
			case "none":{
				treeGuider = new TreeGuider(alignmentInput.get());
				break;
			}
			
			default:{
				
				throw new IllegalArgumentException("Unrecognised tree guiding parameter: '" + treeGuiderNameInput.get() +
							"'. Please use one of the following: {parsimony, posterior, none}");
			}
			
			
			
		
		}
		treeGuider.initAndValidate();
		
		
		
		// Compute sum of all operator weights
		weightSum = 0;
		for (int i = 0; i < operators.size(); i ++) {
			TreeOperator op = operators.get(i);
			op.initAndValidate();
			weightSum += op.getWeight();
			if (op instanceof GuidedTreeOperator) {
				GuidedTreeOperator x = (GuidedTreeOperator)op;
				x.setTree(tree);
			}
		}

	}

	@Override
	public double proposal() {
		
		// Before doing anything, let the Tree Guider know that this current tree
		// is the current posterior state
		treeGuider.cacheCurrentStateTree(tree);
		
		
		
		// Sample an operator
		double u = Randomizer.nextFloat() * weightSum;
		double cumSum = 0;
		lastOperator = null;
		for (TreeOperator op : operators) {
			cumSum += op.getWeight();
			if (u <= cumSum) {
				lastOperator = op;
				break;
			}
		}
		if (lastOperator == null) return Double.NEGATIVE_INFINITY;
		
		
		
		// If it is a normal tree operator then proceed normally
		if (! (lastOperator instanceof GuidedTreeOperator)) {
			return lastOperator.proposal();
		}
		
		
		GuidedTreeOperator guidedOperator = (GuidedTreeOperator)lastOperator;
		

		
		// Get the neighbours of the current tree and compute their scores
		guidedOperator.getNeighbouringTrees(treeGuider, -1);
		if (treeGuider.getNeighbours().size() == 0) return Double.NEGATIVE_INFINITY;
		String originalNewick = treeGuider.getNewick(); 
		
		// Get proposal probabilities
		double[] probabilitiesForward = treeGuider.getProposalProbabilities();
		
		
    	// Sample a neighbouring tree
    	final int proposedTreeNum = Randomizer.randomChoice(probabilitiesForward);
		 
		// original tree newick before
		
		//String originalNewick = getTreeSerial(tree);
		
		// Make the proposal and get its Jacobian determinant
		double logJD = guidedOperator.proposal(proposedTreeNum);
		if (logJD == Double.NEGATIVE_INFINITY) return Double.NEGATIVE_INFINITY;
		
		
		// Forward proposal density
		String proposedNewick = treeGuider.getNeighbours().get(proposedTreeNum);
		double proposalForward = treeGuider.getProposalDensity(proposedNewick, probabilitiesForward);
		
        
        						
		
        
        // Get the neighbours of the proposed tree and compute their scores
		guidedOperator.getNeighbouringTrees(treeGuider, -1);
 		
 		
 		// Get proposal probabilities out of the proposed state
		double[] probabilitiesBackward = treeGuider.getProposalProbabilities();
		
		
		// Backwards proposal density
        double proposalBackward = treeGuider.getProposalDensity(originalNewick, probabilitiesBackward);
        
		
        	
		// Return Hastings ratio
        double logHR = proposalBackward - proposalForward;
        //System.out.println(Math.exp(proposalBackward) + " / " + Math.exp(proposalForward) + " = " + logHR + "\n\n");
		return logHR + logJD;
		
	}
	
	
	

	
	
	
	@Override
	public void optimize(double logAlpha) {
		lastOperator.optimize(logAlpha);
	}
	
	@Override
	public void accept() {
		lastOperator.accept();
		super.accept();
	}
	
	@Override
	public void reject() {
		lastOperator.reject();
		super.reject();
	}
	
	@Override
	public void reject(final int reason) {
		lastOperator.reject(reason);
		super.reject(reason);
	}
	
	@Override
	public List<StateNode> listStateNodes() {
		List<StateNode> list = new ArrayList<StateNode>();
		list.add(tree);
		for (Operator operator : operators) {
			List<StateNode> list2 = operator.listStateNodes();
			list.addAll(list2);
		}
		return list;
	}

	@Override
	public void storeToFile(PrintWriter out) {
		out.print("{\"id\":\"" + getID() + "\",");
		out.print("\"cacheSize\":" + treeGuider.getCacheSize() + ",");
		out.print("\"operators\":[\n");
		int k = 0;
		for (Operator o : operators) {
			o.storeToFile(out);
            if (k++ < operators.size() - 1) {
            	out.println(",");
            }
		}
        out.print("]}");
	}
	
	
	@Override
	public void restoreFromFile(JSONObject o) {
        try {
        JSONArray operatorlist = o.getJSONArray("operators");
        for (int i = 0; i < operatorlist.length(); i++) {
            JSONObject item = operatorlist.getJSONObject(i);
            String id = item.getString("id");
    		boolean found = false;
            if (!id.equals("null")) {
            	for (Operator operator: operators) {
            		if (id.equals(operator.getID())) {
                    	operator.restoreFromFile(item);
                        found = true;
            			break;
            		}
            	}
            }
        	if (!found) {
        		Log.warning.println("Operator (" + id + ") found in state file that is not in operator list any more");
        	}
        }
    	for (Operator operator: operators) {
    		if (operator.getID() == null) {
        		Log.warning.println("Operator (" + operator.getClass() + ") found in BEAST file that could not be restored because it has not ID");
    		}
    	}
        } catch (JSONException e) {
        	// it is not a JSON file -- probably a version 2.0.X state file
	    }
	}
	
	
	@Override
	public void setOperatorSchedule(final OperatorSchedule operatorSchedule) {
		super.setOperatorSchedule(operatorSchedule);
        for (Operator o : operators) {
        	o.setOperatorSchedule(operatorSchedule);
        }
    }
	



}




