package ar.com.siripo.arcache.backend.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

		if (!setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue()) {
			fail("Failed to store");
		}

		Future<Object> getFuture = client.asyncGet("TESTCLIENT");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertFalse(retrievedValue == hs);
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

		assertFalse(retrievedValue == hs);
	}
}
