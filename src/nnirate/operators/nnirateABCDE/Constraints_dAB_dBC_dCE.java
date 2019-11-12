package nnirate.operators.nnirateABCDE;

import nnirate.operators.OperatorABCDE;
import beast.util.Randomizer;

public class Constraints_dAB_dBC_dCE extends OperatorABCDE {

	@Override
	public void init() {
		constraints = "dAB_dBC_dCE";
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
		rap = (2*te*(r_delta + rd) + ra*ta - ra*td - rd*td + rd*te - 2*(r_delta + rd)*(r_epsilon + td))/(ta - te);
		rbp = (te*(r_delta + rd) - rb*tb + rb*td - rd*td + rd*te - (r_delta + rd)*(r_epsilon + td))/(r_epsilon - tb + td);
		rcp = -(te*(r_delta + rd) + rc*tc - rc*te - (r_delta + rd)*(r_epsilon + td))/(r_epsilon - tc + td);
		rdp = r_delta + rd;
		tdp = r_epsilon + td;


		// Jacobian determinant
		double JD = ((ta - td)*(tb - td)*(tc - te))/((ta - te)*(r_epsilon - tb + td)*(r_epsilon - tc + td));
		return JD;

	}

}
