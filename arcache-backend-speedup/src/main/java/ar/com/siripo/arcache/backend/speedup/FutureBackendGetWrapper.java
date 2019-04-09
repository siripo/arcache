package ar.com.siripo.arcache.backend.speedup;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class FutureBackendGetWrapper implements Future<Object> {

	protected Future<Object> backendFuture;
	protected String key;
	protected ArcacheSpeedupClient speedupClient;
	protected boolean protectAgainstBackendFailures;

	public FutureBackendGetWrapper(ArcacheSpeedupClient speedupClient, Future<Object> backendFuture, String key,
			boolean protectAgainstBackendFailures) {
		this.backendFuture = backendFuture;
		this.key = key;
		this.speedupClient = speedupClient;
		this.protectAgainstBackendFailures = protectAgainstBackendFailures;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return backendFuture.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return backendFuture.isCancelled();
	}

	@Override
	public boolean isDone() {
		return backendFuture.isDone();
	}

	@Override
	public Object get() throws InterruptedException, ExecutionException {
		try {
			return wrappGetResult(backendFuture.get());
		} catch (InterruptedException ie) {
			return wrappGetInterruptedException(ie);
		} catch (ExecutionException ee) {
			return wrappGetExecutionException(ee);
		}
	}

	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		try {
			return wrappGetResult(backendFuture.get(timeout, unit));
		} catch (InterruptedException ie) {
			return wrappGetInterruptedException(ie);
		} catch (ExecutionException ee) {
			return wrappGetExecutionException(ee);
		} catch (TimeoutException te) {
			return wrappGetTimeoutException(te);
		}
	}

	protected Object wrappGetResult(Object getResult) {
		try {
			speedupClient.storeSpeedupCache(key, getResult);
		} catch (Exception e) {
		}
		return getResult;
	}

	protected Object wrappGetInterruptedException(InterruptedException cause) throws InterruptedException {
		try {
			return doProtection();
		} catch (Exception e) {
		}
		throw cause;
	}

	protected Object wrappGetExecutionException(ExecutionException cause) throws ExecutionException {
		try {
			return doProtection();
		} catch (Exception e) {
		}
		throw cause;
	}

	protected Object wrappGetTimeoutException(TimeoutException cause) throws TimeoutException {
		try {
			return doProtection();
		} catch (Exception e) {
		}
		throw cause;
	}

	protected Object doProtection() throws Exception {
		if (protectAgainstBackendFailures) {
			RestoredSpeedupCacheObject rsco = speedupClient.restoreObjectFromAnySpeedupCache(key);
			if (rsco != null) {
				return rsco.speedupCacheObject.cachedObject;
			}
		}
		throw new Exception("No protection available");
	}

}