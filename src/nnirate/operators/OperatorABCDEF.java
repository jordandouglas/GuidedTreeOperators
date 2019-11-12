package nnirate.operators;

import beast.core.Distribution;
import beast.core.Operator;
import beast.evolution.operators.TreeOperator;

public class OperatorABCDEF extends OperatorNNIrate {
	
	
	protected String constraints;
	
	
	// Proposals
	protected double rap, rbp, rcp, rdp, rep, tdp, tep;
	
	
	
	public String getName() {
		return constraints;
	}
	

	@Override
	public double get_rap() {
		return rap;
	}
	
	@Override
	public double get_rbp() {
		return rbp;
	}
	
	@Override
	public double get_rcp() {
		return rcp;
	}
	
	@Override
	public double get_rdp() {
		return rdp;
	}
	
	@Override
	public double get_rep() {
		return rep;
	}
	
	@Override
	public double get_tdp() {
		return tdp;
	}
	
	@Override
	public double get_tep() {
		return tep;
	}

	
	
	public void init() {
		
		
	}

	
	// Return log JD
	public double proposal(double width, double ta, double tb, double tc, double td, 
						double te, double tf, double ra, double rb, double rc, double rd, double re) {
		return 0;
	}
	
	public boolean breaksConstraints(double ta, double tb, double tc, double td, 
			double te, double tf, double ra, double rb, double rc, double rd, double re) {
		return rap <= 0 || rbp <= 0 || rcp <= 0 || rdp <= 0 || rep <= 0 || tdp < tc || tdp < tb || tep < tdp || tep < ta || tf < tep;
	}
	

	@Override
	public void initAndValidate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double proposal() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	
	

}
