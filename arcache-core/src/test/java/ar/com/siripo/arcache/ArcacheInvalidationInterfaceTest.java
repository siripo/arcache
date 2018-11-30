package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.test.ArcacheInMemoryTestBackend;
import ar.com.siripo.arcache.util.DummyFuture;

public class ArcacheInvalidationInterfaceTest {

	ArcacheInMemoryTestBackend backendClient;
	BuildInvalidateKeyTaskInterceptor interceptor;
	ArcacheInvalidationInterface invalidationInterface;
	ArcacheConfigurationInterface config;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryTestBackend();
		interceptor = new BuildInvalidateKeyTaskInterceptor(backendClient);
		invalidationInterface = interceptor;
		config = interceptor;
	}

	@Test
	public void testInvalidateKeyString() throws Exception {
		config.setDefaultHardInvalidation(true);
		config.setDefaultInvalidationWindow(777);
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("juan");
		interceptor.assertIntercepted("juan", true, 777);

		config.setDefaultHardInvalidation(false);
		config.setDefaultInvalidationWindow(888);
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("pili");
		interceptor.assertIntercepted("pili", false, 888);
	}

	@Test
	public void testInvalidateKeyStringLong() throws Exception {

		config.setDefaultHardInvalidation(true);
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("juan", 2345);
		interceptor.assertIntercepted("juan", true, 2345);

		config.setDefaultHardInvalidation(false);
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("juan2", 44);
		interceptor.assertIntercepted("juan2", false, 44);
	}

	@Test
	public void testInvalidateKeyStringBoolean() throws Exception {
		config.setDefaultInvalidationWindow(999);
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("juan", true);
		interceptor.assertIntercepted("juan", true, 999);

		config.setDefaultInvalidationWindow(111);
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("pili", false);
		interceptor.assertIntercepted("pili", false, 111);
	}

	@Test
	public void testInvalidateKeyStringBooleanLong() throws Exception {
		interceptor.resetInterceptor();
		invalidationInterface.invalidateKey("juan", true, 33);
		interceptor.assertIntercepted("juan", true, 33);

		// test Timeout Propagation
		interceptor.resetInterceptor();
		try {
			invalidationInterface.invalidateKey("TimeoutException", true, 33);
			fail();
		} catch (TimeoutException e) {
			assertTrue(e instanceof TimeoutException);
		}

		// IllegalArgumentException
		interceptor.resetInterceptor();
		try {
			invalidationInterface.invalidateKey("xxx", true, -1);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e instanceof IllegalArgumentException);
		}

		// InterruptedException
		interceptor.resetInterceptor();
		try {
			invalidationInterface.invalidateKey("InterruptedException", true, 22);
			fail();
		} catch (InterruptedException e) {
			assertTrue(e instanceof InterruptedException);
		}

		// ExecutionException
		interceptor.resetInterceptor();
		try {
			invalidationInterface.invalidateKey("ExecutionException", true, 22);
			fail();
		} catch (ExecutionException e) {
			assertTrue(e instanceof ExecutionException);
		}

	}

	@Test
	public void testAsyncInvalidateKey() throws Exception {
		interceptor.resetInterceptor();
		invalidationInterface.asyncInvalidateKey("juan", true, 33);
		interceptor.assertIntercepted("juan", true, 33);

		Future<Boolean> future = invalidationInterface.asyncInvalidateKey("", true, 33);
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);

		}

		future = invalidationInterface.asyncInvalidateKey(null, true, 33);
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);

		}

		future = invalidationInterface.asyncInvalidateKey("juan", true, -1);
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalArgumentException);

		}

		interceptor.resetInterceptor();
		invalidationInterface.asyncInvalidateKey("juan", true, 0);
		interceptor.assertIntercepted("juan", true, 0);

		future = invalidationInterface.asyncInvalidateKey("IllegalStateException", true, 22);
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e.getCause() instanceof IllegalStateException);

		}

		future = invalidationInterface.asyncInvalidateKey("TimeoutException", true, 22);
		try {
			future.get(1, TimeUnit.HOURS);
			fail();
		} catch (TimeoutException e) {
			assertTrue(e instanceof TimeoutException);

		}

		future = invalidationInterface.asyncInvalidateKey("InterruptedException", true, 22);
		try {
			future.get();
			fail();
		} catch (InterruptedException e) {
			assertTrue(e instanceof InterruptedException);

		}

		future = invalidationInterface.asyncInvalidateKey("ExecutionException", true, 22);
		try {
			future.get();
			fail();
		} catch (ExecutionException e) {
			assertTrue(e instanceof ExecutionException);

		}

	}

	static class BuildInvalidateKeyTaskInterceptor extends ArcacheClient {

		String key;
		boolean hardInvalidation;
		long invalidationWindowSecs;
		boolean called;

		public BuildInvalidateKeyTaskInterceptor(ArcacheBackendClient backendClient) {
			super(backendClient);
			resetInterceptor();
		}

		@Override
		protected Future<Boolean> buildInvalidateKeyTask(final String key, final boolean hardInvalidation,
				final long invalidationWindowSecs) {
			this.key = key;
			this.hardInvalidation = hardInvalidation;
			this.invalidationWindowSecs = invalidationWindowSecs;
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

			return super.buildInvalidateKeyTask(key, hardInvalidation, invalidationWindowSecs);
		}

		public void resetInterceptor() {
			this.key = null;
			this.hardInvalidation = false;
			this.invalidationWindowSecs = 0;
			this.called = false;
		}

		public void assertIntercepted(String key, boolean hardInvalidation, long invalidationWindowSecs) {
			assertTrue(this.called);
			assertEquals(this.key, key);
			assertEquals(this.hardInvalidation, hardInvalidation);
			assertEquals(this.invalidationWindowSecs, invalidationWindowSecs);
		}
	}
}
