package ar.com.siripo.arcache.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.ArcacheClient;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public class ArcacheClientFactoryBeanTest {

	ArcacheClientFactoryBean factoryBean;

	@Before
	public void setUp() throws Exception {
		factoryBean = new ArcacheClientFactoryBean();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testDestroy() throws Exception {
		assertTrue(factoryBean.getObject() instanceof ArcacheClient);
		factoryBean.destroy();
		assertEquals(factoryBean.getObject(), null);
	}

	@Test
	public void testGetObject() throws Exception {
		assertTrue(factoryBean.getObject() instanceof ArcacheClient);
	}

	@Test
	public void testGetObjectType() {
		assertEquals(factoryBean.getObjectType(), ArcacheClient.class);
	}

	@Test
	public void testIsSingleton() {
		assertTrue(factoryBean.isSingleton());
	}

	@Test
	public void testSetDefaultOperationTimeout() throws Exception {
		factoryBean.setDefaultOperationTimeoutMillis(20);
		assertEquals(factoryBean.getObject().getDefaultOperationTimeoutMillis(), 20);
	}

	@Test
	public void testSetTimeMeasurementError() throws Exception {
		factoryBean.setTimeMeasurementErrorMillis(21);
		assertEquals(factoryBean.getObject().getTimeMeasurementErrorMillis(), 21);
	}

	@Test
	public void testSetDefaultInvalidationWindow() throws Exception {
		factoryBean.setDefaultInvalidationWindowMillis(22);
		assertEquals(factoryBean.getObject().getDefaultInvalidationWindowMillis(), 22);
	}

	@Test
	public void testSetDefaultHardInvalidation() throws Exception {
		factoryBean.setDefaultHardInvalidation(true);
		assertEquals(factoryBean.getObject().getDefaultHardInvalidation(), true);
		factoryBean.setDefaultHardInvalidation(false);
		assertEquals(factoryBean.getObject().getDefaultHardInvalidation(), false);
	}

	@Test
	public void testSetKeyNamespace() throws Exception {
		factoryBean.setKeyNamespace("AbC");
		assertEquals(factoryBean.getObject().getKeyNamespace(), "AbC");
	}

	@Test
	public void testSetKeyDelimiter() throws Exception {
		factoryBean.setKeyDelimiter("()");
		assertEquals(factoryBean.getObject().getKeyDelimiter(), "()");
	}

	@Test
	public void testSetDefaultExpirationTime() throws Exception {
		factoryBean.setDefaultExpirationTimeMillis(51);
		assertEquals(factoryBean.getObject().getDefaultExpirationTimeMillis(), 51);
	}

	@Test
	public void testSetDefaultStoredObjectRemovalTime() throws Exception {
		factoryBean.setDefaultStoredObjectRemovalTimeMillis(52);
		assertEquals(factoryBean.getObject().getDefaultStoredObjectRemovalTimeMillis(), 52);
	}

	@Test
	public void testSetBackendClient() throws Exception {
		ArcacheBackendClient mbc = new ArcacheBackendClient() {
			public Future<Boolean> asyncSet(String key, long ttlMillis, Object value) {
				return null;
			}

			public Future<Object> asyncGet(String key) {
				return null;
			}
		};

		factoryBean.setBackendClient(mbc);
		assertEquals(factoryBean.getObject().getBackendClient(), mbc);

		mbc.asyncSet(null, 1, 0);
		mbc.asyncGet(null);
	}

	@Test
	public void testSetExpirationProbabilityFunction() throws Exception {
		ProbabilityFunction myfunc = new ProbabilityFunction() {
			public double getProbability(double x) {
				return 0;
			}
		};
		factoryBean.setExpirationProbabilityFunction(myfunc);
		assertEquals(myfunc, factoryBean.getObject().getExpirationProbabilityFunction());
	}

	@Test
	public void testSetInvalidationProbabilityFunction() throws Exception {
		ProbabilityFunction myfunc = new ProbabilityFunction() {
			public double getProbability(double x) {
				return 0;
			}
		};
		factoryBean.setInvalidationProbabilityFunction(myfunc);
		assertEquals(myfunc, factoryBean.getObject().getInvalidationProbabilityFunction());
	}

	@Test
	public void testSetInvalidationBackendClient() throws Exception {
		ArcacheBackendClient bc = new ArcacheInMemoryClient();
		factoryBean.setInvalidationBackendClient(bc);
		assertEquals(bc, factoryBean.getObject().getInvalidationBackendClient());
	}

	@Test
	public void testSetRelaxOperationTimeoutInHeavyLoadSystem() throws Exception {
		factoryBean.setRelaxOperationTimeoutInHeavyLoadSystem(false);
		assertEquals(false, factoryBean.getObject().getRelaxOperationTimeoutInHeavyLoadSystem());
		factoryBean.setRelaxOperationTimeoutInHeavyLoadSystem(true);
		assertEquals(true, factoryBean.getObject().getRelaxOperationTimeoutInHeavyLoadSystem());
	}

}
