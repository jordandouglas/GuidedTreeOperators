package nnirate.operators.nnirateABCDE;

import nnirate.operators.OperatorABCDE;
import beast.util.Randomizer;

public class Constraints_dAC_dBC extends OperatorABCDE {

	@Override
	public void init() {
		constraints = "dAC_dBC";
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
		rap = -(r_delta*r_epsilon - r_epsilon*r_gamma - r_epsilon*rc + r_epsilon*rd - ra*ta + r_gamma*tc + r_delta*td - r_gamma*td - r_delta*te + ra*td - rc*td + rc*te)/(ta - te);
		rbp = (tc*(r_gamma + rc) - rb*tb + rb*td - rc*tc + rc*te - rd*td + rd*te - (r_gamma + rc)*(r_epsilon + td))/(r_epsilon - tb + td);
		rcp = r_gamma + rc;
		rdp = r_delta + rd;
		tdp = r_epsilon + td;


		// Jacobian determinant
		double JD = -((ta - td)*(tb - td))/((ta - te)*(r_epsilon - tb + td));
		return JD;

	}

}
