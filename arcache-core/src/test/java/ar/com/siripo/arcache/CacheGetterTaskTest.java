package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

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
		ExpirableCacheObject cachedObject=new ExpirableCacheObject();
		cachedObject.timestamp=0;
		cachedObject.minTTLSecs=5;
		cachedObject.maxTTLSecs=10;
		
		CacheGetterTask cgt;
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(1));
		
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 0),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 1),false);
		
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 5000),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 5999),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000),true);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6001),true);
		
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0.5));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000),true);
		
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0.5));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000),true);
		
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(1));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 10000),true);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 11000),true);
		
		cachedObject.timestamp=0;
		cachedObject.minTTLSecs=5;
		cachedObject.maxTTLSecs=3;
		cgt = new CacheGetterTask("xxxx", backendClient, arcache, arcache, new StaticDoubleRandom(0.5));
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 2000),false);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 6000),true);
		assertEquals(cgt.isCachedObjectExpired(cachedObject, 9000),true);
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
