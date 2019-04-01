package ar.com.siripo.arcache.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

public class ObjectSerializerTest {

	ObjectSerializer serializer;

	@Before
	public void setUp() throws Exception {
		serializer = new ObjectSerializer();
	}

	@Test
	public void testSerializationHappyPath() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("SomeString");
		hs.add("SecondString");

		byte data[] = serializer.serializeToByteArray(hs);
		Object retrievedValue = serializer.deserialize(data);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertNotSame(retrievedValue, hs);
	}

	@Test
	public void testDeserializeBorderCases() {
		// Null input
		assertNull(serializer.deserialize(null));

		// Io Exception
		assertNull(serializer.deserialize(new byte[0]));

		// A valid but inexistent class
		byte data[] = new byte[] { -84, -19, 0, 5, 115, 114, 0, 94, 97, 114, 46, 99, 111, 109, 46, 115, 105, 114, 105,
				112, 111, 46, 97, 114, 99, 97, 99, 104, 101, 46, 98, 97, 99, 107, 101, 110, 100, 46, 116, 101, 115, 116,
				46, 65, 114, 99, 97, 99, 104, 101, 73, 110, 77, 101, 109, 111, 114, 121, 84, 101, 115, 116, 66, 97, 99,
				107, 101, 110, 100, 84, 101, 115, 116, 36, 83, 116, 114, 97, 110, 103, 101, 67, 108, 97, 115, 115, 70,
				111, 114, 83, 101, 114, 105, 97, 108, 105, 122, 97, 116, 105, 111, 110, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0,
				120, 112 };
		assertNull(serializer.deserialize(data));

	}

	@Test
	public void testSerializeNull() {
		assertNull(serializer.serializeToByteArray(null));
	}

	@Test
	public void testSerializeNonSerializableObject() {
		final class NonSerializableClass {
		}

		try {
			serializer.serializeToByteArray(new NonSerializableClass());
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}

	}

}
