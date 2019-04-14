package ar.com.siripo.arcache.backend.speedup.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.backend.speedup.ArcacheSpeedupBasicTracker;
import ar.com.siripo.arcache.backend.speedup.ArcacheSpeedupClient;
import ar.com.siripo.arcache.math.LinearProbabilityFunction;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public class ArcacheSpeedupClientFactoryBeanTest {

	ArcacheSpeedupClientFactoryBean factoryBean;
	ArcacheSpeedupClient speedupClient;

	@Before
	public void setUp() throws Exception {
		factoryBean = new ArcacheSpeedupClientFactoryBean();
		speedupClient = factoryBean.client;
	}

	@Test
	public void testArcacheSpeedupClientFactoryBean() {
		factoryBean = new ArcacheSpeedupClientFactoryBean();
		assertNotNull(factoryBean.client);
	}

	@Test
	public void testDestroy() throws Exception {
		assertTrue(factoryBean.getObject() instanceof ArcacheSpeedupClient);
		factoryBean.destroy();
		assertEquals(factoryBean.getObject(), null);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		factoryBean.setInvalidationKeysCacheSize(10);
		factoryBean.setInvalidationKeysExpirationMillis(10);
		factoryBean.setBackendClient(new ArcacheInMemoryClient());
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testGetObject() throws Exception {
		assertEquals(ArcacheSpeedupClient.class, factoryBean.getObject().getClass());
	}

	@Test
	public void testGetObjectType() {
		assertEquals(ArcacheSpeedupClient.class, factoryBean.getObjectType());
	}

	@Test
	public void testIsSingleton() {
		assertTrue(factoryBean.isSingleton());
	}

	@Test
	public void testSetBackendClient() {
		ArcacheBackendClient bc = new ArcacheInMemoryClient();
		factoryBean.setBackendClient(bc);
		assertEquals(bc, speedupClient.getBackendClient());
	}

	@Test
	public void testSetInvalidationKeysCacheSize() {
		factoryBean.setInvalidationKeysCacheSize(1);
		assertEquals(1, speedupClient.getInvalidationKeysCacheSize());
	}

	@Test
	public void testSetObjectsCacheSize() {
		factoryBean.setObjectsCacheSize(2);
		assertEquals(2, speedupClient.getObjectsCacheSize());
	}

	@Test
	public void testSetMissesCacheSize() {
		factoryBean.setMissesCacheSize(3);
		assertEquals(3, speedupClient.getMissesCacheSize());
	}

	@Test
	public void testSetInvalidationKeysExpirationMillis() {
		factoryBean.setInvalidationKeysExpirationMillis(4);
		assertEquals(4, speedupClient.getInvalidationKeysExpirationMillis());
	}

	@Test
	public void testSetObjectsExpirationMillis() {
		factoryBean.setObjectsExpirationMillis(5);
		assertEquals(5, speedupClient.getObjectsExpirationMillis());
	}

	@Test
	public void testSetMissesExpirationMillis() {
		factoryBean.setMissesExpirationMillis(5);
		assertEquals(5, speedupClient.getMissesExpirationMillis());
	}

	@Test
	public void testSetProtectAgainstBackendFailures() {
		factoryBean.setProtectAgainstBackendFailures(true);
		assertEquals(true, speedupClient.getProtectAgainstBackendFailures());

		factoryBean.setProtectAgainstBackendFailures(false);
		assertEquals(false, speedupClient.getProtectAgainstBackendFailures());
	}

	@Test
	public void testSetSpeedupCacheTTLMillis() {
		factoryBean.setSpeedupCacheTTLMillis(6);
		assertEquals(6, speedupClient.getSpeedupCacheTTLMillis());
	}

	@Test
	public void testSetExpirationProbabilityFunction() {
		ProbabilityFunction pf = new LinearProbabilityFunction(0);
		factoryBean.setExpirationProbabilityFunction(pf);
		assertEquals(pf, speedupClient.getExpirationProbabilityFunction());
	}

	@Test
	public void testSetTracker() {
		ArcacheSpeedupBasicTracker bt = new ArcacheSpeedupBasicTracker();
		factoryBean.setTracker(bt);
		assertEquals(bt, speedupClient.getTracker());
	}

	@Test
	public void testSetCacheIsolation() {
		factoryBean.setCacheIsolation(true);
		assertEquals(true, speedupClient.getCacheIsolation());

		factoryBean.setCacheIsolation(false);
		assertEquals(false, speedupClient.getCacheIsolation());
	}
}
