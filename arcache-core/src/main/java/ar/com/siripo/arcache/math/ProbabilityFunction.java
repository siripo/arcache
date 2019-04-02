package ar.com.siripo.arcache.math;

/**
 * This interface exists to allow the definition of ad hoc probability functions
 * that are used within the invalidation and expiration strategy.
 * 
 * @author Mariano Santamarina
 *
 */
public interface ProbabilityFunction {

	/**
	 * What is expected of this function is that it returns probability values with
	 * the following conditions.
	 * 
	 * For x &lt; = 0 you should return probability = 0. For x &gt; = 1 should
	 * return probability = 1. For values between [0,1] you must return values
	 * between [0,1] according to the curve implemented.
	 * 
	 * This must be a monotonically increasing function. The reason is that a
	 * growing form allows good behavior with high and low throughput.
	 * 
	 * Keep in mind that x represents how old the key is for which the probability
	 * that it is expired is being requested. We will indicate x = 0 when the age of
	 * the key is zero. X = 1 will be indicated when the ttl of the key has been
	 * fulfilled.
	 * 
	 * @param x how old is the key in a normalized way age/ttl
	 * @return a probability in the range of [0,1]
	 */
	public double getProbability(double x);
}
