package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.CacheGetterTask.InvalidatedKey;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.util.DummyFuture;

public class CacheGetterTaskTest {
	ArcacheInMemoryClient backendClient;
	ArcacheClient arcache;
	Random random;

	volatile boolean expectedFlow;
	volatile Object flowValue;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryClient();
		arcache = new ArcacheClient(backendClient);
		random = new Random();
	}

	@Test
	public void testCancel() throws Exception {
		// Normal case, cancel when not done
		CacheGetterTask cgt;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		assertEquals(cgt.isCancelled(), false);
		assertEquals(cgt.cancel(false), true);
		assertEquals(cgt.isCancelled(), true);
		assertEquals(cgt.cancel(false), false);
		try {
			cgt.get();
			fail();
		} catch (CancellationException ce) {

		}

		// When Done, its not cancellable
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		assertEquals(cgt.get().getClass(), CacheGetResult.class);
		assertEquals(cgt.isDone(), true);
		assertEquals(cgt.cancel(false), false);
		assertEquals(cgt.isCancelled(), false);

		// No exception thrown when mainFutureGet is null
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = null;
		assertEquals(cgt.isCancelled(), false);
		assertEquals(cgt.cancel(false), true);

	}

	@Test
	public void testCancel_MainFutureGet() throws Exception {

		// when mainFutureGet is running, test if may interrupt is propagated.
		Future<Object> fut = new DummyFuture<Object>(null) {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				flowValue = new Boolean(mayInterruptIfRunning);
				expectedFlow = true;
				return true;
			}
		};

		CacheGetterTask cgt;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = fut;
		expectedFlow = false;
		flowValue = null;
		assertEquals(cgt.cancel(false), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.FALSE);

		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = fut;
		expectedFlow = false;
		flowValue = null;
		assertEquals(cgt.cancel(true), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.TRUE);
	}

	@Test
	public void testCancel_InvalidationKeysFutureGets() throws Exception {

		// when mainFutureGet is running, test if may interrupt is propagated.
		Future<Object> mainfut = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				ExpirableCacheObject expo = new ExpirableCacheObject();
				expo.invalidationKeys = new String[] { "i1", "i2" };
				return expo;
			}
		};

		ArcacheBackendClient bkclient = new ArcacheBackendClient() {

			@Override
			public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
				return null;
			}

			@Override
			public Future<Object> asyncGet(String key) {
				return new DummyFuture<Object>(null) {
					boolean cancelled = false;

					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						flowValue = new Boolean(mayInterruptIfRunning);
						expectedFlow = true;
						cancelled = true;
						return true;
					}

					@Override
					public boolean isCancelled() {
						return cancelled;
					}

					@Override
					public Boolean get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						throw new TimeoutException("thisTimeout");
					}
				};
			}

		};

		CacheGetterTask cgt;
		cgt = new CacheGetterTask("thekey", bkclient, arcache, arcache, random);
		cgt.mainFutureGet = mainfut;

		try {
			cgt.get();
			fail();
		} catch (ExecutionException e) {
			// The timeout thrown when getting ivalidationkeys is transformed as
			// ExecutionException
			assertTrue(e.getCause() instanceof TimeoutException);
			assertEquals(e.getCause().getMessage(), "thisTimeout");
		}

		assertNotNull(cgt.invalidationKeysFutureGets);
		assertEquals(cgt.invalidationKeysFutureGets.size(), 2);
		assertEquals(cgt.invalidationKeysFutureGets.get("i1").isCancelled(), Boolean.FALSE);
		assertEquals(cgt.invalidationKeysFutureGets.get("i2").isCancelled(), Boolean.FALSE);

		expectedFlow = false;
		flowValue = null;
		assertEquals(cgt.cancel(true), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.TRUE);

		// Do verification.
		assertEquals(cgt.invalidationKeysFutureGets.get("i1").isCancelled(), Boolean.TRUE);
		assertEquals(cgt.invalidationKeysFutureGets.get("i2").isCancelled(), Boolean.TRUE);
	}

	@Test
	public void testGet_valueToReturn() throws Exception {
		CacheGetterTask cgt;
		cgt = new CacheGetterTask("thekeyvtr", backendClient, arcache, arcache, random);
		assertEquals(cgt.valueToReturn, null);
		assertEquals(cgt.done, false);
		cgt.get();
		assertEquals(cgt.valueToReturn.getClass(), CacheGetResult.class);
		assertEquals(cgt.done, true);
		assertEquals(cgt.isDone(), true);
		flowValue = new CacheGetResult(CacheGetResult.Type.ERROR);
		cgt.valueToReturn = (CacheGetResult) flowValue;
		assertEquals(cgt.get(), flowValue);
	}

	@Test
	public void testMainFutureGet() throws Exception {
		CacheGetterTask cgt;
		long currentTimeMillis = System.currentTimeMillis();

		// test call to get future in normal flavor
		cgt = new CacheGetterTask("thekeymf1", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				return flowValue;
			}
		};
		expectedFlow = false;
		flowValue = new ExpirableCacheObject();
		assertEquals(cgt.doTask(currentTimeMillis, 1000).getClass(), CacheGetResult.class);
		assertEquals(expectedFlow, true);

		// test timeout without call to get future, timeout reached
		cgt = new CacheGetterTask("thekeymf2", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = false;
				return flowValue;
			}
		};
		expectedFlow = true;
		try {
			cgt.doTask(currentTimeMillis - 2, 1);
			fail();
		} catch (TimeoutException e) {

		}
		assertEquals(expectedFlow, true);

		// test timeout inside get future
		cgt = new CacheGetterTask("thekeymf2", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				flowValue = new TimeoutException();
				throw (TimeoutException) flowValue;
			}
		};
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.doTask(currentTimeMillis, 1000);
			fail();
		} catch (TimeoutException e) {
			assertEquals(flowValue, e);
		}
		assertEquals(expectedFlow, true);

		// test InterruptedException inside get future
		cgt = new CacheGetterTask("thekeymf3", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				flowValue = new InterruptedException();
				throw (InterruptedException) flowValue;
			}
		};
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.doTask(currentTimeMillis, 1000);
			fail();
		} catch (InterruptedException e) {
			assertEquals(flowValue, e);
		}
		assertEquals(expectedFlow, true);

		// test Other rare exception inside get future
		cgt = new CacheGetterTask("thekeymf4", backendClient, arcache, arcache, random);
		cgt.mainFutureGet = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				flowValue = new RuntimeException("rare exception");
				throw (RuntimeException) flowValue;
			}
		};
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.doTask(currentTimeMillis, 1000);
			fail();
		} catch (RuntimeException e) {
			assertEquals(flowValue, e);
		}
		assertEquals(expectedFlow, true);
	}

	@Test
	public void testIsCachedObjectExpired_flows() throws Exception {
		ExpirableCacheObject cachedObject = new ExpirableCacheObject();
		cachedObject.timestamp = 0;
		cachedObject.minTTLSecs = 5;
		cachedObject.maxTTLSecs = 10;

		CacheGetterTask cgt;
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(1));

		assertEquals(cgt.isCachedObjectExpired(cachedObject, 0), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 1), false);

		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 5000), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 5999), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000), true);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6001), true);

		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0.5));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000), true);

		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0.5));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000), true);

		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(1));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 10000), true);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 11000), true);

		cachedObject.timestamp = 0;
		cachedObject.minTTLSecs = 5;
		cachedObject.maxTTLSecs = 3;
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0.5));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 2000), false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000), true);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000), true);
	}

	@Test
	public void testLoadInvalidationKeys() throws Exception {
		CacheGetterTask cgt;
		HashMap<String, CacheInvalidationObject> invMap;
		long currentTimeMillis = System.currentTimeMillis();

		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);

		ExpirableCacheObject cachedObject = new ExpirableCacheObject();

		// On null invalidationKeys expect null invMap
		cachedObject.invalidationKeys = null;
		invMap = cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 10);
		assertNull(invMap);

		// On Empty invalidationKeys expect null invMap
		cachedObject.invalidationKeys = new String[] {};
		invMap = cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 10);
		assertNull(invMap);

		// Create any loader for every InvKey
		cachedObject.invalidationKeys = new String[] { "i1", "i2" };
		cgt.invalidationKeysFutureGets = null;
		invMap = cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 1000);
		assertNotNull(cgt.invalidationKeysFutureGets);
		assertNotNull(cgt.invalidationKeysFutureGets.get("i1"));
		assertNotNull(cgt.invalidationKeysFutureGets.get("i2"));
		assertEquals(cgt.invalidationKeysFutureGets.size(), 2);

		// If the load is broken for example by a timeout, the next execution do not
		// create new loaders.
		Object fut1 = cgt.invalidationKeysFutureGets.get("i1");
		Object fut2 = cgt.invalidationKeysFutureGets.get("i2");
		invMap = cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 1000);
		assertEquals(cgt.invalidationKeysFutureGets.size(), 2);
		assertEquals(cgt.invalidationKeysFutureGets.get("i1"), fut1);
		assertEquals(cgt.invalidationKeysFutureGets.get("i2"), fut2);

		// If the CacheGetterTask is cancelled then loadInvalidationKeys must break with
		// CancellationException
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		cachedObject.invalidationKeys = new String[] { "i1", "i2" };
		cgt.invalidationKeysFutureGets = null;
		cgt.cancel(false);
		try {
			cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 1000);
			fail();
		} catch (CancellationException ce) {
		}

		// When has no more time, throws TimeoutException
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		cachedObject.invalidationKeys = new String[] { "i1", "i2" };
		cgt.invalidationKeysFutureGets = null;
		try {
			cgt.loadInvalidationKeys(cachedObject, currentTimeMillis - 2000, 1000);
			fail();
		} catch (TimeoutException te) {
		}

		// An inner exception loading Invalidation Keys propagate up.
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);
		cachedObject.invalidationKeys = new String[] { "i1", "i2" };
		cgt.invalidationKeysFutureGets = new HashMap<String, Future<Object>>();
		cgt.invalidationKeysFutureGets.put("i1", new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				flowValue = new ExecutionException(new Exception());
				throw (ExecutionException) flowValue;
			}
		});
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 1000);
			fail();
		} catch (ExecutionException ee) {
			assertEquals(ee, flowValue);
			assertTrue(expectedFlow);
		}

		// Assert that the inner exception is propagated to CacheGetterTask get
		arcache.set("magickey", "hola", new String[] { "theinvkey" });
		cgt = new CacheGetterTask("magickey", backendClient, arcache, arcache, random) {
			protected HashMap<String, CacheInvalidationObject> loadInvalidationKeys(
					final ExpirableCacheObject cachedObject, final long startTimeMillis, final long timeoutMillis)
					throws TimeoutException, InterruptedException, ExecutionException {
				expectedFlow = true;
				flowValue = new ExecutionException(new Exception());
				throw (ExecutionException) flowValue;
			}
		};
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.get();
			fail();
		} catch (ExecutionException ee) {
			assertEquals(ee, flowValue);
			assertTrue(expectedFlow);
		}

		// Test if getsCacheInvalidationObjectFromFuture is called from
		// loadInvalidationKeys to test it alone
		cgt = new CacheGetterTask("kkk", backendClient, arcache, arcache, random) {
			protected CacheInvalidationObject getsCacheInvalidationObjectFromFuture(Future<Object> future, long timeout)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				flowValue = new ExecutionException(new Exception());
				throw (ExecutionException) flowValue;
			}
		};
		cachedObject.invalidationKeys = new String[] { "i1", "i2" };
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.loadInvalidationKeys(cachedObject, currentTimeMillis, 1000);
			fail();
		} catch (ExecutionException ee) {
			assertEquals(ee, flowValue);
			assertTrue(expectedFlow);
		}
	}

	@Test
	public void testGetsCacheInvalidationObjectFromFuture() throws Exception {
		CacheGetterTask cgt;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);

		// test basic behavior, a miss
		Future<Object> fut = new DummyFuture<Object>(null);
		assertEquals(cgt.getsCacheInvalidationObjectFromFuture(fut, 1000), null);

		// test the case of a stored valid type value
		flowValue = new CacheInvalidationObject();
		fut = new DummyFuture<Object>(flowValue);
		assertEquals(cgt.getsCacheInvalidationObjectFromFuture(fut, 1000), flowValue);

		/*
		 * When is stored a invalidation key with a value not readable we have a big
		 * problem. Assume that you have two versions running at the same time. And one
		 * version invalidates a key. But the other version is unable to read that
		 * invalidated value. In this scenario its no way to know if the key is
		 * invalidated or not. If we assume it invalidated then the cache will be unable
		 * to run for this key until a readable invalidation key is stored. If we assume
		 * it as a miss, a possible inconsistency will be achieved.
		 * 
		 * By now we naively assume it as a miss and do not any correction.
		 */
		flowValue = new String("aaa");
		fut = new DummyFuture<Object>(flowValue);
		assertEquals(cgt.getsCacheInvalidationObjectFromFuture(fut, 1000), null);

		// Test if a inner exception is propagated up
		fut = new DummyFuture<Object>(null) {
			@Override
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				expectedFlow = true;
				flowValue = new ExecutionException(new Exception());
				throw (ExecutionException) flowValue;
			}
		};
		expectedFlow = false;
		flowValue = null;
		try {
			cgt.getsCacheInvalidationObjectFromFuture(fut, 1000);
		} catch (Exception e) {
			assertEquals(flowValue, e);
			assertTrue(expectedFlow);
		}

	}

	@Test
	public void testIsCachedObjectInvalidated() {
		CacheGetterTask cgt;
		arcache.setTimeMeasurementError(5);

		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, random);

		long currentTimeMillis = System.currentTimeMillis();
		ExpirableCacheObject cachedObject = new ExpirableCacheObject();
		HashMap<String, CacheInvalidationObject> invalidationMap = new HashMap<String, CacheInvalidationObject>();
		CacheInvalidationObject cio = new CacheInvalidationObject();
		InvalidatedKey invKey;
		invalidationMap.put("k1", cio);

		// If the invalidationMap is null then no invalidation
		cachedObject.timestamp = currentTimeMillis;
		assertNull(cgt.isCachedObjectInvalidated(cachedObject, null, currentTimeMillis));

		// if the stored object is older than 15 seconds and invalidated 10 seconds and
		// invalidation window 2 seconds then invalidated
		cachedObject.timestamp = (currentTimeMillis / 1000) - 15;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 10;
		cio.invalidationWindowSecs = 2;
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertEquals(invKey.cacheInvalidationObject, cio);

		/*
		 * if the stored object is older than 15 seconds and invalidated 10 seconds and
		 * invalidation window 2 seconds then must be invalidated, but if time
		 * measurement error is bigger than 15 seconds, then its considered valid
		 */
		cachedObject.timestamp = (currentTimeMillis / 1000) - 15;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 10;
		cio.invalidationWindowSecs = 2;
		arcache.setTimeMeasurementError(14);
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertEquals(invKey.cacheInvalidationObject, cio);

		arcache.setTimeMeasurementError(16);
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertNull(invKey);

		arcache.setTimeMeasurementError(5);

		/*
		 * in this case, the key was stored 5 sec ago, but taking in account of the time
		 * measurament error, the poor condition is if it was really set 5-5 seconds
		 * ago. And considering that the invalidation was 10 seconds ago, the key mas be
		 * considered invalidated
		 */
		cachedObject.timestamp = (currentTimeMillis / 1000) - 5;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 10;
		cio.invalidationWindowSecs = 0;
		arcache.setTimeMeasurementError(5);
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertEquals(invKey.cacheInvalidationObject, cio);

		/*
		 * Here the key was invalidated 11 secs ago, but this key was set 5 secs ago
		 * minus the error are 10 secs ago. In this case the key is valid
		 */
		cachedObject.timestamp = (currentTimeMillis / 1000) - 5;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 11;
		cio.invalidationWindowSecs = 0;
		arcache.setTimeMeasurementError(5);
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertNull(invKey);

		/*
		 * Here the key is into the middle of invalidation window, but with a random of
		 * 0. then it is invalid
		 */
		arcache.setTimeMeasurementError(0);
		cachedObject.timestamp = (currentTimeMillis / 1000) - 5;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 0;
		cio.invalidationWindowSecs = 10;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, new StaticDoubleRandom(0));
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertNotNull(invKey);

		/*
		 * Here the key is into the middle of invalidation window, but with a random of
		 * 1. then it is valid
		 */
		arcache.setTimeMeasurementError(0);
		cachedObject.timestamp = (currentTimeMillis / 1000) - 5;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 0;
		cio.invalidationWindowSecs = 10;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, new StaticDoubleRandom(1));
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertNull(invKey);

		/*
		 * Here the key is into the middle of invalidation window, but with a random of
		 * 1. then it is valid. But with a last invalidation hard newer this must be
		 * invalid hard
		 */
		arcache.setTimeMeasurementError(0);
		cachedObject.timestamp = (currentTimeMillis / 1000) - 5;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 0;
		cio.invalidationWindowSecs = 10;
		cio.lastHardInvalidationTimestamp = (currentTimeMillis / 1000) - 0;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, new StaticDoubleRandom(1));
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertEquals(invKey.hardInvalidation, true);

		/*
		 * Here the key is into the middle of invalidation window, but with a random of
		 * 1. then it is valid. But with a last invalidation soft newer this must be
		 * invalid soft
		 */
		arcache.setTimeMeasurementError(0);
		cachedObject.timestamp = (currentTimeMillis / 1000) - 5;
		cio.invalidationTimestamp = (currentTimeMillis / 1000) - 0;
		cio.invalidationWindowSecs = 10;
		cio.lastHardInvalidationTimestamp = 0;
		cio.lastSoftInvalidationTimestamp = (currentTimeMillis / 1000) - 0;
		cgt = new CacheGetterTask("thekey", backendClient, arcache, arcache, new StaticDoubleRandom(1));
		invKey = cgt.isCachedObjectInvalidated(cachedObject, invalidationMap, currentTimeMillis);
		assertEquals(invKey.hardInvalidation, false);

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
