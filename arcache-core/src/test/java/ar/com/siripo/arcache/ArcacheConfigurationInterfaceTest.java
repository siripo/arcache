package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;

public class ArcacheConfigurationInterfaceTest {

	ArcacheConfigurationInterface config;

	@Before
	public void setUp() throws Exception {
		config = new ArcacheClient();
	}

	@Test
	public void testSetDefaultOperationTimeout() {
		config.setDefaultOperationTimeoutMillis(1);
		assertEquals(config.getDefaultOperationTimeoutMillis(), 1);

		try {
			config.setDefaultOperationTimeoutMillis(0);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetRelaxOperationTimeoutInHeavyLoadSystem() {
		assertTrue("RelaxOperationTimeoutInHeavyLoadSystem default is expected to be true",
				config.getRelaxOperationTimeoutInHeavyLoadSystem());
		config.setRelaxOperationTimeoutInHeavyLoadSystem(false);
		assertFalse(config.getRelaxOperationTimeoutInHeavyLoadSystem());
		config.setRelaxOperationTimeoutInHeavyLoadSystem(true);
		assertTrue(config.getRelaxOperationTimeoutInHeavyLoadSystem());
	}

	@Test
	public void testSetTimeMeasurementError() {
		config.setTimeMeasurementErrorMillis(2);
		assertEquals(config.getTimeMeasurementErrorMillis(), 2);

		try {
			config.setTimeMeasurementErrorMillis(-1);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetDefaultInvalidationWindow() {
		config.setDefaultInvalidationWindowMillis(3);
		assertEquals(config.getDefaultInvalidationWindowMillis(), 3);

		try {
			config.setDefaultInvalidationWindowMillis(-1);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetDefaultHardInvalidation() {
		config.setDefaultHardInvalidation(false);
		assertEquals(config.getDefaultHardInvalidation(), false);
	}

	@Test
	public void testSetKeyNamespace() {
		config.setKeyNamespace(null);
		assertEquals(config.getKeyNamespace(), null);

		config.setKeyNamespace("");
		assertEquals(config.getKeyNamespace(), null);

		config.setKeyNamespace("aaa");
		assertEquals(config.getKeyNamespace(), "aaa");
	}

	@Test
	public void testSetKeyDelimiter() {
		config.setKeyDelimiter("//");
		assertEquals(config.getKeyDelimiter(), "//");

		try {
			config.setKeyDelimiter("");
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			config.setKeyDelimiter(null);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetDefaultExpirationTime() {
		config.setDefaultExpirationTimeMillis(4);
		assertEquals(config.getDefaultExpirationTimeMillis(), 4);

		try {
			config.setDefaultExpirationTimeMillis(0);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetDefaultStoredObjectRemovalTime() {
		config.setDefaultStoredObjectRemovalTimeMillis(5);
		assertEquals(config.getDefaultStoredObjectRemovalTimeMillis(), 5);

		try {
			config.setDefaultStoredObjectRemovalTimeMillis(0);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetBackendClient() {
		ArcacheBackendClient backendClient = new ArcacheInMemoryClient();
		config.setBackendClient(backendClient);
		assertEquals(config.getBackendClient(), backendClient);
	}

}
