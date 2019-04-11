package ar.com.siripo.arcache;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.CacheGetResult.Type;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;

public class CacheGetterTask implements Future<CacheGetResult> {

	protected final String key;
	protected final ArcacheBackendClient backendClient;
	protected final BackendKeyBuilder keyBuilder;
	protected final ArcacheConfigurationGetInterface config;
	protected final Random random;
	protected boolean cancelled = false;
	protected boolean done = false;
	protected CacheGetResult valueToReturn;

	protected Future<Object> mainFutureGet;
	protected HashMap<String, Future<Object>> invalidationKeysFutureGets;

	protected CacheGetterTask(String key, ArcacheBackendClient backendClient, BackendKeyBuilder keyBuilder,
			ArcacheConfigurationGetInterface config, Random random) {
		this.key = key;
		this.backendClient = backendClient;
		this.keyBuilder = keyBuilder;
		this.config = config;
		this.random = random;

		start();
	}

	private void start() {
		mainFutureGet = backendClient.asyncGet(keyBuilder.createBackendKey(key));
	}

	@Override
	public synchronized boolean cancel(boolean mayInterruptIfRunning) {
		if (cancelled || done) {
			return false;
		}
		if (mainFutureGet != null) {
			mainFutureGet.cancel(mayInterruptIfRunning);
		}
		if (invalidationKeysFutureGets != null) {
			for (Future<Object> f : invalidationKeysFutureGets.values()) {
				f.cancel(mayInterruptIfRunning);
			}
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
	public CacheGetResult get() throws InterruptedException, ExecutionException {
		try {
			return get(config.getDefaultOperationTimeoutMillis(), TimeUnit.MILLISECONDS);
		} catch (TimeoutException toe) {
			throw new ExecutionException(toe);
		}
	}

	@Override
	public CacheGetResult get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		final long timeoutMillis = unit.toMillis(timeout);
		final long startTimeMillis = System.currentTimeMillis();
		return doTask(startTimeMillis, timeoutMillis);
	}

	protected synchronized CacheGetResult doTask(final long startTimeMillis, final long timeoutMillis)
			throws InterruptedException, ExecutionException, TimeoutException {

		if (cancelled) {
			throw new CancellationException();
		}
		if (done) {
			return valueToReturn;
		}

		long remainingTimeMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);

		if (remainingTimeMillis <= 0) {
			throw new TimeoutException();
		}

		Object rawCachedObject = mainFutureGet.get(remainingTimeMillis, TimeUnit.MILLISECONDS);

		// In case of a MISS, returns now and stores the result
		if (rawCachedObject == null) {
			done = true;
			valueToReturn = new CacheGetResult(Type.MISS);
			return valueToReturn;
		}

		// Invalid type
		if (!(rawCachedObject instanceof ExpirableCacheObject)) {
			done = true;
			valueToReturn = new CacheGetResult(Type.ERROR,
					new UnexpectedObjectType(ExpirableCacheObject.class, rawCachedObject.getClass()));
			return valueToReturn;
		}

		ExpirableCacheObject cachedObject = (ExpirableCacheObject) rawCachedObject;

		// Load all the invalidation Objects
		HashMap<String, CacheInvalidationObject> invalidationMap = loadInvalidationKeys(cachedObject, startTimeMillis,
				timeoutMillis);

		// Build the result
		CacheGetResult result = new CacheGetResult(Type.HIT);
		result.value = cachedObject.value;
		result.storeTimestampMillis = cachedObject.timestampMillis;
		result.invalidationKeys = cachedObject.invalidationKeys;

		// is Expired?
		if (isCachedObjectExpired(cachedObject, startTimeMillis)) {
			result.type = Type.EXPIRED;
		}

		// is invalidated?
		InvalidatedKey inv = isCachedObjectInvalidated(cachedObject, invalidationMap, startTimeMillis);
		if (inv != null) {
			// if its hard invalidated, the result must be a MISS
			if (inv.hardInvalidation) {
				done = true;
				valueToReturn = new CacheGetResult(Type.MISS);
				return valueToReturn;
			}
			result.type = Type.INVALIDATED;
			result.invalidatedKey = inv.key;
		}

		done = true;
		valueToReturn = result;

		return valueToReturn;
	}

	protected boolean isCachedObjectExpired(final ExpirableCacheObject cachedObject, final long currentTimeMillis) {
		/*
		 * The calculations are done in milliseconds so that it performs better the
		 * probability function
		 */
		double agems = currentTimeMillis - cachedObject.timestampMillis;
		double expms = cachedObject.expirationTTLMillis;

		double age_normalized = 1;
		if (expms > 0) {
			age_normalized = agems / expms;
		}
		double expirationProbability = config.getExpirationProbabilityFunction().getProbability(age_normalized);
		if (expirationProbability <= 0) {
			return false;
		}
		if (expirationProbability >= 1) {
			return true;
		}

		return (expirationProbability > random.nextDouble());
	}

	protected HashMap<String, CacheInvalidationObject> loadInvalidationKeys(final ExpirableCacheObject cachedObject,
			final long startTimeMillis, final long timeoutMillis)
			throws TimeoutException, InterruptedException, ExecutionException {

		if ((cachedObject.invalidationKeys == null) || (cachedObject.invalidationKeys.length <= 0)) {
			return (null);
		}

		if (invalidationKeysFutureGets == null) {
			invalidationKeysFutureGets = new HashMap<String, Future<Object>>();
		}

		// Build the missing futures
		for (final String invkey : cachedObject.invalidationKeys) {
			if (!invalidationKeysFutureGets.containsKey(invkey)) {
				Future<Object> fut = backendClient.asyncGet(keyBuilder.createInvalidationBackendKey(invkey));
				invalidationKeysFutureGets.put(invkey, fut);
			}
		}

		HashMap<String, CacheInvalidationObject> invMap = new HashMap<String, CacheInvalidationObject>();

		// For every InvalidationKey load the invalidationObject
		for (final String invkey : cachedObject.invalidationKeys) {

			// if it is Cancelled, exits
			if (cancelled) {
				throw new CancellationException();
			}

			// If have no more time, throws timeout
			long remainingTimeMillis = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
			if (remainingTimeMillis <= 0) {
				throw new TimeoutException();
			}

			// Load the key
			CacheInvalidationObject invObj = getsCacheInvalidationObjectFromFuture(
					invalidationKeysFutureGets.get(invkey), remainingTimeMillis);
			invMap.put(invkey, invObj);

		}

		return invMap;
	}

	protected CacheInvalidationObject getsCacheInvalidationObjectFromFuture(Future<Object> future, long timeoutMillis)
			throws InterruptedException, ExecutionException, TimeoutException {

		Object rawCachedObject = future.get(timeoutMillis, TimeUnit.MILLISECONDS);

		// In miss case, no problem, returns miss
		if (rawCachedObject == null) {
			return null;
		}

		// If a invalid type is retreived, asume as miss and skip for protection
		if (!(rawCachedObject instanceof CacheInvalidationObject)) {
			return null;
		}

		return (CacheInvalidationObject) rawCachedObject;
	}

	protected InvalidatedKey isCachedObjectInvalidated(final ExpirableCacheObject cachedObject,
			final HashMap<String, CacheInvalidationObject> invalidationMap, final long currentTimeMillis) {
		if (invalidationMap == null) {
			return null;
		}

		// If the key was set more recently than timeMeasurementError assume its valid
		if (currentTimeMillis - config.getTimeMeasurementErrorMillis() < cachedObject.timestampMillis) {
			return null;
		}

		// Effective time used to test validation. The correction applied is to see the
		// key older than read value and gain more consistency
		long effectiveStoreTimestampMillis = cachedObject.timestampMillis - config.getTimeMeasurementErrorMillis();

		for (final String invkey : invalidationMap.keySet()) {
			final CacheInvalidationObject invObj = invalidationMap.get(invkey);
			if (invObj == null) {
				continue;
			}

			// If its older than previous hard invalidation. Its hard invalidated
			if (effectiveStoreTimestampMillis <= invObj.lastHardInvalidationTimestampMillis) {
				return new InvalidatedKey(invObj, invkey, true);
			}

			// Test validity against store time of invalidation
			if (effectiveStoreTimestampMillis <= invObj.invalidationTimestampMillis) {
				// if no window is configured, it is invalidated right now
				if (invObj.invalidationWindowMillis <= 0) {
					return new InvalidatedKey(invObj, invkey, invObj.isHardInvalidation);
				}
				double invalidTimeMS = invObj.invalidationTimestampMillis - effectiveStoreTimestampMillis;
				double normalizedTimeInsideWindow = invalidTimeMS / invObj.invalidationWindowMillis;
				double invalidationProbability = config.getInvalidationProbabilityFunction()
						.getProbability(normalizedTimeInsideWindow);

				if ((invalidationProbability >= 1) || (invalidationProbability > random.nextDouble())) {
					return new InvalidatedKey(invObj, invkey, invObj.isHardInvalidation);
				}

			}

			// If its older than previous soft invalidation. Its soft invalidated
			if (effectiveStoreTimestampMillis <= invObj.lastSoftInvalidationTimestampMillis) {
				return new InvalidatedKey(invObj, invkey, false);
			}
		}

		return (null);
	}

	protected static class InvalidatedKey {
		CacheInvalidationObject cacheInvalidationObject;
		String key;
		boolean hardInvalidation;

		InvalidatedKey(CacheInvalidationObject cacheInvalidationObject, String key, boolean hardInvalidation) {
			this.cacheInvalidationObject = cacheInvalidationObject;
			this.key = key;
			this.hardInvalidation = hardInvalidation;
		}
	}

}