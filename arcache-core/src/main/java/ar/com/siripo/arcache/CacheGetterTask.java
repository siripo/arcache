package ar.com.siripo.arcache;

import java.util.HashMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.CacheGetResult.Type;

public class CacheGetterTask implements Future<CacheGetResult> {

	protected final ArcacheClient arcache;
	protected final String key;
	protected boolean cancelled = false;
	protected boolean done = false;
	protected CacheGetResult valueToReturn;
	protected ExecutionException exceptionToThrow;

	protected Future<Object> mainFutureGet;
	protected HashMap<String, Future<Object>> invalidationKeysFutureGets;

	protected CacheGetterTask(ArcacheClient arcache, String key) {
		this.arcache = arcache;
		this.key = key;
		start();
	}

	private void start() {
		mainFutureGet = arcache.backendClient.asyncGet(arcache.createBackendKey(key));
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
			return get(arcache.defaultOperationTimeoutMillis, TimeUnit.MILLISECONDS);
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
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			return valueToReturn;
		}

		long remainingTime = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);

		if (remainingTime <= 0) {
			throw new TimeoutException();
		}

		Object rawCachedObject = mainFutureGet.get(remainingTime, TimeUnit.MILLISECONDS);

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
		result.storeTimestamp = cachedObject.timestamp;
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
			result.invalidatedByKey = inv.key;
		}

		done = true;
		valueToReturn = result;

		return valueToReturn;
	}

	protected boolean isCachedObjectExpired(final ExpirableCacheObject cachedObject, final long currentTimeMillis) {
		long age = (currentTimeMillis / 1000) - cachedObject.timestamp;
		if (age >= cachedObject.maxTTLSecs) {

			return true;

		} else if ((age > cachedObject.minTTLSecs) && (cachedObject.minTTLSecs < cachedObject.maxTTLSecs)) {
			double ageInZone = age - cachedObject.minTTLSecs;
			double invalidationZoneWidth = cachedObject.maxTTLSecs - cachedObject.minTTLSecs;
			double expirationProbability = ageInZone / invalidationZoneWidth;

			if (expirationProbability > arcache.randomGenerator.nextDouble()) {
				return true;
			}

		}

		return false;
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
				Future<Object> fut = arcache.backendClient.asyncGet(arcache.createInvalidationBackendKey(invkey));
				invalidationKeysFutureGets.put(invkey, fut);
			}
		}

		HashMap<String, CacheInvalidationObject> invMap = new HashMap<String, CacheInvalidationObject>();

		// Try to retrieve the InvalidationObjects, with the big constraint of time
		while (invMap.size() < invalidationKeysFutureGets.size()) {

			// if it is Cancelled, exits
			if (cancelled) {
				throw new CancellationException();
			}

			// If have no more time, throws timeout
			long remainingTime = timeoutMillis - (System.currentTimeMillis() - startTimeMillis);
			if (remainingTime <= 0) {
				throw new TimeoutException();
			}

			// Find the first not retrieved
			String invkey = null;
			for (final String itkey : invalidationKeysFutureGets.keySet()) {
				if (!invMap.containsKey(itkey)) {
					invkey = itkey;
					break;
				}
			}

			// Load the key
			CacheInvalidationObject invObj = getsCacheInvalidationObjectFromFuture(
					invalidationKeysFutureGets.get(invkey), remainingTime);
			invMap.put(invkey, invObj);

		}

		return invMap;
	}

	protected CacheInvalidationObject getsCacheInvalidationObjectFromFuture(Future<Object> future, long timeout)
			throws InterruptedException, ExecutionException, TimeoutException {

		Object rawCachedObject = future.get(timeout, TimeUnit.MILLISECONDS);

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
			HashMap<String, CacheInvalidationObject> invalidationMap, final long currentTimeMillis) {
		if (invalidationMap == null) {
			return null;
		}

		// If the key was set more recently than timeMeasurementError assume it valid
		if ((currentTimeMillis / 1000) - arcache.timeMeasurementErrorSecs <= cachedObject.timestamp) {
			return null;
		}

		// Effective time used to test validation. The correction applied is to see the
		// key older than read value and gain more consistency
		long effectiveStoreTimestamp = cachedObject.timestamp - arcache.timeMeasurementErrorSecs;

		for (final String invkey : invalidationMap.keySet()) {
			final CacheInvalidationObject invObj = invalidationMap.get(invkey);
			if (invObj == null) {
				continue;
			}

			// If its older than previous hard invalidation. Its hard invalidated
			if (effectiveStoreTimestamp <= invObj.lastHardInvalidationTimestamp) {
				return new InvalidatedKey(invObj, invkey, true);
			}

			// Test validity against store time of invalidation
			if (effectiveStoreTimestamp <= invObj.invalidationTimestamp) {
				// if no window is configured, it is invalidated right now
				if (invObj.invalidationWindowSecs <= 0) {
					return new InvalidatedKey(invObj, invkey, invObj.isHardInvalidation);
				}
				double invalidTime = invObj.invalidationTimestamp - effectiveStoreTimestamp;
				double normalizedTimeInsideWindow = invalidTime / invObj.invalidationWindowSecs;

				// Apply linear probability
				if (normalizedTimeInsideWindow > arcache.randomGenerator.nextDouble()) {
					return new InvalidatedKey(invObj, invkey, invObj.isHardInvalidation);
				}

			}

			// If its older than previous hard invalidation. Its soft invalidated
			if (effectiveStoreTimestamp <= invObj.lastSoftInvalidationTimestamp) {
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