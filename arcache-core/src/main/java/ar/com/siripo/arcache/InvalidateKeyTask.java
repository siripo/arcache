package ar.com.siripo.arcache;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InvalidateKeyTask implements Future<Boolean> {

	protected final String key;
	protected final boolean hardInvalidation;
	protected final long invalidationWindowSecs;
	protected final ArcacheClient arcache;

	protected boolean cancelled = false;
	protected boolean done = false;
	protected Boolean valueToReturn;
	protected ExecutionException exceptionToThrow;

	protected Future<Object> prevVersionGetFuture;
	protected Future<Boolean> setFuture;

	protected InvalidateKeyTask(String key, boolean hardInvalidation, long invalidationWindowSecs,
			ArcacheClient arcache) {

		this.key = key;
		this.hardInvalidation = hardInvalidation;
		this.invalidationWindowSecs = invalidationWindowSecs;
		this.arcache = arcache;
		start();
	}

	private void start() {
		prevVersionGetFuture = arcache.backendClient.asyncGet(arcache.createInvalidationBackendKey(key));
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled || done) {
			return false;
		}
		if (prevVersionGetFuture != null) {
			prevVersionGetFuture.cancel(mayInterruptIfRunning);
		}
		if (setFuture != null) {
			setFuture.cancel(mayInterruptIfRunning);
		}
		cancelled = true;
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public Boolean get() throws InterruptedException, ExecutionException {
		try {
			return get(arcache.defaultOperationTimeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException toe) {
			throw new ExecutionException(toe);
		}
	}

	@Override
	public Boolean get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		final long timeoutMillis = unit.toMillis(timeout);
		final long startTimeMillis = System.currentTimeMillis();
		return doTask(startTimeMillis, timeoutMillis);
	}

	protected synchronized Boolean doTask(final long startTimeMillis, final long timeoutMillis)
			throws InterruptedException, ExecutionException, TimeoutException {

		if (cancelled) {
			throw new CancellationException();
		}
		if (done) {
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			return valueToReturn;
		}

		CacheInvalidationObject lastValue = null;
		long remainingTime;

		try {
			remainingTime = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
			if (remainingTime <= 0) {
				throw new TimeoutException();
			}
			Object rawCachedObject = prevVersionGetFuture.get(remainingTime, TimeUnit.MILLISECONDS);
			if (rawCachedObject instanceof CacheInvalidationObject) {
				lastValue = (CacheInvalidationObject) rawCachedObject;
			}
		} catch (TimeoutException te) {
			throw te;
		} catch (InterruptedException ie) {
			throw ie;
		} catch (Exception e) {
			// treat as miss
		}

		CacheInvalidationObject invalidationObject = new CacheInvalidationObject();

		invalidationObject.invalidationTimestamp = startTimeMillis / 1000;
		invalidationObject.invalidationWindowSecs = invalidationWindowSecs;
		invalidationObject.isHardInvalidation = hardInvalidation;
		invalidationObject.lastHardInvalidationTimestamp = 0;
		invalidationObject.lastSoftInvalidationTimestamp = 0;

		if (lastValue != null) {
			invalidationObject.lastHardInvalidationTimestamp = lastValue.lastHardInvalidationTimestamp;
			invalidationObject.lastSoftInvalidationTimestamp = lastValue.lastSoftInvalidationTimestamp;

			if (lastValue.isHardInvalidation) {
				invalidationObject.lastHardInvalidationTimestamp = lastValue.invalidationTimestamp;
			} else {
				invalidationObject.lastSoftInvalidationTimestamp = lastValue.invalidationTimestamp;
			}

		}

		setFuture = arcache.backendClient.asyncSet(arcache.createInvalidationBackendKey(key),
				(int) arcache.defaultOperationTimeoutMillis, invalidationObject);

		remainingTime = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
		if (remainingTime <= 0) {
			throw new TimeoutException();
		}
		valueToReturn = setFuture.get(remainingTime, TimeUnit.MILLISECONDS);
		done = true;

		return valueToReturn;
	}
}