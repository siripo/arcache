package ar.com.siripo.arcache.backend.inmemory;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheInMemoryClientTest {

	ArcacheInMemoryClient client;

	@Before
	public void setUp() throws Exception {
		client = new ArcacheInMemoryClient();
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
	public void testHitAndMiss() throws Exception {
		// Hit Scenario
		Future<Boolean> setFuture = client.asyncSet("HIT", 10, "hello");
		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<Object> getFuture = client.asyncGet("HIT");
		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);
		assertEquals(retrievedValue, "hello");

		// Miss Scenario
		getFuture = client.asyncGet("MISS");
		retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);
		assertNull(retrievedValue);
	}

	@Test
	public void testNullValue() throws Exception {
		Future<Boolean> setFuture = client.asyncSet("NULL", 10, null);
		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<Object> getFuture = client.asyncGet("NULL");
		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);
		assertNull(retrievedValue);
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

	@Test
	public void testLRUEviction() throws Exception {
		client = new ArcacheInMemoryClient(3);
		client.set("key1", 10, "1");
		client.set("key2", 10, "2");
		client.set("key3", 10, "3");

		assertEquals("1", client.get("key1"));
		assertEquals("2", client.get("key2"));
		assertEquals("3", client.get("key3"));

		client.set("key4", 10, "4");

		// Test eviction of Key1
		assertNull(client.get("key1"));
		assertEquals("2", client.get("key2"));
		assertEquals("3", client.get("key3"));
		assertEquals("4", client.get("key4"));

		// Test eviction in LRU manner
		assertEquals("4", client.get("key4"));
		assertEquals("3", client.get("key3"));
		assertEquals("2", client.get("key2"));

		client.set("key5", 10, "5");

		assertNull(client.get("key1"));
		assertEquals("2", client.get("key2"));
		assertEquals("3", client.get("key3"));
		assertNull(client.get("key4"));
		assertEquals("5", client.get("key5"));

	}
}
