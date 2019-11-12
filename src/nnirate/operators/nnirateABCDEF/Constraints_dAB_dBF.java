package nnirate.operators.nnirateABCDEF;

import nnirate.operators.OperatorABCDEF;
import beast.util.Randomizer;

public class Constraints_dAB_dBF extends OperatorABCDEF {

	@Override
	public void init() {
		constraints = "dAB_dBF";
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
		rap = -(r_eta*r_epsilon + r_eta*re + ra*ta + r_epsilon*te - ra*td - r_epsilon*tf - rd*td + rd*te)/(r_eta - ta + te);
		rbp = (r_eta*r_epsilon - r_delta*r_eta + r_delta*r_zeta - r_eta*rd + r_zeta*rd + r_eta*re + r_delta*td - rb*tb - r_delta*te + r_epsilon*te - r_epsilon*tf + rb*td)/(r_zeta - tb + td);
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
