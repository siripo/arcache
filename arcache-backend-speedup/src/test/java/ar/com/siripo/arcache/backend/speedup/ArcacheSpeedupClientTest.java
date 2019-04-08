package ar.com.siripo.arcache.backend.speedup;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheSpeedupClientTest {

	ArcacheSpeedupClient client;
	ArcacheInMemoryClient backendClient;

	@Before
	public void setUp() throws Exception {

		backendClient = new ArcacheInMemoryClient();

		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);
		client.setInvalidationKeysCacheSize(100);
		client.setInvalidationKeysExpirationMillis(1000);
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

	}
}
