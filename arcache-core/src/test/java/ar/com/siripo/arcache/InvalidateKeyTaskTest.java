package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.CancellationException;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;

public class InvalidateKeyTaskTest {

	ArcacheInMemoryClient backendClient;
	ArcacheClient arcache;

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
	}

	// TODO test case when prevVersionGetFuture is running, test if may interrupt is
	// propagated.
	// TODO test case when set is running and cancel is executed.

}
