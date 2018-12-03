package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
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
		config.setDefaultOperationTimeout(1);
		assertEquals(config.getDefaultOperationTimeout(), 1);

		try {
			config.setDefaultOperationTimeout(0);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetTimeMeasurementError() {
		config.setTimeMeasurementError(2);
		assertEquals(config.getTimeMeasurementError(), 2);

		try {
			config.setTimeMeasurementError(-1);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetDefaultInvalidationWindow() {
		config.setDefaultInvalidationWindow(3);
		assertEquals(config.getDefaultInvalidationWindow(), 3);

		try {
			config.setDefaultInvalidationWindow(-1);
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
		config.setDefaultExpirationTime(4);
		assertEquals(config.getDefaultExpirationTime(), 4);

		try {
			config.setDefaultExpirationTime(0);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSetDefaultStoredObjectRemovalTime() {
		config.setDefaultStoredObjectRemovalTime(5);
		assertEquals(config.getDefaultStoredObjectRemovalTime(), 5);

		try {
			config.setDefaultStoredObjectRemovalTime(0);
			fail();
		} catch (IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testSetBackendClient() {
		ArcacheBackendClient backendClient=new ArcacheInMemoryClient();
		config.setBackendClient(backendClient);
		assertEquals(config.getBackendClient(), backendClient);
	}

}
