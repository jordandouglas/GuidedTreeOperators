
package nnirate.operators;

import beast.core.Operator;

public class OperatorNNIrate extends Operator {
	
	
	protected String constraints;
	
	
	public String getName() {
		return constraints;
	}
	
	public double get_rup() {
		return 0;
	}
	
	public double get_rvp() {
		return 0;
	}
	
	public double get_rwp() {
		return 0;
	}
	
	public double get_rxp() {
		return 0;
	}
	
	public double get_ryp() {
		return 0;
	}
	
	public double get_txp() {
		return 0;
	}
	
	public double get_typ() {
		return 0;
	}
	


	@Override
	public void initAndValidate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double proposal() {
		return 0;
	}

	public double get_rap() {
		return 0;
	}

	public double get_rbp() {
		return 0;
	}

	public double get_rcp() {
		return 0;
	}

	public double get_rdp() {
		return 0;
	}
	
	public double get_rep() {
		return 0;
	}

	public double get_tdp() {
		return 0;
	}
	
	public double get_tep() {
		return 0;
	}

}
