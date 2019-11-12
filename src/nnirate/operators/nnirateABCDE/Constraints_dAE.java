package nnirate.operators.nnirateABCDE;

import nnirate.operators.OperatorABCDE;
import beast.util.Randomizer;

public class Constraints_dAE extends OperatorABCDE {

	@Override
	public void init() {
		constraints = "dAE";
	}

	@Override
	public double proposal(double width, double ta, double tb, double tc, double td, double te, double ra, double rb, double rc, double rd) {

		// Random proposals
		double r_alpha = (Randomizer.nextFloat()-0.5) *2*width;
		double r_beta = (Randomizer.nextFloat()-0.5) *2*width;
		double r_gamma = (Randomizer.nextFloat()-0.5) *2*width;
		double r_delta = (Randomizer.nextFloat()-0.5) *2*width;
		double r_epsilon = (Randomizer.nextFloat()-0.5) *2*width;


		// Recalculate times and rates
		rap = (ra*ta - ra*td + rd*td - rd*te)/(ta - te);
		rbp = r_beta + rb;
		rcp = r_gamma + rc;
		rdp = r_delta + rd;
		tdp = r_epsilon + td;


		// Jacobian determinant
		double JD = (ta - td)/(ta - te);
		return JD;

	}

}
