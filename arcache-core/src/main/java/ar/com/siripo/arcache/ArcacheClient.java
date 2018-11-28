package ar.com.siripo.arcache;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.util.DummyFuture;

public class ArcacheClient implements ArcacheClientInterface {

	protected long defaultOperationTimeoutMillis = 50000;
	protected long timeMeasurementErrorSecs = 2;
	protected long defaultInvalidationWindowSecs = 5;
	protected String keyNamespace = null;
	protected String keyDelimiter = "|";
	protected String invalidationKeyPrefix = "InvKey";
	protected long defaultExpirationTimeSecs = 3600;
	protected long defaultRemoveTimeSecs = 86400;

	protected ArcacheBackendClient backendClient;

	protected Random randomGenerator;

	public ArcacheClient(ArcacheBackendClient backendClient) {
		this.backendClient = backendClient;
		randomGenerator = new Random();
	}

	@Override
	public void setDefaultOperationTimeout(long timeoutMillis) {
		defaultOperationTimeoutMillis = timeoutMillis;
	}

	@Override
	public void setTimeMeasurementError(long errorSecs) {
		timeMeasurementErrorSecs = errorSecs;
	}

	@Override
	public void setDefaultInvalidationWindow(long windowSecs) {
		defaultInvalidationWindowSecs = windowSecs;
	}

	@Override
	public void setKeyNamespace(String namespace) {
		keyNamespace = namespace;
		if ("".equals(keyNamespace)) {
			keyNamespace = null;
		}
	}

	@Override
	public void setKeyDelimiter(String keyDelimiter) {
		this.keyDelimiter = keyDelimiter;
	}

	@Override
	public void setDefaultExpirationTime(long expirationTimeSecs) {
		this.defaultExpirationTimeSecs = expirationTimeSecs;

	}

	@Override
	public void setDefaultRemoveTime(long removeTimeSecs) {
		this.defaultRemoveTimeSecs = removeTimeSecs;
	}

	@Override
	public Object get(String key) throws TimeoutException, Exception {
		return get(key, defaultOperationTimeoutMillis);
	}

	@Override
	public Object get(String key, long timeoutMillis) throws TimeoutException, Exception {
		CacheGetResult cacheGetResult = getCacheObject(key, timeoutMillis);
		switch (cacheGetResult.type) {
		case HIT:
			return cacheGetResult.value;
		case ERROR:
		case TIMEOUT:
			throw cacheGetResult.cause;
		case MISS:
		case EXPIRED:
		case INVALIDATED:
			return null;
		default:
			return new Exception("Invalid case");
		}
	}

	@Override
	public CacheGetResult getCacheObject(String key) {
		return getCacheObject(key, defaultOperationTimeoutMillis);
	}

	@Override
	public CacheGetResult getCacheObject(String key, long timeoutMillis) {
		try {
			Future<CacheGetResult> getFuture = asyncGetCacheObject(key);
			CacheGetResult r = getFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
			if (r == null) {
				throw new NullPointerException();
			}
			return r;
		} catch (TimeoutException tx) {
			CacheGetResult err = new CacheGetResult();
			err.type = CacheGetResult.Type.TIMEOUT;
			err.cause = tx;
			return err;

		} catch (Exception e) {
			CacheGetResult err = new CacheGetResult();
			err.type = CacheGetResult.Type.ERROR;
			err.cause = e;
			return err;
		}
	}

	@Override
	public void set(String key, Object value) throws TimeoutException, Exception {
		set(key, value, null);
	}

	@Override
	public void set(String key, Object value, String[] invalidationKeys) throws TimeoutException, Exception {
		Future<Boolean> future = asyncSet(key, value, invalidationKeys);
		future.get();
	}

	@Override
	public void invalidateKey(String key) throws TimeoutException, Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void invalidateKey(String key, long invalidationWindowSecs) throws TimeoutException, Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void invalidateKey(String key, boolean hardInvalidation) throws TimeoutException, Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void invalidateKey(String key, boolean hardInvalidation, long invalidationWindowSecs)
			throws TimeoutException, Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Future<CacheGetResult> asyncGetCacheObject(String key) {
		try {
			return new CacheGetterTask(key);
		} catch (Exception e) {
			return DummyFuture.createWithException(e);
		}
	}

	@Override
	public Future<Boolean> asyncSet(String key, Object value, String[] invalidationKeys) {
		try {
			ExpirableCacheObject expObj = new ExpirableCacheObject();
			expObj.timestamp = System.currentTimeMillis() / 1000;
			expObj.value = value;
			expObj.invalidationKeys = invalidationKeys;
			expObj.maxTTLSecs = defaultExpirationTimeSecs;
			expObj.minTTLSecs = defaultExpirationTimeSecs / 2;
			String backendKey = createBackendKey(key);
			return backendClient.asyncSet(backendKey, (int) defaultRemoveTimeSecs, expObj);

		} catch (Exception e) {
			return DummyFuture.createWithException(e);
		}
	}

	@Override
	public Future<Boolean> asyncSet(String key, Object value) {
		return asyncSet(key, value, null);
	}

	/** Create the key to be used in the backend client */
	protected String createBackendKey(String userKey) {
		if (keyNamespace == null) {
			return userKey;
		}
		return keyNamespace + keyDelimiter + userKey;
	}

	protected String createInvalidationBackendKey(String invalidationKey) {
		return createBackendKey(invalidationKeyPrefix + keyDelimiter + invalidationKey);
	}

	protected class CacheGetterTask implements Future<CacheGetResult> {

		protected final String key;
		protected boolean cancelled = false;
		protected boolean done = false;
		protected CacheGetResult valueToReturn;
		protected ExecutionException exceptionToThrow;

		protected Future<Object> mainFutureGet;
		protected HashMap<String, Future<Object>> invalidationKeysFutureGets;

		protected CacheGetterTask(String key) {
			this.key = key;
			start();
		}

		private void start() {
			mainFutureGet = backendClient.asyncGet(createBackendKey(key));
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
				return get(defaultOperationTimeoutMillis, TimeUnit.MILLISECONDS);
			} catch (TimeoutException toe) {
				throw new ExecutionException(toe);
			}
		}

		@Override
		public synchronized CacheGetResult get(long timeout, TimeUnit unit)
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
			long millisTimeout = unit.toMillis(timeout);
			final long startTime = System.currentTimeMillis();

			Object rawCachedObject = mainFutureGet.get(millisTimeout, TimeUnit.MILLISECONDS);

			// In case of a MISS, returns now and stores the result
			if (rawCachedObject == null) {
				done = true;
				valueToReturn = new CacheGetResult();
				valueToReturn.type = CacheGetResult.Type.MISS;
				return valueToReturn;
			}

			// Invalid type
			if (!(rawCachedObject instanceof ExpirableCacheObject)) {
				done = true;
				valueToReturn = new CacheGetResult();
				valueToReturn.type = CacheGetResult.Type.ERROR;
				valueToReturn.cause = new UnexpectedObjectType(ExpirableCacheObject.class, rawCachedObject.getClass());
				return valueToReturn;
			}

			ExpirableCacheObject cachedObject = (ExpirableCacheObject) rawCachedObject;

			// Need Load Invalidation Keys?
			if ((cachedObject.invalidationKeys != null) && (cachedObject.invalidationKeys.length > 0)) {
				// TODO Implementar
				throw new UnsupportedOperationException("Code Not implemented");
			}

			done = true;
			valueToReturn = new CacheGetResult();
			valueToReturn.value = cachedObject.value;
			valueToReturn.storeTimestamp = cachedObject.timestamp;
			valueToReturn.type = CacheGetResult.Type.HIT;

			// Test against Expiration
			long age = (startTime / 1000) - cachedObject.timestamp;
			if (age >= cachedObject.maxTTLSecs) {

				valueToReturn.type = CacheGetResult.Type.EXPIRED;

			} else if ((age > cachedObject.minTTLSecs) && (cachedObject.minTTLSecs < cachedObject.maxTTLSecs)) {
				double ageInZone = age - cachedObject.minTTLSecs;
				double invalidationZoneWidth = cachedObject.maxTTLSecs - cachedObject.minTTLSecs;
				double expirationProbability = ageInZone / invalidationZoneWidth;

				if (expirationProbability > randomGenerator.nextDouble()) {
					valueToReturn.type = CacheGetResult.Type.EXPIRED;
				}

			}

			return valueToReturn;
		}

	}
}
