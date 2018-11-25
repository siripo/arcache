package ar.com.siripo.arcache.backend.memcached;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import net.spy.memcached.MemcachedClient;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheMemcachedClientTest {

	ArcacheMemcachedClient client;
	MemcachedClient memcachedClient;

	@Before
	public void setUp() throws Exception {
		memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11211));
		client = new ArcacheMemcachedClient(memcachedClient);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test001MemcacheConnection() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("THEVALUE");

		Future<Boolean> setFuture = memcachedClient.set("TESTMEMCACHEKEY", 10, hs);

		try {
			if (!setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue()) {
				fail("Failed to store");
			}
		} catch (TimeoutException e) {
			fail("Store Timeout, Maybe Memcached Not Running");
		}

		Future<Object> getFuture = memcachedClient.asyncGet("TESTMEMCACHEKEY");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertFalse(retrievedValue == hs);
	}

	@Test
	public void test002ClientGetSet() throws Exception {
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
}
