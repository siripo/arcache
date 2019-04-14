package ar.com.siripo.arcache.math;

import static org.junit.Assert.fail;

import org.junit.Test;

public class AbstractProbabilityFunctionTest {

	@Test
	public void testSetGraceZone() {
		AbstractProbabilityFunction myfunc = new AbstractProbabilityFunction() {
			@Override
			public double getProbability(double x) {
				return 0;
			}
		};

		try {
			myfunc.setGraceZone(-1);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException ia) {

		}

		try {
			myfunc.setGraceZone(1);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException ia) {

		}

		try {
			myfunc.setGraceZone(2);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException ia) {

		}

		myfunc.setGraceZone(0);
		myfunc.setGraceZone(0.5);
		myfunc.setGraceZone(0.999);
	}

}
