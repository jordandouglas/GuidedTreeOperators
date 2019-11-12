package nnirate.operators.nnirateABCDEF;

import nnirate.operators.OperatorABCDEF;
import beast.util.Randomizer;

public class Constraints_dAB_dAC extends OperatorABCDEF {

	@Override
	public void init() {
		constraints = "dAB_dAC";
	}

	@Override
	public double proposal(double width, double ta, double tb, double tc, double td, double te, double tf, double ra, double rb, double rc, double rd, double re) {

		// Random proposals
		double r_alpha = (Randomizer.nextFloat()-0.5) *2*width;
		double r_beta = (Randomizer.nextFloat()-0.5) *2*width;
		double r_gamma = (Randomizer.nextFloat()-0.5) *2*width;
		double r_delta = (Randomizer.nextFloat()-0.5) *2*width;
		double r_epsilon = (Randomizer.nextFloat()-0.5) *2*width;
		double r_zeta = (Randomizer.nextFloat()-0.5) *2*width*(te - Math.max(tb, tc));
		double r_eta = (Randomizer.nextFloat()-0.5) *2*width*(tf - Math.max(td, tc));


		// Recalculate times and rates
		rap = (tc*(r_gamma + rc) - ra*ta + ra*td - rc*tc + rc*te - rd*td + rd*te - (r_gamma + rc)*(r_zeta + td) + (r_delta + rd)*(r_zeta + td) - (r_delta + rd)*(r_eta + te))/(r_eta - ta + te);
		rbp = -(tc*(r_gamma + rc) + rb*tb - rb*td - rc*tc + rc*te - rd*td + rd*te - (r_gamma + rc)*(r_zeta + td))/(r_zeta - tb + td);
		rcp = r_gamma + rc;
		rdp = r_delta + rd;
		rep = r_epsilon + re;
		tdp = r_zeta + td;
		tep = r_eta + te;


		// Jacobian determinant
		double JD = ((ta - td)*(tb - td))/((r_eta - ta + te)*(r_zeta - tb + td));
		return JD;

	}

}
