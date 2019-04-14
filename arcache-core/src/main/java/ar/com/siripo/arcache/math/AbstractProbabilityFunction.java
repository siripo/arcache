package ar.com.siripo.arcache.math;

public abstract class AbstractProbabilityFunction implements ProbabilityFunction {

	/** The zone where the probability is zero , must be between [0,1) */
	protected double graceZone = 0;

	protected void setGraceZone(final double graceZone) {
		if (graceZone < 0) {
			throw new IllegalArgumentException("Invalid Grace Zone Value, must be >=0 ");
		}
		if (graceZone >= 1) {
			throw new IllegalArgumentException("Invalid Grace Zone Value, must be < 1 ");
		}
		this.graceZone = graceZone;
	}
}
