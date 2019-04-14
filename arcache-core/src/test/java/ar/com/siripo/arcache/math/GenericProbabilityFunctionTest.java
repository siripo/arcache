package ar.com.siripo.arcache.math;

import static org.junit.Assert.fail;

import org.junit.Test;

public class GenericProbabilityFunctionTest {

	@Test
	public void testProbabilityFunctions() {
		// Test Linear Probability
		testProbabilityFunctionConstraints(new LinearProbabilityFunction(0));
		testProbabilityFunctionConstraints(new LinearProbabilityFunction(0.2));
		testProbabilityFunctionConstraints(new LinearProbabilityFunction(0.5));

		// Test Adjusted Exponential
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.0, 1));
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.2, 1));
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.5, 1));

		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.0, 5));
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.2, 5));
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.5, 5));

		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.0, 11));
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.2, 11));
		testProbabilityFunctionConstraints(new AdjustedExponentialProbabilityFunction(0.5, 11));
	}

	private void testProbabilityFunctionConstraints(ProbabilityFunction probabilityFunction) {
		double l = 0;
		for (double x = -5; x <= 6; x += 0.1) {
			double r = probabilityFunction.getProbability(x);
			if (x <= 0) {
				if (r != 0) {
					fail("Constraint Violated for x=" + x + " the expected value is 0.0 and was " + r);
				}
			} else if (x >= 1) {
				if (r != 1) {
					fail("Constraint Violated for x=" + x + " the expected value is 1.0 and was " + r);
				}
			} else {
				if (r < 0 || r > 1) {
					fail("Constraint Violated for x=" + x + " the expected value must be between [0,1] and was " + r);
				}
				if (r < l) { // monotonically increasing
					fail("Constraint Violated for x=" + x
							+ " the function must be monotonically increasing and the previos value was " + l
							+ " and the current value is " + r);
				}
				l = r;
			}

		}
	}

}
