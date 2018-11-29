package ar.com.siripo.arcache.backend.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.test.ArcacheInMemoryTestBackend.MemoryObject;

public class ArcacheInMemoryTestBackendTest {

	ArcacheInMemoryTestBackend client;

	@Before
	public void setUp() throws Exception {
		client = new ArcacheInMemoryTestBackend();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetSet() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("OTHER");

		Future<Boolean> setFuture = client.asyncSet("TESTCLIENT", 10, hs);

		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<Object> getFuture = client.asyncGet("TESTCLIENT");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertNotSame(retrievedValue, hs);
	}

	@Test
	public void testSerialization() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("SomeString");
		hs.add("SecondString");

		byte data[] = client.serialize(hs);
		Object retrievedValue = client.deserialize(data);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertNotSame(retrievedValue, hs);
	}

	@Test
	public void testDeserializeBorderCases() {
		// Null input
		assertNull(client.deserialize(null));

		// Io Exception
		assertNull(client.deserialize(new byte[0]));

		// A valid but inexistent class
		byte data[] = new byte[] { -84, -19, 0, 5, 115, 114, 0, 94, 97, 114, 46, 99, 111, 109, 46, 115, 105, 114, 105,
				112, 111, 46, 97, 114, 99, 97, 99, 104, 101, 46, 98, 97, 99, 107, 101, 110, 100, 46, 116, 101, 115, 116,
				46, 65, 114, 99, 97, 99, 104, 101, 73, 110, 77, 101, 109, 111, 114, 121, 84, 101, 115, 116, 66, 97, 99,
				107, 101, 110, 100, 84, 101, 115, 116, 36, 83, 116, 114, 97, 110, 103, 101, 67, 108, 97, 115, 115, 70,
				111, 114, 83, 101, 114, 105, 97, 108, 105, 122, 97, 116, 105, 111, 110, 0, 0, 0, 0, 0, 0, 0, 1, 2, 0, 0,
				120, 112 };
		assertNull(client.deserialize(data));

	}

	@Test
	public void testSerializeBorderCases() {
		try {
			client.serialize(null);
			fail("Expected NullPointerException");
		} catch (NullPointerException e) {
		}

		final class NonSerializableClass {
		}

		try {
			client.serialize(new NonSerializableClass());
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}

	}

	@Test
	public void testClear() throws Exception {
		client.set("TESTCLIENT", 10, "aa");
		assertNotEquals(client.storage.size(), 0);

		client.clear();
		assertEquals(client.storage.size(), 0);
	}

	@Test
	public void testExpiration() throws Exception {
		final String key = "k";
		assertTrue(client.set(key, 10, "aa"));
		assertNotNull(client.get(key));
		MemoryObject mobj = client.storage.get(key);
		mobj.expirationTime = mobj.expirationTime - 8000;
		assertNotNull(client.get(key));
		mobj.expirationTime = mobj.expirationTime - 8000;
		assertNull(client.get(key));
	}

}
