package ar.com.siripo.arcache;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.math.LinearProbabilityFunction;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheClientTest {

	ArcacheInMemoryClient backendClient;
	ArcacheClient arcache;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryClient();
		arcache = new ArcacheClient(backendClient);
		arcache.setDefaultHardInvalidation(false);
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

		assertThat(cacheGetResult.errorCause, instanceOf(UnexpectedObjectType.class));
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
		k.timestampMillis = k.timestampMillis - k.expirationTTLMillis - 1;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);
		assertEquals(cacheGetResult.value, val);
	}

	@Test
	public void testProbabilisticExpirationLinear() throws Exception {
		arcache.setExpirationProbabilityFunction(new LinearProbabilityFunction(0.5));
		arcache.setInvalidationProbabilityFunction(new LinearProbabilityFunction(0));

		String key = "testProbabilisticExpirationLinear";
		CacheGetResult cacheGetResult;

		arcache.set(key, "val");

		// Alter the timeout, in 20% off time
		// The expected behavior is have a 20% of expiration probability
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - (k.expirationTTLMillis / 2) - ((k.expirationTTLMillis / 2) * 2 / 10);
		backendClient.set(key, 10000, k);

		// Expired
		arcache.randomGenerator = new StaticDoubleRandom(0);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);

		// Near but Expired
		arcache.randomGenerator = new StaticDoubleRandom(0.18);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);

		// Near OK
		arcache.randomGenerator = new StaticDoubleRandom(0.22);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// OK
		arcache.randomGenerator = new StaticDoubleRandom(0.5);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// OK
		arcache.randomGenerator = new StaticDoubleRandom(1);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

	}

	@Test
	public void testProbabilisticExpirationDefaultExponential11() throws Exception {

		String key = "testProbabilisticExpiration";
		CacheGetResult cacheGetResult;

		arcache.set(key, "val");

		// 90% of expiration zone
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.expirationTTLMillis = 100000;
		k.timestampMillis = k.timestampMillis - (k.expirationTTLMillis * 19 / 20);
		backendClient.set(key, 10000, k);

		// Expired
		arcache.randomGenerator = new StaticDoubleRandom(0);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.EXPIRED, cacheGetResult.type);

		// Near but Expired
		arcache.randomGenerator = new StaticDoubleRandom(0.2);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.EXPIRED, cacheGetResult.type);

		// Near OK
		arcache.randomGenerator = new StaticDoubleRandom(0.5);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.HIT, cacheGetResult.type);

		// OK
		arcache.randomGenerator = new StaticDoubleRandom(0.7);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.HIT, cacheGetResult.type);

		// OK
		arcache.randomGenerator = new StaticDoubleRandom(1);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.HIT, cacheGetResult.type);

	}

	@Test
	public void testInvalidationKeysStoredInResult() throws Exception {
		String key = "testInvalidationKeysStored";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);
		CacheGetResult cacheGetResult = arcache.getCacheObject(key);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);
		assertEquals(cacheGetResult.value, val);

		assertArrayEquals(invKeys, cacheGetResult.invalidationKeys);
	}

	@Test
	public void testSoftInvalidation() throws Exception {
		String key = "testInvalidation";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);

		// Update main key timestamp
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - (k.expirationTTLMillis / 2) + 1;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		arcache.invalidateKey(invKeys[0], false);

		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.INVALIDATED);
		assertEquals(cacheGetResult.value, val);
		assertEquals(invKeys[0], cacheGetResult.invalidatedKey);

		assertArrayEquals(invKeys, cacheGetResult.invalidationKeys);

	}

	@Test
	public void testHardInvalidation() throws Exception {
		String key = "testHardInvalidation";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);

		// Update main key timestamp
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - (k.expirationTTLMillis / 2) + 1;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		arcache.invalidateKey(invKeys[0], true);

		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.MISS);
		assertEquals(cacheGetResult.value, null);
		assertNull(cacheGetResult.invalidatedKey);
		assertNull(cacheGetResult.invalidationKeys);
	}

	@Test
	public void testInvalidationOverExpiration() throws Exception {
		String key = "testInvalidationOverExpiration";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);

		// Update main key timestamp to a Expired value
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - k.expirationTTLMillis - 10;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.EXPIRED);

		arcache.invalidateKey(invKeys[0], false);

		cacheGetResult = arcache.getCacheObject(key);
		// When is invalidated and expired, the invalidation has precedence
		assertEquals(cacheGetResult.type, CacheGetResult.Type.INVALIDATED);
		assertEquals(cacheGetResult.value, val);
	}

	@Test
	public void testInvalidationOverwrite() throws Exception {
		String invalidationKey = "invkey";
		String backendKey = arcache.createInvalidationBackendKey(invalidationKey);
		CacheInvalidationObject invalidationObject;
		// Test the invalidation, and overwrite
		invalidationObject = (CacheInvalidationObject) backendClient.get(backendKey);

		assertNull(invalidationObject);

		arcache.invalidateKey(invalidationKey, false, 10);

		invalidationObject = (CacheInvalidationObject) backendClient.get(backendKey);

		assertNotNull(invalidationObject);
		assertEquals(invalidationObject.invalidationWindowMillis, 10);
		assertEquals(invalidationObject.isHardInvalidation, false);
		assertEquals(invalidationObject.lastHardInvalidationTimestampMillis, 0);
		assertEquals(invalidationObject.lastSoftInvalidationTimestampMillis, 0);

		// send 10 secs before the timestamp
		invalidationObject.invalidationTimestampMillis -= 10;
		backendClient.set(backendKey, 10000, invalidationObject);
		long lastSoftTimestamp = invalidationObject.invalidationTimestampMillis;

		// Overwrite with hard
		arcache.invalidateKey(invalidationKey, true, 15000);

		invalidationObject = (CacheInvalidationObject) backendClient.get(backendKey);

		assertNotNull(invalidationObject);
		assertEquals(invalidationObject.invalidationWindowMillis, 15000);
		assertEquals(invalidationObject.isHardInvalidation, true);
		assertEquals(invalidationObject.lastHardInvalidationTimestampMillis, 0);
		assertEquals(invalidationObject.lastSoftInvalidationTimestampMillis, lastSoftTimestamp);

		long lastHardTimestamp = invalidationObject.invalidationTimestampMillis;

		// Overwrite with Soft
		arcache.invalidateKey(invalidationKey, false, 13000);

		invalidationObject = (CacheInvalidationObject) backendClient.get(backendKey);

		assertNotNull(invalidationObject);
		assertEquals(invalidationObject.invalidationWindowMillis, 13000);
		assertEquals(invalidationObject.isHardInvalidation, false);
		assertEquals(invalidationObject.lastHardInvalidationTimestampMillis, lastHardTimestamp);
		assertEquals(invalidationObject.lastSoftInvalidationTimestampMillis, lastSoftTimestamp);
	}

	@Test
	public void testInvalidationWithTimeMeasurementError() throws Exception {
		String key = "testInvalidation";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);

		// Update main key timestamp
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - 100000;
		k.expirationTTLMillis = 600000;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		arcache.invalidateKey(invKeys[0], false, 0);

		arcache.timeMeasurementErrorMillis = 90000;

		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.INVALIDATED);

		arcache.timeMeasurementErrorMillis = 110000;

		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);
	}

	@Test
	public void testInvalidationWindowLinear() throws Exception {

		arcache.setExpirationProbabilityFunction(new LinearProbabilityFunction(0.5));
		arcache.setInvalidationProbabilityFunction(new LinearProbabilityFunction(0));

		String key = "testInvalidation";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);

		// Update main key timestamp
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - 100000;
		k.expirationTTLMillis = 600000;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		arcache.invalidateKey(invKeys[0], false, 200000);
		arcache.timeMeasurementErrorMillis = 50000;

		// Invalidated
		arcache.randomGenerator = new StaticDoubleRandom(0);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.INVALIDATED);

		// Invalidated
		arcache.randomGenerator = new StaticDoubleRandom(0.74);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.INVALIDATED);

		// HIT
		arcache.randomGenerator = new StaticDoubleRandom(0.76);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// HIT
		arcache.randomGenerator = new StaticDoubleRandom(1);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// Full invalidated hard
		arcache.invalidateKey(invKeys[0], true, 50000);
		arcache.timeMeasurementErrorMillis = 0;

		arcache.randomGenerator = new StaticDoubleRandom(1);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.MISS);

	}

	@Test
	public void testInvalidationWindowDefaultExponential11() throws Exception {

		String key = "testInvalidation";
		String val = "value";
		String[] invKeys = new String[] { "invkey1", "invkey2" };

		arcache.set(key, val, invKeys);

		// Update main key timestamp
		ExpirableCacheObject k = (ExpirableCacheObject) backendClient.get(key);
		k.timestampMillis = k.timestampMillis - 90000;
		k.expirationTTLMillis = 1000000;
		backendClient.set(key, 10000, k);

		CacheGetResult cacheGetResult = arcache.getCacheObject(key);
		assertEquals(cacheGetResult.type, CacheGetResult.Type.HIT);

		// window 100 sec, age 90 sec, (90% inside invalidation window)
		arcache.invalidateKey(invKeys[0], false, 100000);
		arcache.timeMeasurementErrorMillis = 0;

		// Invalidated
		arcache.randomGenerator = new StaticDoubleRandom(0);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.INVALIDATED, cacheGetResult.type);

		// Invalidated
		arcache.randomGenerator = new StaticDoubleRandom(0.2);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.INVALIDATED, cacheGetResult.type);

		// HIT
		arcache.randomGenerator = new StaticDoubleRandom(0.7);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.HIT, cacheGetResult.type);

		// HIT
		arcache.randomGenerator = new StaticDoubleRandom(1);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.HIT, cacheGetResult.type);

		// Full invalidated hard
		arcache.invalidateKey(invKeys[0], true, 50000);
		arcache.timeMeasurementErrorMillis = 0;

		arcache.randomGenerator = new StaticDoubleRandom(1);
		cacheGetResult = arcache.getCacheObject(key);
		assertEquals(CacheGetResult.Type.MISS, cacheGetResult.type);

	}

	@SuppressWarnings("serial")
	private static class StaticDoubleRandom extends Random {
		double rv;

		StaticDoubleRandom(double v) {
			rv = v;
		}

		public double nextDouble() {
			return (rv);
		}
	}

}
