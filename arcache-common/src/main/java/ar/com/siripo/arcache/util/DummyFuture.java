package ar.com.siripo.arcache.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DummyFuture<T> implements Future<T> {

	private final T theresult;
	private Exception exceptionToThrow = null;

	public DummyFuture(T result) {
		theresult = result;
	}

	public static <T> DummyFuture<T> createWithException(Exception e) {
		DummyFuture<T> df = new DummyFuture<T>(null);
		df.exceptionToThrow = e;
		return df;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		if (exceptionToThrow != null) {
			if (exceptionToThrow instanceof InterruptedException) {
				throw (InterruptedException) exceptionToThrow;
			}
			if (exceptionToThrow instanceof ExecutionException) {
				throw (ExecutionException) exceptionToThrow;
			}
			throw new ExecutionException(exceptionToThrow);
		}
		return theresult;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (exceptionToThrow != null) {
			if (exceptionToThrow instanceof InterruptedException) {
				throw (InterruptedException) exceptionToThrow;
			}
			if (exceptionToThrow instanceof ExecutionException) {
				throw (ExecutionException) exceptionToThrow;
			}
			if (exceptionToThrow instanceof TimeoutException) {
				throw (TimeoutException) exceptionToThrow;
			}
			throw new ExecutionException(exceptionToThrow);
		}
		return theresult;
	}

}