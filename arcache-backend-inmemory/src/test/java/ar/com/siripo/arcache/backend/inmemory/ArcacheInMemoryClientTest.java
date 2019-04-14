package ar.com.siripo.arcache.backend.inmemory;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

	@Test
	public void testGetSetNoIsolation() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("OTHER");

		Future<Boolean> setFuture = client.asyncSet("TESTCLIENT", 10000, hs);

		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<Object> getFuture = client.asyncGet("TESTCLIENT");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		// With NO isolation, exactly the seme object is expected
		assertSame(hs, retrievedValue);
	}

	@Test
	public void testGetSetIsolation() throws Exception {
		client = new ArcacheInMemoryClient(100, true);
		HashSet<String> hs = new HashSet<String>();
		hs.add("OTHER");

		Future<Boolean> setFuture = client.asyncSet("TESTCLIENT", 10000, hs);

		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<Object> getFuture = client.asyncGet("TESTCLIENT");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		// With isolation, NOT the seme object is expected
		assertNotSame(retrievedValue, hs);
	}

	@Test
	public void testHitAndMiss() throws Exception {
		// Hit Scenario
		Future<Boolean> setFuture = client.asyncSet("HIT", 10000, "hello");
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
		Future<Boolean> setFuture = client.asyncSet("NULL", 10000, null);
		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<Object> getFuture = client.asyncGet("NULL");
		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);
		assertNull(retrievedValue);
	}

	@Test
	public void testClear() throws Exception {
		client.set("TESTCLIENT", 10000, "aa");
		assertNotEquals(client.storage.size(), 0);

		client.clear();
		assertEquals(client.storage.size(), 0);
	}

	@Test
	public void testExpiration() throws Exception {
		final String key = "k";
		assertTrue(client.set(key, 10000, "aa"));
		assertNotNull(client.get(key));
		MemoryObject mobj = client.storage.get(key);
		mobj.expirationTimeMillis = mobj.expirationTimeMillis - 8000;
		assertNotNull(client.get(key));
		mobj.expirationTimeMillis = mobj.expirationTimeMillis - 8000;
		assertNull(client.get(key));
	}

	@Test
	public void testLRUEviction() throws Exception {
		client = new ArcacheInMemoryClient(3);
		client.set("key1", 10000, "1");
		client.set("key2", 10000, "2");
		client.set("key3", 10000, "3");

		assertEquals("1", client.get("key1"));
		assertEquals("2", client.get("key2"));
		assertEquals("3", client.get("key3"));

		client.set("key4", 10000, "4");

		// Test eviction of Key1
		assertNull(client.get("key1"));
		assertEquals("2", client.get("key2"));
		assertEquals("3", client.get("key3"));
		assertEquals("4", client.get("key4"));

		// Test eviction in LRU manner
		assertEquals("4", client.get("key4"));
		assertEquals("3", client.get("key3"));
		assertEquals("2", client.get("key2"));

		client.set("key5", 10000, "5");

		assertNull(client.get("key1"));
		assertEquals("2", client.get("key2"));
		assertEquals("3", client.get("key3"));
		assertNull(client.get("key4"));
		assertEquals("5", client.get("key5"));

	}

	@Test
	public void testRemove() throws Exception {
		final String key = "k";
		assertTrue(client.set(key, 10000, "aa"));
		assertTrue(client.set("k2", 10000, "aa"));
		assertNotNull(client.get(key));
		client.remove(key);
		assertNull(client.get(key));
		assertNotNull(client.get("k2"));
	}
}
