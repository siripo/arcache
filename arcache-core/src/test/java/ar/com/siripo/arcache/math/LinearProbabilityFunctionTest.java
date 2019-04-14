package ar.com.siripo.arcache.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LinearProbabilityFunctionTest {

	@Test
	public void testLinearProbabilityFunction() {
		LinearProbabilityFunction lp = new LinearProbabilityFunction(0.974);
		assertEquals(0.974, lp.graceZone, 0);
	}

	@Test
	public void testGetProbability() {
		LinearProbabilityFunction lp = new LinearProbabilityFunction(0.2);
		assertEquals(0, lp.getProbability(-1), 0);
		assertEquals(0, lp.getProbability(0), 0);
		assertEquals(0, lp.getProbability(0.1), 0);
		assertEquals(0, lp.getProbability(0.2), 0);
		assertEquals(1.0 / 8.0, lp.getProbability(0.3), 0.0001);
		assertEquals(3.0 / 8.0, lp.getProbability(0.5), 0.0001);
		assertEquals(7.0 / 8.0, lp.getProbability(0.9), 0.0001);
		assertEquals(1, lp.getProbability(0.9999), 0.01);
		assertEquals(1, lp.getProbability(1), 0);
		assertEquals(1, lp.getProbability(2), 0);
	}

}
