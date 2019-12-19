package guidedtreeoperators.operators.treeguiding;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ContinuousDistribution;
import org.apache.commons.math.distribution.ExponentialDistribution;
import org.apache.commons.math.distribution.ExponentialDistributionImpl;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;
import beast.math.distributions.ParametricDistribution;

public class BranchRateTreeGuider extends TreeGuider {
	
	
	public final Input<Double> warpFactorInput = new Input<>("warpFactor", "Warp factor", 1.0);
	public final  Input<RealParameter> rateInput = new Input<>("rates", "the rates associated with nodes in the tree for sampling of individual rates among branches.", Input.Validate.REQUIRED);
	public final  Input<RealParameter> sigmaInput = new Input<>("sigma", "lognormal clock SD associated with rates. If unspecified, exponential clock model is assumed.");
    
	
    enum RateDistribution {
        exponential,
        lognormal
    }
	
	
	double scoreSum;
	RateDistribution rateDistribution = RateDistribution.exponential;
	ContinuousDistribution distribution; 
	
	double warpfactor;
	RealParameter rates;
	RealParameter sigma;
	
	@Override
	public void initAndValidate() {
		rates = rateInput.get();
		sigma = sigmaInput.get();
		scoreSum = 0;
		warpfactor = warpFactorInput.get();
		
		rateDistribution = sigma == null ? RateDistribution.exponential : RateDistribution.lognormal;
		if (rateDistribution ==  RateDistribution.lognormal) distribution = new ExponentialDistributionImpl(1);
		
		
		super.initAndValidate();
	}
	
	
	@Override
	public void cacheCurrentStateTree(Tree tree) {
		super.cacheCurrentStateTree(tree);
	}
	
	
	@Override
	protected void resetNeighbours() {
		
		// Update lognormal distribution
		if ( rateDistribution == RateDistribution.lognormal) {
			
			double s = 0.5;
			final double M = Math.log(1) - (0.5 * sigma.getValue() * sigma.getValue());
			distribution = new NormalDistributionImpl(M, sigma.getValue());
           // final double M = Math.log(1) - (0.5 * sigma.getValue() * sigma.getValue());
           // distribution = new NormalDistributionImpl(M, sigma.getValue());
			
		}
		
		scoreSum = 0;
		super.resetNeighbours();
		
	}
	
	
	// Calculates score of the tree
	@Override
	protected double computeUnnormalisedScore(Tree tree, String newick, int nodeBeingMovedNr, int nodeBeingMoveToNr) {
		
		double score = 0;
		
		
		// The score is proportional to the cdf of the rates of the affected branches
		double rateFrom = rates.getArrayValue(nodeBeingMovedNr);
		double rateTo = rates.getArrayValue(nodeBeingMoveToNr);
		
		double cdfFrom = 0;
		double cdfTo = 0;
		
		try {
			cdfFrom = distribution.cumulativeProbability(rateFrom);
			cdfTo = distribution.cumulativeProbability(rateTo);
		} catch (MathException e) {
			e.printStackTrace();
		}
		
		// Convert 0.99 to 0.01 etc. Now large numbers indicate being close to the median.
		if (cdfFrom > 0.5) cdfFrom = 1 - cdfFrom;
		if (cdfTo > 0.5) cdfTo = 1 - cdfTo;
		
		score = Math.exp(warpfactor * cdfFrom); //Math.pow(cdfFrom, 1) * Math.pow(cdfTo, warpfactor);
		
		scoreSum += score;
		
		return score;
		
	}
	

	
	@Override
	public void optimize(double delta) {
		delta += Math.log(warpfactor);
		warpfactor = Math.exp(delta);
	}
	
	
	
	@Override
	public double[] getProposalProbabilities() {
		
		
		if (neighbours.size() == 0) return null;
		
		double[] probabilities = new double[neighbours.size()];

		
		// Convert scores into a cumulative probability array
		double cumSum = 0;
		for (int i = 0; i < neighbours.size(); i ++) {
			if (scoreSum <= 0) probabilities[i] = 1.0 / neighbours.size();
			else {
				cumSum += neighbourScores.get(i) / scoreSum;
				probabilities[i] = cumSum;
			}
		}
		
		
		return probabilities;
		
	}
	

	
	
}
