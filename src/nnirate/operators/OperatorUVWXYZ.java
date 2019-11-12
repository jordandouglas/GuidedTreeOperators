package nnirate.operators;

import beast.core.Distribution;
import beast.core.Operator;
import beast.evolution.operators.TreeOperator;

public class OperatorUVWXYZ extends OperatorNNIrate {
	
	
	protected String constraints;
	
	
	// Proposals
	protected double rup, rvp, rwp, rxp, ryp, txp, typ;
	
	
	
	public String getName() {
		return constraints;
	}
	

	
	@Override
	public double get_rup() {
		return rup;
	}
	
	@Override
	public double get_rvp() {
		return rvp;
	}
	
	@Override
	public double get_rwp() {
		return rwp;
	}
	
	@Override
	public double get_rxp() {
		return rxp;
	}
	
	@Override
	public double get_ryp() {
		return ryp;
	}
	
	@Override
	public double get_txp() {
		return txp;
	}
	
	@Override
	public double get_typ() {
		return typ;
	}
	



	
	public void init() {
		
		
	}
	
	// Return log JD
	public double proposal(double width, double tu, double tv, double tw, double tx, 
			double ty, double tz, double ru, double rv, double rw, double rx, double ry) {
		return 0;
	}
	
	public boolean breaksConstraints(double tu, double tv, double tw, double tx, 
			double ty, double tz, double ru, double rv, double rw, double rx, double ry) {
		return rup <= 0 || rvp <= 0 || rwp <= 0 || rxp <= 0 || ryp <= 0 || txp < tw || txp < tv || typ < txp || typ < tu || tz < typ;
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
