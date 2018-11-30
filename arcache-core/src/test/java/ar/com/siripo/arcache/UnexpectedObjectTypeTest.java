package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UnexpectedObjectTypeTest {

	@Test
	public void testUnexpectedObjectType() {
		UnexpectedObjectType ux = new UnexpectedObjectType(String.class, Boolean.class);
		assertEquals(ux.expectedClass, String.class);
		assertEquals(ux.foundClass, Boolean.class);
	}

	@Test
	public void testToString() {
		UnexpectedObjectType ux = new UnexpectedObjectType(String.class, Boolean.class);
		String ts = ux.toString();
		assertEquals(ts, "UnexpectedObjectType expecting:java.lang.String found:java.lang.Boolean");
	}

}
