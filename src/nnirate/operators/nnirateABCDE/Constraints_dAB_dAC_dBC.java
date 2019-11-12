package nnirate.operators.nnirateABCDE;

import nnirate.operators.OperatorABCDE;
import beast.util.Randomizer;

public class Constraints_dAB_dAC_dBC extends OperatorABCDE {

	@Override
	public void init() {
		constraints = "dAB_dAC_dBC";
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
		rap = (te*(r_delta + rd) + ra*ta - ra*td - (r_delta + rd)*(r_epsilon + td))/(ta - te);
		rbp = -(rb*(tb - td))/(r_epsilon - tb + td);
		rcp = -(rc*tc - rc*te + rd*td - rd*te)/(r_epsilon - tc + td);
		rdp = r_delta + rd;
		tdp = r_epsilon + td;


		// Jacobian determinant
		double JD = ((ta - td)*(tb - td)*(tc - te))/((ta - te)*(r_epsilon - tb + td)*(r_epsilon - tc + td));
		return JD;

	}

}
