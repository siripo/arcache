package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

public class InvalidateKeyTaskTest {

	ArcacheInMemoryClient backendClient;
	ArcacheClient arcache;

	boolean expectedFlow;
	Object flowValue;

	@Before
	public void setUp() throws Exception {
		backendClient = new ArcacheInMemoryClient();
		arcache = new ArcacheClient(backendClient);
	}

	@Test
	public void testInvalidateKeyTask() throws Exception {
		InvalidateKeyTask ikt;
		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		assertEquals(ikt.get(), true);
	}

	@Test
	public void testCancel() throws Exception {
		// Normal case, cancel when not done
		InvalidateKeyTask ikt;
		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		assertEquals(ikt.isCancelled(), false);
		assertEquals(ikt.cancel(false), true);
		assertEquals(ikt.isCancelled(), true);
		assertEquals(ikt.cancel(false), false);
		try {
			ikt.get();
			fail();
		} catch (CancellationException ce) {

		}

		// When Done, its not cancellable
		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		assertEquals(ikt.get(), true);
		assertEquals(ikt.isDone(), true);
		assertEquals(ikt.cancel(false), false);
		assertEquals(ikt.isCancelled(), false);

		// No exception thrown when prevVersionGetFuture is null
		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		ikt.prevVersionGetFuture = null;
		assertEquals(ikt.isCancelled(), false);
		assertEquals(ikt.cancel(false), true);

	}

	@Test
	public void testCancel_PrevVersionGetFuture() throws Exception {

		// when prevVersionGetFuture is running, test if may interrupt is propagated.
		Future<Object> fut = new DummyFuture<Object>(null) {
			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				flowValue = new Boolean(mayInterruptIfRunning);
				expectedFlow = true;
				return true;
			}
		};

		InvalidateKeyTask ikt;
		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		ikt.prevVersionGetFuture = fut;
		expectedFlow = false;
		flowValue = null;
		assertEquals(ikt.cancel(false), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.FALSE);

		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		ikt.prevVersionGetFuture = fut;
		expectedFlow = false;
		flowValue = null;
		assertEquals(ikt.cancel(true), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.TRUE);
	}

	@Test
	public void testCancel_SetFuture() throws Exception {

		// when set is running and cancel is executed.

		ArcacheBackendClient bkclient = new ArcacheBackendClient() {

			@Override
			public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
				// TODO Auto-generated method stub
				return new DummyFuture<Boolean>(null) {
					@Override
					public boolean cancel(boolean mayInterruptIfRunning) {
						flowValue = new Boolean(mayInterruptIfRunning);
						expectedFlow = true;
						return true;
					}

					@Override
					public Boolean get(long timeout, TimeUnit unit)
							throws InterruptedException, ExecutionException, TimeoutException {
						throw new TimeoutException();
					}
				};
			}

			@Override
			public Future<Object> asyncGet(String key) {
				return new DummyFuture<Object>(null);
			}

		};

		InvalidateKeyTask ikt;
		ikt = new InvalidateKeyTask("firstkey", false, 10, bkclient, arcache, arcache);
		expectedFlow = false;
		flowValue = null;
		try {
			ikt.get();
			fail();
		} catch (ExecutionException e) {
		}
		assertEquals(ikt.cancel(true), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.TRUE);

		ikt = new InvalidateKeyTask("firstkey", false, 10, bkclient, arcache, arcache);
		expectedFlow = false;
		flowValue = null;
		try {
			ikt.get();
			fail();
		} catch (ExecutionException e) {
		}
		assertEquals(ikt.cancel(false), true);
		assertEquals(expectedFlow, true);
		assertEquals(flowValue, Boolean.FALSE);
	}

	@Test
	public void testGet_valueToReturn() throws Exception {
		InvalidateKeyTask ikt;
		ikt = new InvalidateKeyTask("firstkey", false, 10, backendClient, arcache, arcache);
		ikt.valueToReturn=false;
		assertEquals(ikt.get(), true);
		assertEquals(ikt.get(), true);
		ikt.valueToReturn=false;
		assertEquals(ikt.get(), false);
	}

}
