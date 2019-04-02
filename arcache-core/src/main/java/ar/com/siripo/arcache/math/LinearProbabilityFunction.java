package ar.com.siripo.arcache.math;

public class LinearProbabilityFunction extends AbstractProbabilityFunction {

	public LinearProbabilityFunction(final double graceZone) {
		this.setGraceZone(graceZone);
	}

	@Override
	public double getProbability(final double x) {
		final double y = (x - graceZone) / (1 - graceZone);
		if (y <= 0) {
			return (0);
		}
		if (y >= 1) {
			return (1);
		}
		return y;
	}
}
