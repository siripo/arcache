package ar.com.siripo.arcache.util;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

public class DummyFutureTest {

	@Test
	public void testDummyFuture() throws Exception {
		DummyFuture<Boolean> df = new DummyFuture<Boolean>(true);
		assertEquals(df.get(), true);
	}

	@Test
	public void testGetExceptions() throws Exception {
		DummyFuture<Boolean> df;
		df = DummyFuture.createWithException(new InterruptedException());
		try {
			df.get();
			fail();
		} catch (InterruptedException e) {
		}

		df = DummyFuture.createWithException(new ExecutionException(new Exception()));
		try {
			df.get();
			fail();
		} catch (ExecutionException e) {
		}

		df = DummyFuture.createWithException(new IOException());
		try {
			df.get();
			fail();
		} catch (ExecutionException e) {
			assertThat(e.getCause(), instanceOf(IOException.class));
		}

		df = DummyFuture.createWithException(new InterruptedException());
		try {
			df.get(1, TimeUnit.SECONDS);
			fail();
		} catch (InterruptedException e) {
		}

		df = DummyFuture.createWithException(new ExecutionException(new Exception()));
		try {
			df.get(1, TimeUnit.SECONDS);
			fail();
		} catch (ExecutionException e) {
		}

		df = DummyFuture.createWithException(new TimeoutException());
		try {
			df.get(1, TimeUnit.SECONDS);
			fail();
		} catch (TimeoutException e) {
		}

		df = DummyFuture.createWithException(new IOException());
		try {
			df.get(1, TimeUnit.SECONDS);
			fail();
		} catch (ExecutionException e) {
			assertThat(e.getCause(), instanceOf(IOException.class));
		}
	}

	@Test
	public void testCancel() {
		DummyFuture<Boolean> df = new DummyFuture<Boolean>(true);
		assertFalse(df.cancel(false));
	}

	@Test
	public void testIsCancelled() {
		DummyFuture<Boolean> df = new DummyFuture<Boolean>(true);
		assertFalse(df.isCancelled());
	}

	@Test
	public void testIsDone() {
		DummyFuture<Boolean> df = new DummyFuture<Boolean>(true);
		assertTrue(df.isDone());
	}

	@Test
	public void testGet() throws Exception {
		DummyFuture<Boolean> df = new DummyFuture<Boolean>(true);
		assertEquals(df.get(), true);
	}

	@Test
	public void testGetLongTimeUnit() throws Exception {
		DummyFuture<Boolean> df = new DummyFuture<Boolean>(true);
		assertEquals(df.get(1, TimeUnit.DAYS), true);
	}

}
