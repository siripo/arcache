package ar.com.siripo.arcache;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;

/**
 * This task is needed because to set a Invalidation Key is needed some previous
 * invalidation key values if available. If two invalidation keys are set in a
 * very short time. its equivalent to have only one set, (only in the case of
 * the same type of invalidation).
 * 
 * The read (local update) write race condition is not a problem because the
 * time shift is supposed to be very short, and the type of invalidation in this
 * condition has a high probability to be of the same type
 * 
 * 
 * @author Mariano Santamarina
 *
 */
public class InvalidateKeyTask implements Future<Boolean> {

	protected final String key;
	protected final boolean hardInvalidation;
	protected final long invalidationWindowMillis;
	protected final ArcacheBackendClient backendClient;
	protected final BackendKeyBuilder keyBuilder;
	protected final ArcacheConfigurationGetInterface config;

	protected boolean cancelled = false;
	protected boolean done = false;
	protected Boolean valueToReturn;

	protected Future<Object> prevVersionGetFuture;
	protected Future<Boolean> setFuture;

	protected InvalidateKeyTask(String key, boolean hardInvalidation, long invalidationWindowMillis,
			ArcacheBackendClient backendClient, BackendKeyBuilder keyBuilder, ArcacheConfigurationGetInterface config) {

		this.key = key;
		this.hardInvalidation = hardInvalidation;
		this.invalidationWindowMillis = invalidationWindowMillis;
		this.backendClient = backendClient;
		this.keyBuilder = keyBuilder;
		this.config = config;
		start();
	}

	private void start() {
		prevVersionGetFuture = backendClient.asyncGet(keyBuilder.createInvalidationBackendKey(key));
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
			return get(config.getDefaultOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
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
			return valueToReturn;
		}

		CacheInvalidationObject previousInvalidationObject = getPreviousInvalidationObject(startTimeMillis,
				timeoutMillis);

		CacheInvalidationObject invalidationObject = createInvalidationObject(startTimeMillis,
				previousInvalidationObject);

		valueToReturn = setInvalidationObject(startTimeMillis, timeoutMillis, invalidationObject);

		done = true;

		return valueToReturn;
	}

	protected CacheInvalidationObject createInvalidationObject(final long startTimeMillis,
			final CacheInvalidationObject previousInvalidationObject) {
		CacheInvalidationObject invalidationObject = new CacheInvalidationObject();

		invalidationObject.invalidationTimestampMillis = startTimeMillis;
		invalidationObject.invalidationWindowMillis = invalidationWindowMillis;
		invalidationObject.isHardInvalidation = hardInvalidation;
		invalidationObject.lastHardInvalidationTimestampMillis = 0;
		invalidationObject.lastSoftInvalidationTimestampMillis = 0;

		if (previousInvalidationObject != null) {
			invalidationObject.lastHardInvalidationTimestampMillis = previousInvalidationObject.lastHardInvalidationTimestampMillis;
			invalidationObject.lastSoftInvalidationTimestampMillis = previousInvalidationObject.lastSoftInvalidationTimestampMillis;

			if (previousInvalidationObject.isHardInvalidation) {
				invalidationObject.lastHardInvalidationTimestampMillis = previousInvalidationObject.invalidationTimestampMillis;
			} else {
				invalidationObject.lastSoftInvalidationTimestampMillis = previousInvalidationObject.invalidationTimestampMillis;
			}

		}
		return invalidationObject;
	}

	protected CacheInvalidationObject getPreviousInvalidationObject(final long startTimeMillis,
			final long timeoutMillis) throws InterruptedException, ExecutionException, TimeoutException {
		try {
			long remainingTimeMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
			if (remainingTimeMillis <= 0) {
				throw new TimeoutException();
			}
			Object rawCachedObject = prevVersionGetFuture.get(remainingTimeMillis, TimeUnit.MILLISECONDS);
			if (!(rawCachedObject instanceof CacheInvalidationObject)) {
				// In case of invalid type, treat as miss
				return null;
			}
			return (CacheInvalidationObject) rawCachedObject;
		} catch (TimeoutException te) {
			throw te;
		} catch (InterruptedException ie) {
			throw ie;
		} catch (Exception e) {
			// treat as miss
			return null;
		}
	}

	protected boolean setInvalidationObject(final long startTimeMillis, final long timeoutMillis,
			CacheInvalidationObject invalidationObject)
			throws InterruptedException, ExecutionException, TimeoutException {

		setFuture = backendClient.asyncSet(keyBuilder.createInvalidationBackendKey(key),
				config.getDefaultStoredObjectRemovalTimeMillis(), invalidationObject);

		long remainingTimeMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
		if (remainingTimeMillis <= 0) {
			throw new TimeoutException();
		}
		return setFuture.get(remainingTimeMillis, TimeUnit.MILLISECONDS);
	}

}