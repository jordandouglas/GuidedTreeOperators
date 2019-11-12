package nnirate.operators.nnirateABCDEF;

import nnirate.operators.OperatorABCDEF;
import beast.util.Randomizer;

public class Constraints_null extends OperatorABCDEF {

	@Override
	public void init() {
		constraints = "null";
	}

	@Override
	public double proposal(double width, double ta, double tb, double tc, double td, double te, double tf, double ra, double rb, double rc, double rd, double re) {

		// Recalculate times and rates
		rap = ra;
		rbp = rb;
		rcp = rc;
		rdp = rd;
		rep = re;
		tdp = td;
		tep = te;

		// Jacobian determinant
		return 1;

	}

}
