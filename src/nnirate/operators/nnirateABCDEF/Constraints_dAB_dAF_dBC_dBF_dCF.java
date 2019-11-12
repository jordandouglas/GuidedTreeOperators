package nnirate.operators.nnirateABCDEF;

import nnirate.operators.OperatorABCDEF;
import beast.util.Randomizer;

public class Constraints_dAB_dAF_dBC_dBF_dCF extends OperatorABCDEF {

	@Override
	public void init() {
		constraints = "dAB_dAF_dBC_dBF_dCF";
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
		rap = -(ra*(ta - td))/(r_eta - ta + te);
		rbp = -(rb*tb - rb*td + rd*td - rd*te)/(r_zeta - tb + td);
		rcp = -(rc*(tc - te))/(r_zeta - tc + td);
		rdp = (rd*(td - te))/(r_eta - r_zeta - td + te);
		rep = (rd*td - rd*te + re*te - re*tf)/(r_eta + te - tf);
		tdp = r_zeta + td;
		tep = r_eta + te;


		// Jacobian determinant
		double JD = -((ta - td)*(tb - td)*(tc - te)*(td - te)*(te - tf))/((r_eta - ta + te)*(r_zeta - tb + td)*(r_zeta - tc + td)*(r_eta + te - tf)*(r_eta - r_zeta - td + te));
		return JD;

	}

}
