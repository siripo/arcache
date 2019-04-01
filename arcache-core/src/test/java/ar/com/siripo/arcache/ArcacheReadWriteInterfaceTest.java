package ar.com.siripo.arcache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.util.DummyFuture;

public class ArcacheReadWriteInterfaceTest {

	ArcacheInMemoryClient backendClient;
	BuildCacheGetterTaskInterceptor rinterceptor;
	ArcacheReadWriteInterface rInterface;
	ArcacheConfigurationInterface rconfig;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryClient();
		rinterceptor = new BuildCacheGetterTaskInterceptor(backendClient);
		rInterface = rinterceptor;
		rconfig = rinterceptor;
	}

	@Test
	public void testGetString() throws Exception {
		Object readedv;

		rInterface.set("juan", "algo");
		rinterceptor.resetInterceptor();
		readedv = rInterface.get("juan");
		rinterceptor.assertIntercepted("juan");
		assertEquals(readedv, "algo");

		// Test timeout propagation
		ArcacheClient clix = new ArcacheClient(backendClient) {
			@Override
			public Object get(final String key, final long timeoutMillis) throws TimeoutException, Exception {
				return "" + timeoutMillis;
			}
		};

		clix.setDefaultOperationTimeout(456);
		assertEquals(clix.get("key"), "456");
	}

	@Test
	public void testGetStringLong() throws Exception {
		Object readedv;

		rInterface.set("juan", "algo");
		rinterceptor.resetInterceptor();
		readedv = rInterface.get("juan", 111);
		rinterceptor.assertIntercepted("juan");
		assertEquals(readedv, "algo");

		readedv = rInterface.get("Not found key", 111);
		assertEquals(readedv, null);

		try {
			rInterface.get("TimeoutException", 111);
			fail();
		} catch (TimeoutException e) {
		}

		try {
			rInterface.get("InterruptedException", 111);
			fail();
		} catch (InterruptedException e) {
		}

		try {
			rInterface.get(null, 111);
			fail();
		} catch (IllegalArgumentException e) {
		}

		// Walk all types testing if all are interpreted
		for (final CacheGetResult.Type typetest : CacheGetResult.Type.values()) {
			ArcacheClient cli = new ArcacheClient(backendClient) {
				@Override
				public CacheGetResult getCacheObject(final String key, final long timeoutMillis) {
					return new CacheGetResult(typetest, new InterruptedException());
				}
			};

			try {
				cli.get("key", 22);
			} catch (InterruptedException e) {
			}
		}

		// Test expired
		ArcacheClient clix = new ArcacheClient(backendClient) {
			@Override
			public CacheGetResult getCacheObject(final String key, final long timeoutMillis) {
				CacheGetResult cr = new CacheGetResult(CacheGetResult.Type.EXPIRED);
				cr.value = "xx";
				return cr;
			}
		};

		assertNull(clix.get("key", 22));

		// Test Invalidated
		ArcacheClient clii = new ArcacheClient(backendClient) {
			@Override
			public CacheGetResult getCacheObject(final String key, final long timeoutMillis) {
				CacheGetResult cr = new CacheGetResult(CacheGetResult.Type.INVALIDATED);
				cr.value = "xx";
				return cr;
			}
		};
		assertNull(clii.get("key", 22));

		// Test the case of null or a unhandled type results in IllegalStateException
		ArcacheClient cli = new ArcacheClient(backendClient) {
			@Override
			public CacheGetResult getCacheObject(final String key, final long timeoutMillis) {
				return null;
			}
		};

		try {
			cli.get("key", 22);
			fail();
		} catch (IllegalStateException e) {
		}

	}

	@Test
	public void testGetCacheObjectString() throws Exception {
		CacheGetResult cgr;

		rInterface.set("juan", "algo");
		rinterceptor.resetInterceptor();
		cgr = rInterface.getCacheObject("juan");
		rinterceptor.assertIntercepted("juan");
		assertEquals(cgr.value, "algo");
		assertTrue(cgr.isHit());

		// Test timeout propagation

		ArcacheClient clix = new ArcacheClient(backendClient) {
			@Override
			public CacheGetResult getCacheObject(final String key, final long timeoutMillis) {
				CacheGetResult cr = new CacheGetResult(CacheGetResult.Type.HIT);
				cr.value = "" + timeoutMillis;
				return cr;
			}
		};

		clix.setDefaultOperationTimeout(334);
		assertEquals(clix.getCacheObject("key").value, "334");
	}

	@Test
	public void testGetCacheObjectStringLong() throws Exception {

		CacheGetResult cgr;

		rInterface.set("juan", "algo");
		rinterceptor.resetInterceptor();
		cgr = rInterface.getCacheObject("juan", 22);
		rinterceptor.assertIntercepted("juan");
		assertEquals(cgr.value, "algo");
		assertTrue(cgr.isHit());

		cgr = rInterface.getCacheObject("NULL", 22);
		rinterceptor.assertIntercepted("NULL");
		assertEquals(cgr.value, null);
		assertEquals(cgr.type, CacheGetResult.Type.ERROR);
		assertTrue(cgr.errorCause instanceof NullPointerException);

		cgr = rInterface.getCacheObject("TimeoutException", 22);
		assertEquals(cgr.type, CacheGetResult.Type.TIMEOUT);
		assertTrue(cgr.errorCause instanceof TimeoutException);

		cgr = rInterface.getCacheObject("", 22);
		assertEquals(cgr.type, CacheGetResult.Type.ERROR);
		assertTrue(cgr.errorCause instanceof IllegalArgumentException);

		cgr = rInterface.getCacheObject("ExecutionException", 22);
		assertEquals(cgr.type, CacheGetResult.Type.ERROR);
		assertTrue(cgr.errorCause instanceof ExecutionException);

		cgr = rInterface.getCacheObject("InterruptedException", 22);
		assertEquals(cgr.type, CacheGetResult.Type.ERROR);
		assertTrue(cgr.errorCause instanceof InterruptedException);

		cgr = rInterface.getCacheObject("IllegalStateException", 22);
		assertEquals(cgr.type, CacheGetResult.Type.ERROR);
		assertTrue(cgr.errorCause instanceof IllegalStateException);

	}

	@Test
	public void testAsyncGetCacheObject() throws Exception {
		rinterceptor.resetInterceptor();
		rInterface.asyncGetCacheObject("juan");
		rinterceptor.assertIntercepted("juan");

		Future<CacheGetResult> future = rInterface.asyncGetCacheObject("");
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}

		future = rInterface.asyncGetCacheObject(null);
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}

		future = rInterface.asyncGetCacheObject("IllegalStateException");
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalStateException);

		}

		future = rInterface.asyncGetCacheObject("TimeoutException");
		try {
			future.get(1, TimeUnit.HOURS);
			fail();
		} catch (TimeoutException e) {
			assertTrue(e instanceof TimeoutException);

		}

		future = rInterface.asyncGetCacheObject("InterruptedException");
		try {
			future.get();
			fail();
		} catch (InterruptedException e) {
			assertTrue(e instanceof InterruptedException);

		}

		future = rInterface.asyncGetCacheObject("ExecutionException");
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e instanceof ExecutionException);

		}
	}

	@Test
	public void testSetStringObject() throws Exception {
		final ArcacheClient cli = new ArcacheClient(backendClient) {
			public void set(final String key, final Object value, final String[] invalidationKeys)
					throws TimeoutException, Exception {
				assertEquals(key, "key");
				assertEquals(value, "val");
				assertNull(invalidationKeys);
			}
		};
		cli.set("key", "val");
	}

	@Test
	public void testSetStringObjectStringArray() throws Exception {
		// Functional
		rInterface.set("key", "val", null);
		assertEquals(rInterface.get("key"), "val");

		try {
			rInterface.set(null, "val", null);
		} catch (IllegalArgumentException ex) {

		}

		// White Box
		final ArcacheClient cli = new ArcacheClient(backendClient) {
			public Future<Boolean> asyncSet(final String key, final Object value, final String[] invalidationKeys) {
				return new DummyFuture<Boolean>(null) {
					@Override
					public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
						throw new ExecutionException(new Error());
					}
				};
			}
		};

		try {
			cli.set(null, "val", null);
		} catch (ExecutionException xx) {
			assertTrue(xx.getCause() instanceof Error);
		}
	}

	@Test
	public void testAsyncSetStringObject() {
		final ArcacheClient cli = new ArcacheClient(backendClient) {
			public Future<Boolean> asyncSet(final String key, final Object value, final String[] invalidationKeys) {
				assertEquals(key, "key");
				assertEquals(value, "val");
				assertNull(invalidationKeys);
				return null;
			}
		};
		cli.asyncSet("key", "val");
	}

	@Test
	public void testAsyncSetStringObjectStringArray() throws Exception {
		// Functional
		rInterface.asyncSet("key", "val", null).get();
		assertEquals(rInterface.get("key"), "val");

		// Functional Exception
		try {
			rInterface.asyncSet(null, "val", null).get();
		} catch (ExecutionException ex) {
			assertTrue(ex.getCause() instanceof IllegalArgumentException);
		}

		try {
			rInterface.asyncSet("", "val", null).get();
		} catch (ExecutionException ex) {
			assertTrue(ex.getCause() instanceof IllegalArgumentException);
		}

		// White Box test
		final ArcacheClient cliorig = new ArcacheClient(backendClient);

		ArcacheBackendClient backendClientx = new ArcacheInMemoryClient() {
			@Override
			public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
				ExpirableCacheObject expObj = (ExpirableCacheObject) value;
				assertEquals(cliorig.createBackendKey("key"), key);
				assertEquals(Math.max(2, Math.abs(expObj.timestamp - System.currentTimeMillis() / 1000)), 2);
				assertEquals(expObj.value, "val");
				assertArrayEquals(expObj.invalidationKeys, new String[] { "a", "b" });
				assertEquals(ttlSeconds, 555);
				return null;
			}
		};
		ArcacheClient clix = new ArcacheClient(backendClientx);
		clix.setDefaultStoredObjectRemovalTime(555);

		clix.asyncSet("key", "val", new String[] { "a", "b" });

		// Test handle Exception
		ArcacheBackendClient backendClientEx = new ArcacheInMemoryClient() {
			@Override
			public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
				throw new IllegalStateException();
			}
		};
		ArcacheClient cliEx = new ArcacheClient(backendClientEx);

		Future<Boolean> fut = cliEx.asyncSet("key", "val", new String[] {});
		try {
			fut.get();
			fail();
		} catch (ExecutionException ex) {
			assertTrue(ex.getCause() instanceof IllegalStateException);
		}
	}

	static class BuildCacheGetterTaskInterceptor extends ArcacheClient {

		String key;
		boolean called;

		public BuildCacheGetterTaskInterceptor(ArcacheBackendClient backendClient) {
			super(backendClient);
			resetInterceptor();
		}

		@Override
		protected Future<CacheGetResult> buildCacheGetterTask(final String key) {
			this.key = key;
			this.called = true;

			if (key == "IllegalStateException") {
				throw new IllegalStateException();
			}
			if (key == "TimeoutException") {
				return DummyFuture.createWithException(new TimeoutException());
			}
			if (key == "InterruptedException") {
				return DummyFuture.createWithException(new InterruptedException());
			}
			if (key == "ExecutionException") {
				return DummyFuture.createWithException(new ExecutionException(new Error()));
			}

			if (key == "NULL") {
				return new DummyFuture<CacheGetResult>(null);
			}

			return super.buildCacheGetterTask(key);
		}

		public void resetInterceptor() {
			this.key = null;
			this.called = false;
		}

		public void assertIntercepted(String key) {
			assertTrue(this.called);
			assertEquals(this.key, key);
		}
	}

}
