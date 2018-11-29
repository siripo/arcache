package ar.com.siripo.arcache;

import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.CacheGetResult.Type;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.util.DummyFuture;

public class ArcacheClient implements ArcacheClientInterface {

	protected long defaultOperationTimeoutMillis = 500;
	protected long timeMeasurementErrorSecs = 4;
	protected long defaultInvalidationWindowSecs = 5;
	protected boolean defaultHardInvalidation = true;
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
	public void setDefaultHardInvalidation(boolean hardInvalidation) {
		defaultHardInvalidation = hardInvalidation;
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
			CacheGetResult err = new CacheGetResult(Type.TIMEOUT, tx);
			return err;

		} catch (Exception e) {
			CacheGetResult err = new CacheGetResult(Type.ERROR, e);
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
		invalidateKey(key, defaultInvalidationWindowSecs);
	}

	@Override
	public void invalidateKey(String key, long invalidationWindowSecs) throws TimeoutException, Exception {
		invalidateKey(key, defaultHardInvalidation, invalidationWindowSecs);
	}

	@Override
	public void invalidateKey(String key, boolean hardInvalidation) throws TimeoutException, Exception {
		invalidateKey(key, hardInvalidation, defaultInvalidationWindowSecs);
	}

	@Override
	public void invalidateKey(String key, boolean hardInvalidation, long invalidationWindowSecs)
			throws TimeoutException, Exception {
		Future<Boolean> future = asyncInvalidateKey(key, hardInvalidation, invalidationWindowSecs);
		future.get();
	}

	@Override
	public Future<Boolean> asyncInvalidateKey(String key, boolean hardInvalidation, long invalidationWindowSecs) {
		try {
			return new InvalidateKeyTask(key, hardInvalidation, invalidationWindowSecs, this);
		} catch (Exception e) {
			return DummyFuture.createWithException(e);
		}
	}

	@Override
	public Future<CacheGetResult> asyncGetCacheObject(String key) {
		try {
			return new CacheGetterTask(this, key);
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

}
