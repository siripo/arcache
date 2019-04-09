package ar.com.siripo.arcache.backend.speedup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

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
			protected void storeSpeedupCache(String key, Object value) {
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
}
