package ar.com.siripo.arcache.backend.speedup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	boolean flag;

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

		futureBackendGetWrapper = new FutureBackendGetWrapper(client, futureGet, key, true, client.tracker);

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
	public void testGet() throws Exception {
		// Test wrapp result OK
		String thevaluetoreturn = "HOLAQUETAL";
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(thevaluetoreturn);
		client.clear();
		assertNull(futureBackendGetWrapper.speedupClient.objectsCache.get(key));
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get());
		assertNotNull(futureBackendGetWrapper.speedupClient.objectsCache.get(key));

		// Test exception when storing, expect no error
		futureBackendGetWrapper.speedupClient = null;
		client.clear();
		assertNull(client.objectsCache.get(key));
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get());
		assertNull(client.objectsCache.get(key));

		// --------------------------------------------------------------
		// Test Interrupt fetching from future
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(null) {
			public Object get() throws InterruptedException, ExecutionException {
				throw new InterruptedException();
			}
		};
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		// value recovered from cache
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get());
		futureBackendGetWrapper.protectAgainstBackendFailures = false;
		// Fail because protection off
		try {
			futureBackendGetWrapper.get();
			fail();
		} catch (InterruptedException e) {
		}

		// Recover from cache
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		client.clear();
		// Fail because no cache available
		try {
			futureBackendGetWrapper.get();
			fail();
		} catch (InterruptedException e) {
		}

		// --------------------------------------------------------------
		// Test ExecutionException fetching from future
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(null) {
			public Object get() throws InterruptedException, ExecutionException {
				throw new ExecutionException(new Exception());
			}
		};
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		// value recovered from cache
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get());
		futureBackendGetWrapper.protectAgainstBackendFailures = false;
		// Fail because protection off
		try {
			futureBackendGetWrapper.get();
			fail();
		} catch (ExecutionException e) {
		}

		// Recover from cache
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		client.clear();
		// Fail because no cache available
		try {
			futureBackendGetWrapper.get();
			fail();
		} catch (ExecutionException e) {
		}

		// Protection allowed but internal error
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get());
		futureBackendGetWrapper.speedupClient = null;
		try {
			futureBackendGetWrapper.get();
			fail();
		} catch (ExecutionException e) {
		}

	}

	@Test
	public void testGetLongTimeUnit() throws Exception {
		// Test wrapp result OK
		String thevaluetoreturn = "HOLAQUETAL";
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(thevaluetoreturn);
		client.clear();
		assertNull(futureBackendGetWrapper.speedupClient.objectsCache.get(key));
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get(100, TimeUnit.SECONDS));
		assertNotNull(futureBackendGetWrapper.speedupClient.objectsCache.get(key));

		// Test exception when storing, expect no error
		futureBackendGetWrapper.speedupClient = null;
		client.clear();
		assertNull(client.objectsCache.get(key));
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get(100, TimeUnit.SECONDS));
		assertNull(client.objectsCache.get(key));

		// --------------------------------------------------------------
		// Test Interrupt fetching from future
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(null) {
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				throw new InterruptedException();
			}
		};
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		// value recovered from cache
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get(100, TimeUnit.SECONDS));
		futureBackendGetWrapper.protectAgainstBackendFailures = false;
		// Fail because protection off
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (InterruptedException e) {
		}

		// Recover from cache
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		client.clear();
		// Fail because no cache available
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (InterruptedException e) {
		}

		// --------------------------------------------------------------
		// Test ExecutionException fetching from future
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(null) {
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				throw new ExecutionException(new Exception());
			}
		};
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		// value recovered from cache
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get(100, TimeUnit.SECONDS));
		futureBackendGetWrapper.protectAgainstBackendFailures = false;
		// Fail because protection off
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (ExecutionException e) {
		}

		// Recover from cache
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		client.clear();
		// Fail because no cache available
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (ExecutionException e) {
		}

		// Protection allowed but internal error
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get(100, TimeUnit.SECONDS));
		futureBackendGetWrapper.speedupClient = null;
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (ExecutionException e) {
		}

		// --------------------------------------------------------------
		// Test ExecutionException fetching from future
		futureBackendGetWrapper.speedupClient = client;
		futureBackendGetWrapper.backendFuture = new DummyFuture<Object>(null) {
			public Object get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				throw new TimeoutException();
			}
		};
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		// value recovered from cache
		assertEquals(thevaluetoreturn, futureBackendGetWrapper.get(100, TimeUnit.SECONDS));
		futureBackendGetWrapper.protectAgainstBackendFailures = false;
		// Fail because protection off
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (TimeoutException e) {
		}

		// Recover from cache
		futureBackendGetWrapper.protectAgainstBackendFailures = true;
		client.storeSpeedupCache(key, thevaluetoreturn);
		client.clear();
		// Fail because no cache available
		try {
			futureBackendGetWrapper.get(100, TimeUnit.SECONDS);
			fail();
		} catch (TimeoutException e) {
		}

	}

	@Test
	public void testWrappGetResult() {
		final String result = "ret";
		client.clear();
		assertEquals(result, futureBackendGetWrapper.wrappGetResult(result));

		flag = false;
		futureBackendGetWrapper.speedupClient = new ArcacheSpeedupClient() {
			protected ArcacheInMemoryClient storeSpeedupCache(String tkey, Object value) {
				if ((value == result) && (tkey == key)) {
					flag = true;
					throw new RuntimeException();
				}
				return null;
			};
		};
		flag = false;
		assertEquals(result, futureBackendGetWrapper.wrappGetResult(result));
		assertTrue(flag);
	}
}
