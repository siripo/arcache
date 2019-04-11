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
import net.spy.memcached.internal.OperationFuture;

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

		Future<Boolean> setFuture = client.asyncSet("TESTCLIENT", 10000, hs);

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

	private int testVerifyMillisToSecondConversion_exp;

	@Test
	public void testVerifyMillisToSecondConversion() throws Exception {
		memcachedClient = new MemcachedClient(new InetSocketAddress("localhost", 11211)) {
			@Override
			public OperationFuture<Boolean> set(String key, int exp, Object o) {
				testVerifyMillisToSecondConversion_exp = exp;
				return null;
			}
		};
		client = new ArcacheMemcachedClient(memcachedClient);

		client.asyncSet(null, 0, null);
		assertEquals(0, testVerifyMillisToSecondConversion_exp);

		client.asyncSet(null, 1, null);
		assertEquals(1, testVerifyMillisToSecondConversion_exp);

		client.asyncSet(null, 2, null);
		assertEquals(1, testVerifyMillisToSecondConversion_exp);

		client.asyncSet(null, 999, null);
		assertEquals(1, testVerifyMillisToSecondConversion_exp);

		client.asyncSet(null, 1000, null);
		assertEquals(1, testVerifyMillisToSecondConversion_exp);

		client.asyncSet(null, 1001, null);
		assertEquals(2, testVerifyMillisToSecondConversion_exp);

		client.asyncSet(null, 123456, null);
		assertEquals(124, testVerifyMillisToSecondConversion_exp);
	}
}
