package ar.com.siripo.arcache.backend.speedup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.util.DummyFuture;

public class FutureBackendGetWrapperTest {

	ArcacheSpeedupClient client;
	ArcacheInMemoryClient backendClient;
	FutureBackendGetWrapper futureBackendGetWrapper;
	String key;
	Future<Object> futureGet;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryClient();

		client = new ArcacheSpeedupClient();
		client.setBackendClient(backendClient);
		client.setObjectsCacheSize(1000);
		client.setObjectsExpirationMillis(1000);
		client.setMissesCacheSize(1000);
		client.setMissesExpirationMillis(1000);
		client.initialize();

		key = "thekey";

		futureGet = client.backendClient.asyncGet(key);

		futureBackendGetWrapper = new FutureBackendGetWrapper(client, futureGet, key, true);

	}

	@Test
	public void testCancel() {
		final IllegalArgumentException ex = new IllegalArgumentException();
		futureGet = new DummyFuture<Object>(null) {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				if (mayInterruptIfRunning)
					throw ex;
				return false;
			}
		};
		futureBackendGetWrapper.backendFuture = futureGet;
		try {
			futureBackendGetWrapper.cancel(true);
			fail();
		} catch (Exception e) {
			assertEquals(ex, e);
		}

		assertFalse(futureBackendGetWrapper.cancel(false));

	}

	@Test
	public void testIsCancelled() {
		futureGet = new DummyFuture<Object>(null) {
			@Override
			public boolean isCancelled() {
				return true;
			}
		};
		assertFalse(futureBackendGetWrapper.isCancelled());
		futureBackendGetWrapper.backendFuture = futureGet;
		assertTrue(futureBackendGetWrapper.isCancelled());
	}

	@Test
	public void testIsDone() {
		futureGet = new DummyFuture<Object>(null) {
			@Override
			public boolean isDone() {
				return true;
			}
		};
		futureBackendGetWrapper.backendFuture = futureGet;
		assertTrue(futureBackendGetWrapper.isDone());

		futureGet = new DummyFuture<Object>(null) {
			@Override
			public boolean isDone() {
				return false;
			}
		};
		futureBackendGetWrapper.backendFuture = futureGet;
		assertFalse(futureBackendGetWrapper.isDone());
	}

	@Test
	public void testGet() {
		// Test wrapp result OK
		String thevaluetoreturn = "HOLAQUETAL";
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(thevaluetoreturn);

	}

	@Test
	public void testGetLongTimeUnit() {
		// TODO fail("Not yet implemented");
	}

	@Test
	public void testWrappGetResult() {
		// TODO fail("Not yet implemented");
	}

	@Test
	public void testWrappGetInterruptedException() {
		// TODO fail("Not yet implemented");
	}

	@Test
	public void testWrappGetExecutionException() {
		// TODO fail("Not yet implemented");
	}

	@Test
	public void testWrappGetTimeoutException() {
		// TODO fail("Not yet implemented");
	}

	@Test
	public void testDoProtection() {
		// TODO fail("Not yet implemented");
	}

}
