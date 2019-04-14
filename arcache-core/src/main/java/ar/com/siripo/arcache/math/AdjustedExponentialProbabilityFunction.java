package ar.com.siripo.arcache.math;

public class AdjustedExponentialProbabilityFunction extends AbstractProbabilityFunction {

	protected double shapeFactor = 11;

	public AdjustedExponentialProbabilityFunction(final double graceZone, final double shapeFactor) {
		this.setGraceZone(graceZone);
		if (shapeFactor == 0) {
			throw new IllegalArgumentException("shapeFactor must be != 0");
		}
		this.shapeFactor = shapeFactor;
	}

	@Override
	public double getProbability(final double x) {
		final double xx = (x - graceZone) / (1 - graceZone);
		final double y = (Math.exp(xx * shapeFactor) - 1) / (Math.exp(shapeFactor) - 1);
		if (y <= 0) {
			return (0);
		}
		if (y >= 1) {
			return (1);
		}

		return y;
	}
}
