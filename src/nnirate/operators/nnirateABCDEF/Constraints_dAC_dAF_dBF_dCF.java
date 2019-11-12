package nnirate.operators.nnirateABCDEF;

import nnirate.operators.OperatorABCDEF;
import beast.util.Randomizer;

public class Constraints_dAC_dAF_dBF_dCF extends OperatorABCDEF {

	@Override
	public void init() {
		constraints = "dAC_dAF_dBF_dCF";
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
		rap = -(ra*ta - ra*td + rd*td - rd*te)/(r_eta - ta + te);
		rbp = -(rb*tb - rb*td + rd*td - rd*te - (r_delta + rd)*(r_zeta + td) + (r_delta + rd)*(r_eta + te))/(r_zeta - tb + td);
		rcp = -(rc*tc - rc*te - (r_delta + rd)*(r_zeta + td) + (r_delta + rd)*(r_eta + te))/(r_zeta - tc + td);
		rdp = r_delta + rd;
		rep = (re*(te - tf))/(r_eta + te - tf);
		tdp = r_zeta + td;
		tep = r_eta + te;


		// Jacobian determinant
		double JD = -((ta - td)*(tb - td)*(tc - te)*(te - tf))/((r_eta - ta + te)*(r_zeta - tb + td)*(r_zeta - tc + td)*(r_eta + te - tf));
		return JD;

	}

}
