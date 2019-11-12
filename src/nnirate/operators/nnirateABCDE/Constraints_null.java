package nnirate.operators.nnirateABCDE;

import nnirate.operators.OperatorABCDE;
import beast.util.Randomizer;

public class Constraints_null extends OperatorABCDE {

	@Override
	public void init() {
		constraints = "null";
	}

	@Override
	public double proposal(double width, double ta, double tb, double tc, double td, double te, double ra, double rb, double rc, double rd) {

		// Recalculate times and rates
		rap = ra;
		rbp = rb;
		rcp = rc;
		rdp = rd;
		tdp = td;

		// Jacobian determinant
		return 1;

	}

}
