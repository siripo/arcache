package ar.com.siripo.arcache.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class AdjustedExponentialProbabilityFunctionTest {

	@Test
	public void testAdjustedExponentialProbabilityFunction() {
		AdjustedExponentialProbabilityFunction ep = new AdjustedExponentialProbabilityFunction(0.123, 25);
		assertEquals(0.123, ep.graceZone, 0);
		assertEquals(25, ep.shapeFactor, 0);

		try {
			new AdjustedExponentialProbabilityFunction(-1, 25);
			fail("Expected IllegalArgumentException, graceZone -1 is invalid");
		} catch (IllegalArgumentException ia) {

		}

		try {
			new AdjustedExponentialProbabilityFunction(22, 25);
			fail("Expected IllegalArgumentException, graceZone 22 is invalid");
		} catch (IllegalArgumentException ia) {

		}

		new AdjustedExponentialProbabilityFunction(0, 1);

		try {
			new AdjustedExponentialProbabilityFunction(0, 0);
			fail("Expected IllegalArgumentException, shapeFactor zero is invalid");
		} catch (IllegalArgumentException ia) {

		}
	}

	@Test
	public void testGetProbability() {
		AdjustedExponentialProbabilityFunction ep = new AdjustedExponentialProbabilityFunction(0.5, 2);

		assertEquals(0, ep.getProbability(-1), 0);
		assertEquals(0, ep.getProbability(0), 0);
		assertEquals(0, ep.getProbability(0.4), 0);
		assertEquals(0, ep.getProbability(0.5), 0);
		assertEquals(0.077, ep.getProbability(0.6), 0.001);
		assertEquals(0.192, ep.getProbability(0.7), 0.001);
		assertEquals(0.363, ep.getProbability(0.8), 0.001);
		assertEquals(0.619, ep.getProbability(0.9), 0.001);
		assertEquals(0.790, ep.getProbability(0.95), 0.001);
		assertEquals(0.955, ep.getProbability(0.99), 0.001);
		assertEquals(1, ep.getProbability(1), 0);
		assertEquals(1, ep.getProbability(1.5), 0);
		assertEquals(1, ep.getProbability(2), 0);
	}

}
