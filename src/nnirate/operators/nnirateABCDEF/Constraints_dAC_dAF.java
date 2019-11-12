package nnirate.operators.nnirateABCDEF;

import nnirate.operators.OperatorABCDEF;
import beast.util.Randomizer;

public class Constraints_dAC_dAF extends OperatorABCDEF {

	@Override
	public void init() {
		constraints = "dAC_dAF";
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
		rap = (r_eta*r_epsilon + r_eta*re - ra*ta + r_epsilon*te + ra*td - r_epsilon*tf - rd*td + rd*te)/(r_eta - ta + te);
		rbp = r_beta + rb;
		rcp = (tf*(r_epsilon + re) - rc*tc + rc*te + re*te - re*tf + (r_delta + rd)*(r_zeta + td) - (r_delta + rd)*(r_eta + te) - (r_epsilon + re)*(r_eta + te))/(r_zeta - tc + td);
		rdp = r_delta + rd;
		rep = r_epsilon + re;
		tdp = r_zeta + td;
		tep = r_eta + te;


		// Jacobian determinant
		double JD = ((ta - td)*(tc - te))/((r_eta - ta + te)*(r_zeta - tc + td));
		return JD;

	}

}
