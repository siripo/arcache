package ar.com.siripo.arcache.backend.speedup;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.CacheInvalidationObjectType;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;
import ar.com.siripo.arcache.math.AdjustedExponentialProbabilityFunction;
import ar.com.siripo.arcache.math.ProbabilityFunction;
import ar.com.siripo.arcache.util.DummyFuture;

/**
 * This is a local memory proxy of another backend. The purpose is speed up the
 * access to a slow backend.
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheSpeedupClient implements ArcacheBackendClient, ArcacheSpeedupConfigurationSetInterface {

	protected ArcacheBackendClient backendClient;

	protected int invalidationKeysCacheSize = 0;
	protected int objectsCacheSize = 0;
	protected int missesCacheSize = 0;

	protected long invalidationKeysExpirationMillis = 0;
	protected long objectsExpirationMillis = 0;
	protected long missesExpirationMillis = 0;

	protected boolean protectAgainstBackendFailures = true;
	protected int speedupCacheTTLSeconds = 365 * 24 * 3600;
	protected ProbabilityFunction expirationProbabilityFunction;

	protected ArcacheInMemoryClient invalidationKeysCache = null;
	protected ArcacheInMemoryClient objectsCache = null;
	protected ArcacheInMemoryClient missesCache = null;

	protected boolean initialized = false;

	protected Random random;

	public ArcacheSpeedupClient() {
	}

	@Override
	public void setBackendClient(ArcacheBackendClient backendClient) {
		this.backendClient = backendClient;
	}

	@Override
	public void setInvalidationKeysCacheSize(int cacheSize) {
		validateSetCacheSize(cacheSize);
		this.invalidationKeysCacheSize = cacheSize;
	}

	@Override
	public void setObjectsCacheSize(int cacheSize) {
		validateSetCacheSize(cacheSize);
		this.objectsCacheSize = cacheSize;
	}

	@Override
	public void setMissesCacheSize(int cacheSize) {
		validateSetCacheSize(cacheSize);
		this.missesCacheSize = cacheSize;
	}

	@Override
	public void setInvalidationKeysExpirationMillis(long expirationMillis) {
		validateSetExpirationMillis(expirationMillis);
		this.invalidationKeysExpirationMillis = expirationMillis;
	}

	@Override
	public void setObjectsExpirationMillis(long expirationMillis) {
		validateSetExpirationMillis(expirationMillis);
		this.objectsExpirationMillis = expirationMillis;
	}

	@Override
	public void setMissesExpirationMillis(long expirationMillis) {
		validateSetExpirationMillis(expirationMillis);
		this.missesExpirationMillis = expirationMillis;
	}

	@Override
	public void setProtectAgainstBackendFailures(boolean protect) {
		this.protectAgainstBackendFailures = protect;
	}

	@Override
	public void setSpeedupCacheTTLSeconds(int ttlSeconds) {
		this.speedupCacheTTLSeconds = ttlSeconds;
	}

	@Override
	public void setExpirationProbabilityFunction(ProbabilityFunction probabilityFunction) {
		this.expirationProbabilityFunction = probabilityFunction;
	}

	public void initialize() {
		if (initialized) {
			throw new IllegalStateException("Already Initialized");
		}

		if ((invalidationKeysCacheSize != 0) || (invalidationKeysExpirationMillis != 0)) {
			if ((invalidationKeysCacheSize == 0) || (invalidationKeysExpirationMillis == 0)) {
				throw new IllegalArgumentException("InvalidationKeys Cache Policy is invalid");
			}
			invalidationKeysCache = new ArcacheInMemoryClient(invalidationKeysCacheSize);
		}

		if ((objectsCacheSize != 0) || (objectsExpirationMillis != 0)) {
			if ((objectsCacheSize == 0) || (objectsExpirationMillis == 0)) {
				throw new IllegalArgumentException("Objects Cache Policy is invalid");
			}
			objectsCache = new ArcacheInMemoryClient(objectsCacheSize);
		}

		if ((missesCacheSize != 0) || (missesExpirationMillis != 0)) {
			if ((missesCacheSize == 0) || (missesExpirationMillis == 0)) {
				throw new IllegalArgumentException("Misses Cache Policy is invalid");
			}
			missesCache = new ArcacheInMemoryClient(missesCacheSize);
		}

		if ((objectsCache == null) && (invalidationKeysCache == null) && (missesCache == null)) {
			throw new IllegalArgumentException(
					"it was not configured neither invalidationKeys nor objects nor misses policy");
		}

		if (backendClient == null) {
			throw new IllegalArgumentException("Backend Client was not configured");
		}

		if (expirationProbabilityFunction == null) {
			expirationProbabilityFunction = new AdjustedExponentialProbabilityFunction(0, 11);
		}
		random = new Random();

		initialized = true;
	}

	public void clear() {
		if (objectsCache != null) {
			objectsCache.clear();
		}
		if (invalidationKeysCache != null) {
			invalidationKeysCache.clear();
		}
		if (missesCache != null) {
			missesCache.clear();
		}
	}

	protected void validateSetCacheSize(int cachesize) {
		// The value must be 0 to disable, >0 to specify the number of elements
		if (cachesize < 0) {
			throw new IllegalArgumentException("Invalid cache size");
		}
	}

	protected void validateSetExpirationMillis(long expirationMillis) {
		// The value must be greater than 0
		if (expirationMillis <= 0) {
			throw new IllegalArgumentException("Invalid expiration millis");
		}
	}

	@Override
	public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
		try {
			storeSpeedupCache(key, value);
		} catch (Exception e) {
			// This catch is here because the speedup is a wish functionality, if it does
			// not work must do not break the inner backend access
		}
		return backendClient.asyncSet(key, ttlSeconds, value);
	}

	protected void storeSpeedupCache(String key, Object value) {
		if (value == null) {
			if (invalidationKeysCache != null) {
				invalidationKeysCache.remove(key);
			}
			if (objectsCache != null) {
				objectsCache.remove(key);
			}
			if (missesCache != null) {
				missesCache.set(key, speedupCacheTTLSeconds, createSpeedupCacheObject(value));
			}
		} else {
			if (value instanceof CacheInvalidationObjectType) {
				if (invalidationKeysCache != null) {
					invalidationKeysCache.set(key, speedupCacheTTLSeconds, createSpeedupCacheObject(value));
				}
			} else {
				if (objectsCache != null) {
					objectsCache.set(key, speedupCacheTTLSeconds, createSpeedupCacheObject(value));
				}
			}
			if (missesCache != null) {
				missesCache.remove(key);
			}
		}
	}

	@Override
	public Future<Object> asyncGet(String key) {
		// First try to restore the value from any cache.
		// If its found returns the value

		try {
			RestoredSpeedupCacheObject rsco = restoreObjectFromAnySpeedupCache(key);
			if ((rsco != null) && (!rsco.expired)) {
				return new DummyFuture<Object>(rsco.speedupCacheObject.cachedObject);
			}
		} catch (Exception e) {

		}

		// Otherwise create a wrapper to capture the backend value
		Future<Object> backendFuture = backendClient.asyncGet(key);

		try {
			return new FutureBackendGetWrapper(backendFuture, key);
		} catch (Exception e) {

		}

		return backendFuture;
	}

	protected SpeedupCacheObject createSpeedupCacheObject(Object value) {
		SpeedupCacheObject sco = new SpeedupCacheObject();
		sco.storeTimeMillis = System.currentTimeMillis();
		sco.cachedObject = value;
		return sco;
	}

	protected RestoredSpeedupCacheObject restoreObjectFromAnySpeedupCache(String key) {
		RestoredSpeedupCacheObject rsco = null;

		if (invalidationKeysCache != null) {
			rsco = restoreObjectFromSpeedupCache(key, invalidationKeysCache, invalidationKeysExpirationMillis);
		}
		if ((rsco != null) && (objectsCache != null)) {
			rsco = restoreObjectFromSpeedupCache(key, objectsCache, objectsExpirationMillis);
		}
		if ((rsco != null) && (missesCache != null)) {
			rsco = restoreObjectFromSpeedupCache(key, missesCache, missesExpirationMillis);
		}

		return rsco;
	}

	protected RestoredSpeedupCacheObject restoreObjectFromSpeedupCache(String key, ArcacheInMemoryClient speedupCache,
			long timeoutMillis) {

		SpeedupCacheObject sco = (SpeedupCacheObject) speedupCache.get(key);
		if (sco == null) {
			return null;
		}
		RestoredSpeedupCacheObject rsco = new RestoredSpeedupCacheObject();
		rsco.speedupCacheObject = sco;

		double age = System.currentTimeMillis() - sco.storeTimeMillis;
		double normalizedTimeInsideWindow = age / timeoutMillis;
		double invalidationProbability = expirationProbabilityFunction.getProbability(normalizedTimeInsideWindow);

		if ((invalidationProbability >= 1) || (invalidationProbability > random.nextDouble())) {
			rsco.expired = true;
		}

		return rsco;
	}

	protected class FutureBackendGetWrapper implements Future<Object> {

		protected Future<Object> backendFuture;
		protected String key;

		public FutureBackendGetWrapper(Future<Object> backendFuture, String key) {
			this.backendFuture = backendFuture;
			this.key = key;
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
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
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
				storeSpeedupCache(key, getResult);
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
				RestoredSpeedupCacheObject rsco = restoreObjectFromAnySpeedupCache(key);
				if (rsco != null) {
					return rsco.speedupCacheObject.cachedObject;
				}
			}
			throw new Exception("No protection available");
		}

	}

}
