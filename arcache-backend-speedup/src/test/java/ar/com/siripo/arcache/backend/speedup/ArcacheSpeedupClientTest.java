package ar.com.siripo.arcache.backend.speedup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import ar.com.siripo.arcache.CacheInvalidationObject;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.math.AdjustedExponentialProbabilityFunction;
import ar.com.siripo.arcache.math.ProbabilityFunction;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheSpeedupClientTest {

	ArcacheSpeedupClient client;
	ArcacheInMemoryClient backendClient;

	@Before
	public void setUp() throws Exception {

		backendClient = new ArcacheInMemoryClient();

		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);
		client.setInvalidationKeysCacheSize(1000);
		client.setInvalidationKeysExpirationMillis(1000);
		client.setObjectsCacheSize(1000);
		client.setObjectsExpirationMillis(1000);
		client.setMissesCacheSize(1000);
		client.setMissesExpirationMillis(1000);
		client.initialize();
	}

	@Test
	public void testInitializationStrategys() throws Exception {

		// Test normal behavior
		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);
		client.setInvalidationKeysCacheSize(100);
		client.setInvalidationKeysExpirationMillis(1000);
		client.initialize();

		// Test fail on double initialization
		try {
			client.initialize();
			fail();
		} catch (IllegalStateException ise) {
		}

		// Test no Backend Defined
		client = new ArcacheSpeedupClient();
		client.setInvalidationKeysCacheSize(100);
		client.setInvalidationKeysExpirationMillis(1000);
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		// Test no policy defined
		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		// Test Specific Values to all
		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);
		ProbabilityFunction pf = new AdjustedExponentialProbabilityFunction(0.23, 2);
		client.setExpirationProbabilityFunction(pf);
		client.setInvalidationKeysCacheSize(1);
		client.setInvalidationKeysExpirationMillis(2);
		client.setMissesCacheSize(3);
		client.setMissesExpirationMillis(4);
		client.setObjectsCacheSize(5);
		client.setObjectsExpirationMillis(6);
		client.setProtectAgainstBackendFailures(false);
		client.setSpeedupCacheTTLSeconds(7);
		ArcacheSpeedupBasicTracker tracker = new ArcacheSpeedupBasicTracker();
		client.setTracker(tracker);
		client.initialize();

		assertEquals(backendClient, client.backendClient);
		assertEquals(pf, client.expirationProbabilityFunction);
		assertEquals(1, client.invalidationKeysCacheSize);
		assertNotNull(client.invalidationKeysCache);
		assertEquals(2, client.invalidationKeysExpirationMillis);
		assertEquals(3, client.missesCacheSize);
		assertNotNull(client.missesCache);
		assertEquals(4, client.missesExpirationMillis);
		assertEquals(5, client.objectsCacheSize);
		assertNotNull(client.objectsCache);
		assertEquals(6, client.objectsExpirationMillis);
		assertFalse(client.protectAgainstBackendFailures);
		assertEquals(7, client.speedupCacheTTLSeconds);

	}

	@Test
	public void testInitializationInvalidationKeys() throws Exception {

		// Test invalidation combinations
		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);

		client.invalidationKeysCacheSize = 0;
		client.invalidationKeysExpirationMillis = 0;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.invalidationKeysCacheSize = 1;
		client.invalidationKeysExpirationMillis = 0;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.invalidationKeysCacheSize = 0;
		client.invalidationKeysExpirationMillis = 1;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.invalidationKeysCacheSize = 1;
		client.invalidationKeysExpirationMillis = 1;
		client.initialize();

	}

	@Test
	public void testInitializationObjects() throws Exception {

		// Test invalidation combinations
		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);

		client.objectsCacheSize = 0;
		client.objectsExpirationMillis = 0;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.objectsCacheSize = 1;
		client.objectsExpirationMillis = 0;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.objectsCacheSize = 0;
		client.objectsExpirationMillis = 1;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.objectsCacheSize = 1;
		client.objectsExpirationMillis = 1;
		client.initialize();

	}

	@Test
	public void testInitializationMisses() throws Exception {

		// Test invalidation combinations
		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);

		client.missesCacheSize = 0;
		client.missesExpirationMillis = 0;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.missesCacheSize = 1;
		client.missesExpirationMillis = 0;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.missesCacheSize = 0;
		client.missesExpirationMillis = 1;
		try {
			client.initialize();
			fail();
		} catch (IllegalArgumentException iae) {
		}

		client.missesCacheSize = 1;
		client.missesExpirationMillis = 1;
		client.initialize();

	}

	@Test
	public void testValidateSetCacheSize() {
		client.validateSetCacheSize(100);
		client.validateSetCacheSize(1);
		client.validateSetCacheSize(0);
		try {
			client.validateSetCacheSize(-1);
			fail();
		} catch (IllegalArgumentException iae) {
		}
	}

	@Test
	public void testValidateSetExpirationMillis() {
		client.validateSetExpirationMillis(100);
		client.validateSetExpirationMillis(1);

		try {
			client.validateSetExpirationMillis(-1);
			fail();
		} catch (IllegalArgumentException iae) {
		}

		try {
			client.validateSetExpirationMillis(0);
			fail();
		} catch (IllegalArgumentException iae) {
		}
	}

	@Test
	public void testClear() {

		class ArcacheInMemoryClientMock extends ArcacheInMemoryClient {
			int clearcount = 0;

			@Override
			public void clear() {
				super.clear();
				clearcount++;
			}
		}

		ArcacheInMemoryClientMock mc = new ArcacheInMemoryClientMock();

		client.invalidationKeysCache = mc;
		client.missesCache = mc;
		client.objectsCache = mc;

		client.clear();

		assertEquals(3, mc.clearcount);

		mc.clearcount = 0;

		client.invalidationKeysCache = null;
		client.missesCache = null;
		client.objectsCache = null;

		client.clear();

		assertEquals(0, mc.clearcount);

	}

	@Test
	public void testAsyncSet() throws Exception {

		Object value;
		SpeedupCacheObject sco;

		value = new String("ABC");
		String key = "thekey";

		// Test set as a normal object and replace miss

		client.clear();
		backendClient.clear();
		client.missesCache.set(key, 1000, "aaa");
		assertNotNull(client.missesCache.get(key));
		client.asyncSet(key, 1234, value).get();
		assertEquals(value, backendClient.get(key));
		sco = (SpeedupCacheObject) client.objectsCache.get(key);
		assertEquals(value, sco.cachedObject);
		assertNull(client.missesCache.get(key));
		assertNull(client.invalidationKeysCache.get(key));

		// Test set as a invalidation object
		value = new CacheInvalidationObject();
		((CacheInvalidationObject) value).invalidationTimestamp = 1234567;

		client.clear();
		backendClient.clear();
		client.missesCache.set(key, 1000, "aaa");
		assertNotNull(client.missesCache.get(key));
		client.asyncSet(key, 1234, value).get();
		assertEquals(1234567, ((CacheInvalidationObject) backendClient.get(key)).invalidationTimestamp);
		sco = (SpeedupCacheObject) client.invalidationKeysCache.get(key);
		assertEquals(1234567, ((CacheInvalidationObject) sco.cachedObject).invalidationTimestamp);
		assertNull(client.missesCache.get(key));
		assertNull(client.objectsCache.get(key));

	}

	@Test
	public void testAsyncSetWithFailedSpeedupOperation() throws Exception {

		/* When the storeSpeedup fails, the set over backend must proceed */
		client = new ArcacheSpeedupClient() {
			@Override
			protected ArcacheInMemoryClient storeSpeedupCache(String key, Object value) {
				throw new IllegalStateException();
			}
		};
		client.setBackendClient(backendClient);
		client.setInvalidationKeysCacheSize(1000);
		client.setInvalidationKeysExpirationMillis(1000);
		client.initialize();

		Object value = new String("ABC");
		String key = "thekey";

		client.asyncSet(key, 1234, value).get();
		assertEquals(value, backendClient.get(key));
	}

	/* Test the storing in one bucket and the removal from a previous one */
	@Test
	public void testStoreSpeedupCacheSwapFromCaches() {
		String key = "thekey";
		Object objvalue = "Hello";
		Object invvalue = new CacheInvalidationObject();
		client.clear();

		// Store as object
		assertEquals(client.objectsCache, client.storeSpeedupCache(key, objvalue));
		assertNull(client.invalidationKeysCache.get(key));
		assertNotNull(client.objectsCache.get(key));
		assertNull(client.missesCache.get(key));

		// Store as miss and expect remove from object
		assertEquals(client.missesCache, client.storeSpeedupCache(key, null));
		assertNull(client.invalidationKeysCache.get(key));
		assertNull(client.objectsCache.get(key));
		assertNotNull(client.missesCache.get(key));

		// Store as invalidationKey and expect remove from miss
		assertEquals(client.invalidationKeysCache, client.storeSpeedupCache(key, invvalue));
		assertNotNull(client.invalidationKeysCache.get(key));
		assertNull(client.objectsCache.get(key));
		assertNull(client.missesCache.get(key));

		// Store as miss and expect remove from invalidationKey
		assertEquals(client.missesCache, client.storeSpeedupCache(key, null));
		assertNull(client.invalidationKeysCache.get(key));
		assertNull(client.objectsCache.get(key));
		assertNotNull(client.missesCache.get(key));

		// Store as inv key
		assertEquals(client.invalidationKeysCache, client.storeSpeedupCache(key, invvalue));
		assertNotNull(client.invalidationKeysCache.get(key));
		assertNull(client.objectsCache.get(key));
		assertNull(client.missesCache.get(key));

		// Store as object and expect remove from invKey
		assertEquals(client.objectsCache, client.storeSpeedupCache(key, objvalue));
		assertNull(client.invalidationKeysCache.get(key));
		assertNotNull(client.objectsCache.get(key));
		assertNull(client.missesCache.get(key));

		// Store as inv key and expect remove from object
		assertEquals(client.invalidationKeysCache, client.storeSpeedupCache(key, invvalue));
		assertNotNull(client.invalidationKeysCache.get(key));
		assertNull(client.objectsCache.get(key));
		assertNull(client.missesCache.get(key));

	}

	@Test
	public void testStoreSpeedupCacheWithNoAllCaches() {

		String key = "thekey";
		Object objvalue = "Hello";
		Object invvalue = new CacheInvalidationObject();
		client.clear();

		ArcacheInMemoryClient objectsCache = client.objectsCache;
		ArcacheInMemoryClient invalidationKeysCache = client.invalidationKeysCache;
		ArcacheInMemoryClient missesCache = client.missesCache;

		client.invalidationKeysCache = null;
		client.objectsCache = null;
		client.missesCache = null;

		assertEquals(null, client.storeSpeedupCache(key, invvalue));
		assertEquals(null, client.storeSpeedupCache(key, objvalue));
		assertEquals(null, client.storeSpeedupCache(key, null));

		client.invalidationKeysCache = invalidationKeysCache;
		assertEquals(invalidationKeysCache, client.storeSpeedupCache(key, invvalue));
		assertNotNull(client.invalidationKeysCache.get(key));
		assertEquals(null, client.storeSpeedupCache(key, objvalue));
		assertNull(client.invalidationKeysCache.get(key));

		client.invalidationKeysCache = null;
		client.objectsCache = objectsCache;
		assertEquals(client.objectsCache, client.storeSpeedupCache(key, objvalue));
		assertNotNull(client.objectsCache.get(key));
		assertEquals(null, client.storeSpeedupCache(key, invvalue));
		assertNull(client.objectsCache.get(key));

		client.objectsCache = null;
		client.missesCache = missesCache;
		assertEquals(client.missesCache, client.storeSpeedupCache(key, null));
		assertNotNull(client.missesCache.get(key));
		assertEquals(null, client.storeSpeedupCache(key, invvalue));
		assertNull(client.missesCache.get(key));
	}

	@Test
	public void testAsyncGet() throws Exception {
		// ----------------------------------------------------------------
		// First the happiest path with cached value
		Object value = new String("ABC");
		Object invkey = new CacheInvalidationObject();
		String key = "thekey";

		client.random = new StaticDoubleRandom(1);
		client.asyncSet(key, 1234, value).get();
		assertEquals(value, backendClient.get(key));
		assertEquals(value, client.asyncGet(key).get());

		// Alter the backend value to verify if its restored from speedup Cache
		backendClient.set(key, 1234, "lala");
		assertNotEquals(value, backendClient.get(key));
		assertEquals(value, client.asyncGet(key).get());

		// Test no speedup cache value
		client.clear();
		assertNotEquals(value, client.asyncGet(key).get());

		// ----------------------------------------------------------------
		// Test store and restore invalidationkeys

		client.asyncSet(key, 1234, invkey).get();
		assertEquals(invkey.getClass(), backendClient.get(key).getClass());
		assertEquals(invkey.getClass(), client.asyncGet(key).get().getClass());

		// Clear backend, and restore from invkeycache
		backendClient.clear();
		assertNull(backendClient.get(key));
		assertEquals(invkey.getClass(), client.asyncGet(key).get().getClass());
		client.invalidationKeysCache.clear();
		assertNull(client.asyncGet(key).get());

		// ----------------------------------------------------------------
		// Test no speedup cache, next restore, after that must have cache value
		client.clear();
		backendClient.clear();
		client.asyncSet(key, 1234, value).get();
		client.clear();
		assertNull(client.restoreObjectFromAnySpeedupCache(key));
		// Now restore form backend
		assertEquals(value, client.asyncGet(key).get());
		assertNotNull(client.restoreObjectFromAnySpeedupCache(key));
		// Now alter the backend, and the cached value must be there
		backendClient.set(key, 1234, "lala");
		assertNotEquals(value, backendClient.get(key));
		assertEquals(value, client.asyncGet(key).get());
		client.clear();
		assertNotEquals(value, client.asyncGet(key).get());

		// ----------------------------------------------------------------
		// Test the miss behavior, Get a key that is miss in backend, check the miss,
		// after that store the key in the backend. and speedup must be miss.
		client.clear();
		backendClient.clear();
		assertNull(client.asyncGet(key).get());
		backendClient.set(key, 1234, value);
		// The miss cache must return miss
		assertNull(client.asyncGet(key).get());
		assertNotNull(client.missesCache.get(key));
		client.clear();
		// Now the value must be restored
		assertEquals(value, client.asyncGet(key).get());
		assertNotNull(client.objectsCache.get(key));

		// ----------------------------------------------------------------
		// Test the miss behavior with a InvalidationKey, Get a key that is miss in
		// backend, check the miss,
		// after that store the key in the backend. and speedup must be miss.
		client.clear();
		backendClient.clear();
		assertNull(client.asyncGet(key).get());
		backendClient.set(key, 1234, invkey);
		// The miss cache must return miss
		assertNull(client.asyncGet(key).get());
		assertNotNull(client.missesCache.get(key));
		client.clear();
		// Now the value must be restored
		assertEquals(invkey.getClass(), client.asyncGet(key).get().getClass());
		assertNotNull(client.invalidationKeysCache.get(key));

		// ----------------------------------------------------------------
		// Here test what happens with an expired speedup cache
		client = new ArcacheSpeedupClient() {
			@Override
			protected RestoredSpeedupCacheObject restoreObjectFromAnySpeedupCache(String key) {
				RestoredSpeedupCacheObject rsco = new RestoredSpeedupCacheObject();
				rsco.expired = true;
				rsco.speedupCacheObject = new SpeedupCacheObject();
				rsco.speedupCacheObject.cachedObject = "ll";
				return rsco;
			}
		};
		client.setBackendClient(backendClient);
		client.setObjectsCacheSize(1000);
		client.setObjectsExpirationMillis(1000);
		client.initialize();
		client.random = new StaticDoubleRandom(1);

		client.asyncSet(key, 1234, value).get();
		assertEquals(value, backendClient.get(key));
		assertEquals(value, client.asyncGet(key).get());

		// ----------------------------------------------------------------
		// Test exception restoring from speedup cache
		client = new ArcacheSpeedupClient() {
			@Override
			protected RestoredSpeedupCacheObject restoreObjectFromAnySpeedupCache(String key) {
				throw new IllegalStateException();
			}
		};
		client.setBackendClient(backendClient);
		client.setObjectsCacheSize(1000);
		client.setObjectsExpirationMillis(1000);
		client.initialize();
		client.random = new StaticDoubleRandom(1);

		client.asyncSet(key, 1234, value).get();
		assertEquals(value, backendClient.get(key));
		assertEquals(value, client.asyncGet(key).get());
		backendClient.set(key, 1234, "lala");
		assertNotEquals(value, client.asyncGet(key).get());

		// ----------------------------------------------------------------
		// Test exceptions in local cache
		client = new ArcacheSpeedupClient() {
			@Override
			protected RestoredSpeedupCacheObject restoreObjectFromAnySpeedupCache(String key) {
				throw new IllegalStateException();
			}

			@Override
			protected FutureBackendGetWrapper createFutureBackendGetWrapper(Future<Object> backendFuture, String key) {
				throw new IllegalStateException();
			}
		};
		client.setBackendClient(backendClient);
		client.setObjectsCacheSize(1000);
		client.setObjectsExpirationMillis(1000);
		client.initialize();

		client.asyncSet(key, 1234, value).get();
		assertEquals(value, backendClient.get(key));
		assertEquals(value, client.asyncGet(key).get());

	}

	@Test
	public void testRestoreObjectFromAnySpeedupCache() {
		// The behavior is to restore in order from invkeys,objets, then miss
		ArcacheSpeedupClient clientx = new ArcacheSpeedupClient() {
			@Override
			protected RestoredSpeedupCacheObject restoreObjectFromSpeedupCache(String key,
					ArcacheInMemoryClient speedupCache, long timeoutMillis) {
				if (key == "hit") {
					RestoredSpeedupCacheObject rsco = new RestoredSpeedupCacheObject();
					rsco.expired = false;
					rsco.speedupCacheObject = new SpeedupCacheObject();
					rsco.speedupCacheObject.cachedObject = speedupCache;
					return rsco;
				}
				return null;

			}
		};
		clientx.initialized = true;
		clientx.invalidationKeysCache = client.invalidationKeysCache;
		clientx.objectsCache = client.objectsCache;
		clientx.missesCache = client.missesCache;

		assertNull(clientx.restoreObjectFromAnySpeedupCache("miss"));
		assertNotNull(clientx.restoreObjectFromAnySpeedupCache("hit"));
		assertEquals(clientx.invalidationKeysCache,
				clientx.restoreObjectFromAnySpeedupCache("hit").speedupCacheObject.cachedObject);

		clientx.invalidationKeysCache = null;
		assertEquals(clientx.objectsCache,
				clientx.restoreObjectFromAnySpeedupCache("hit").speedupCacheObject.cachedObject);

		clientx.objectsCache = null;
		assertEquals(clientx.missesCache,
				clientx.restoreObjectFromAnySpeedupCache("hit").speedupCacheObject.cachedObject);

		clientx.missesCache = null;
		assertNull(clientx.restoreObjectFromAnySpeedupCache("hit"));

	}

	@Test
	public void testRestoreObjectFromSpeedupCache() {
		String key = "a";
		ArcacheInMemoryClient speedupCache = client.invalidationKeysCache;
		String value = "thevalue";

		assertNull(client.restoreObjectFromSpeedupCache(key, speedupCache, 1000));
		speedupCache.set(key, 1000, "InvalidType");

		// This is a rare case
		try {
			client.restoreObjectFromSpeedupCache(key, speedupCache, 1000);
			fail();
		} catch (ClassCastException ce) {

		}

		// store a valid type and check restore.
		client.random = new StaticDoubleRandom(1);
		SpeedupCacheObject sco = client.createSpeedupCacheObject(value);
		speedupCache.set(key, 1000, sco);
		RestoredSpeedupCacheObject rsco = client.restoreObjectFromSpeedupCache(key, speedupCache, 1000);
		assertEquals(value, rsco.speedupCacheObject.cachedObject);
		assertFalse(rsco.expired);

		// Now store an expired value
		sco.storeTimeMillis = System.currentTimeMillis() - 11000;
		speedupCache.set(key, 99999, sco);
		rsco = client.restoreObjectFromSpeedupCache(key, speedupCache, 10000);
		assertEquals(value, rsco.speedupCacheObject.cachedObject);
		assertTrue(rsco.expired);

		// Now store a value in the middle of invalidation window
		sco.storeTimeMillis = System.currentTimeMillis() - 5000;
		speedupCache.set(key, 99999, sco);
		client.random = new StaticDoubleRandom(1);
		assertFalse(client.restoreObjectFromSpeedupCache(key, speedupCache, 10000).expired);
		client.random = new StaticDoubleRandom(0);
		assertTrue(client.restoreObjectFromSpeedupCache(key, speedupCache, 10000).expired);
		client.random = new StaticDoubleRandom(0.5); // this value depends of the probability function
		assertFalse(client.restoreObjectFromSpeedupCache(key, speedupCache, 10000).expired);
	}

	@Test
	public void createFutureBackendGetWrapper() {
		String key = "key";
		Future<Object> fut = client.backendClient.asyncGet(key);

		client.protectAgainstBackendFailures = false;
		FutureBackendGetWrapper fbgw = client.createFutureBackendGetWrapper(fut, key);
		assertEquals(fut, fbgw.backendFuture);
		assertEquals(client, fbgw.speedupClient);
		assertEquals(false, fbgw.protectAgainstBackendFailures);
		assertEquals(key, fbgw.key);

		client.protectAgainstBackendFailures = true;
		fbgw = client.createFutureBackendGetWrapper(fut, key);
		assertEquals(true, fbgw.protectAgainstBackendFailures);
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
