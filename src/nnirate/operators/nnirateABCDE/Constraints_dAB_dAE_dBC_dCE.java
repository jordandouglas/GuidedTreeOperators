package nnirate.operators.nnirateABCDE;

import nnirate.operators.OperatorABCDE;
import beast.util.Randomizer;

public class Constraints_dAB_dAE_dBC_dCE extends OperatorABCDE {

	@Override
	public void init() {
		constraints = "dAB_dAE_dBC_dCE";
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
		rbp = -(rb*(tb - td))/(r_epsilon - tb + td);
		rcp = -(rc*tc - rc*te + rd*td - rd*te)/(r_epsilon - tc + td);
		rdp = -(rd*(td - te))/(r_epsilon + td - te);
		tdp = r_epsilon + td;


		// Jacobian determinant
		double JD = -((ta - td)*(tb - td)*(tc - te)*(td - te))/((ta - te)*(r_epsilon - tb + td)*(r_epsilon - tc + td)*(r_epsilon + td - te));
		return JD;

	}

}
