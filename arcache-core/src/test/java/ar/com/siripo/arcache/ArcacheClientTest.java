package ar.com.siripo.arcache;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
	public void testMiss() throws Exception {
		String key = "testMiss";

		Future<CacheGetResult> getFuture = arcache.asyncGetCacheObject(key);

		CacheGetResult cacheGetResult = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertEquals(cacheGetResult.type, CacheGetResult.Type.MISS);

		assertEquals(cacheGetResult.value, null);
	}

	@Test
	public void testFullExpired() throws Exception {
		fail("Implementar caso donde supera maxTTL y vuelve como expirado");
	}

	@Test
	public void testProbabilisticExpire() throws Exception {
		fail("Implementar caso donde no se da y si se da la expiracion probabilistica");
	}

}
