package ar.com.siripo.arcache;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import ar.com.siripo.arcache.backend.test.ArcacheInMemoryTestBackend;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheClientTest {

	ArcacheInMemoryTestBackend backendClient;
	ArcacheClient arcache;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryTestBackend();
		arcache = new ArcacheClient(backendClient);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAsyncGetSet() throws Exception {

		String storeValue = "SOME VALUE";
		String key = "key";

		Future<Boolean> setFuture = arcache.asyncSet(key, storeValue);

		assertTrue(setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());

		Future<CacheGetResult> getFuture = arcache.asyncGetCacheObject(key);

		CacheGetResult cacheGetResult = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(cacheGetResult);

		assertThat(cacheGetResult.value, instanceOf(storeValue.getClass()));

		assertEquals(cacheGetResult.value, storeValue);

	}

	@Test
	public void testInvalidStoredObjectType() throws Exception {
		String key = "testInvalidStoredObjectType";

		backendClient.asyncSet(key, 1000, new Vector<Object>()).get();

		Future<CacheGetResult> getFuture = arcache.asyncGetCacheObject(key);

		CacheGetResult cacheGetResult = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.ERROR);

		assertThat(cacheGetResult.cause, instanceOf(UnexpectedObjectType.class));
	}

	@Test
	public void testHit() throws Exception {
		String key = "testHit";
		String val = "value";

		arcache.set(key, val);
		CacheGetResult cacheGetResult = arcache.getCacheObject(key);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		assertEquals(cacheGetResult.value, val);
	}

	@Test
	public void testMiss() throws Exception {
		String key = "testMiss";

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.MISS);

		assertEquals(cacheGetResult.value, null);
	}

	@Test
	public void testFullExpired() throws Exception {
		String key = "testFullExpired";
		String val = "value";

		arcache.set(key, val);

		// Alter the timeout
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestamp = k.timestamp - k.maxTTLSecs - 1;
		backendClient.set(key, 10, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);
		assertEquals(cacheGetResult.value, val);
	}

	@SuppressWarnings("serial")
	@Test
	public void testProbabilisticExpiration() throws Exception {
		String key = "testProbabilisticExpiration";
		CacheGetResult cacheGetResult;

		arcache.set(key, "val");

		// Alter the timeout, in 20% off time
		// The expected behavior is have a 20% of expiration probability
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestamp = k.timestamp - k.minTTLSecs - ((k.maxTTLSecs - k.minTTLSecs) * 2 / 10);
		backendClient.set(key, 10, k);

		// Expired
		arcache.randomGenerator = new Random() {
			public double nextDouble() {
				return (0);
			}
		};
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);

		// Near but Expired
		arcache.randomGenerator = new Random() {
			public double nextDouble() {
				return (0.18);
			}
		};
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);

		// Near OK
		arcache.randomGenerator = new Random() {
			public double nextDouble() {
				return (0.22);
			}
		};
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// OK
		arcache.randomGenerator = new Random() {
			public double nextDouble() {
				return (0.50);
			}
		};
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// OK
		arcache.randomGenerator = new Random() {
			public double nextDouble() {
				return (1);
			}
		};
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

	}

}
