package ar.com.siripo.arcache.backend.speedup;

import java.util.Random;
import java.util.concurrent.Future;

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
public class ArcacheSpeedupClient implements ArcacheBackendClient, ArcacheSpeedupConfigurationSetInterface,
		ArcacheSpeedupConfigurationGetInterface {

	protected ArcacheBackendClient backendClient;

	protected int invalidationKeysCacheSize = 0;
	protected int objectsCacheSize = 0;
	protected int missesCacheSize = 0;

	protected long invalidationKeysExpirationMillis = 0;
	protected long objectsExpirationMillis = 0;
	protected long missesExpirationMillis = 0;

	protected boolean protectAgainstBackendFailures = true;
	protected long speedupCacheTTLMillis = 365 * 24 * 3600000L;
	protected ProbabilityFunction expirationProbabilityFunction;

	protected ArcacheSpeedupTracker tracker = null;

	protected boolean cacheIsolation = false;

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
	public void setSpeedupCacheTTLMillis(long ttlMillis) {
		this.speedupCacheTTLMillis = ttlMillis;
	}

	@Override
	public void setExpirationProbabilityFunction(ProbabilityFunction probabilityFunction) {
		this.expirationProbabilityFunction = probabilityFunction;
	}

	@Override
	public void setTracker(ArcacheSpeedupTracker tracker) {
		this.tracker = tracker;
	}

	@Override
	public ArcacheBackendClient getBackendClient() {
		return backendClient;
	}

	@Override
	public int getInvalidationKeysCacheSize() {
		return invalidationKeysCacheSize;
	}

	@Override
	public int getObjectsCacheSize() {
		return objectsCacheSize;
	}

	@Override
	public int getMissesCacheSize() {
		return missesCacheSize;
	}

	@Override
	public long getInvalidationKeysExpirationMillis() {
		return invalidationKeysExpirationMillis;
	}

	@Override
	public long getObjectsExpirationMillis() {
		return objectsExpirationMillis;
	}

	@Override
	public long getMissesExpirationMillis() {
		return missesExpirationMillis;
	}

	@Override
	public boolean getProtectAgainstBackendFailures() {
		return protectAgainstBackendFailures;
	}

	@Override
	public long getSpeedupCacheTTLMillis() {
		return speedupCacheTTLMillis;
	}

	@Override
	public ProbabilityFunction getExpirationProbabilityFunction() {
		return expirationProbabilityFunction;
	}

	@Override
	public ArcacheSpeedupTracker getTracker() {
		return tracker;
	}

	@Override
	public boolean getCacheIsolation() {
		return cacheIsolation;
	}

	@Override
	public void setCacheIsolation(boolean cacheIsolation) {
		this.cacheIsolation = cacheIsolation;
	}

	public void initialize() {
		if (initialized) {
			throw new IllegalStateException("Already Initialized");
		}

		if ((invalidationKeysCacheSize != 0) || (invalidationKeysExpirationMillis != 0)) {
			if ((invalidationKeysCacheSize == 0) || (invalidationKeysExpirationMillis == 0)) {
				throw new IllegalArgumentException("InvalidationKeys Cache Policy is invalid");
			}
			invalidationKeysCache = new ArcacheInMemoryClient(invalidationKeysCacheSize, cacheIsolation);
		}

		if ((objectsCacheSize != 0) || (objectsExpirationMillis != 0)) {
			if ((objectsCacheSize == 0) || (objectsExpirationMillis == 0)) {
				throw new IllegalArgumentException("Objects Cache Policy is invalid");
			}
			objectsCache = new ArcacheInMemoryClient(objectsCacheSize, cacheIsolation);
		}

		if ((missesCacheSize != 0) || (missesExpirationMillis != 0)) {
			if ((missesCacheSize == 0) || (missesExpirationMillis == 0)) {
				throw new IllegalArgumentException("Misses Cache Policy is invalid");
			}
			// The misses cache is never isolated
			missesCache = new ArcacheInMemoryClient(missesCacheSize, false);
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

		if (tracker == null) {
			tracker = new ArcacheSpeedupBasicTracker();
		}

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
	public Future<Boolean> asyncSet(String key, long ttlMillis, Object value) {
		try {
			storeSpeedupCache(key, value);
		} catch (Exception e) {
			// This catch is here because the speedup is a wish functionality, if it does
			// not work must do not break the inner backend access
			tracker.trackException(key, e);
		}
		return backendClient.asyncSet(key, ttlMillis, value);
	}

	protected ArcacheInMemoryClient storeSpeedupCache(String key, Object value) {
		ArcacheInMemoryClient destination = null;
		if (value == null) {
			if (invalidationKeysCache != null) {
				invalidationKeysCache.remove(key);
			}
			if (objectsCache != null) {
				objectsCache.remove(key);
			}
			if (missesCache != null) {
				destination = missesCache;
			}
		} else {
			if (value instanceof CacheInvalidationObjectType) {
				if (invalidationKeysCache != null) {
					destination = invalidationKeysCache;
				}
				if (objectsCache != null) {
					objectsCache.remove(key);
				}
			} else {
				if (objectsCache != null) {
					destination = objectsCache;
				}
				if (invalidationKeysCache != null) {
					invalidationKeysCache.remove(key);
				}
			}
			if (missesCache != null) {
				missesCache.remove(key);
			}
		}

		if (destination != null) {
			destination.set(key, speedupCacheTTLMillis, createSpeedupCacheObject(value));
		}
		return (destination);
	}

	protected SpeedupCacheObject createSpeedupCacheObject(Object value) {
		SpeedupCacheObject sco = new SpeedupCacheObject();
		sco.storeTimeMillis = System.currentTimeMillis();
		sco.cachedObject = value;
		return sco;
	}

	@Override
	public Future<Object> asyncGet(String key) {
		// First try to restore the value from any cache.
		// If its found returns the value

		try {
			RestoredSpeedupCacheObject rsco = restoreObjectFromAnySpeedupCache(key);
			if ((rsco != null) && (!rsco.expired)) {
				if (rsco.fromCache == invalidationKeysCache) {
					tracker.trackInvalidationKeysCacheHit(key);
				}
				if (rsco.fromCache == objectsCache) {
					tracker.trackObjectsCacheHit(key);
				}
				if (rsco.fromCache == missesCache) {
					tracker.trackMissesCacheHit(key);
				}
				return new DummyFuture<Object>(rsco.speedupCacheObject.cachedObject);
			}
		} catch (Exception e) {
			tracker.trackException(key, e);
		}

		// Otherwise create a wrapper to capture the backend value
		Future<Object> backendFuture = backendClient.asyncGet(key);

		try {
			return createFutureBackendGetWrapper(backendFuture, key);
		} catch (Exception e) {
			tracker.trackException(key, e);
		}

		return backendFuture;
	}

	protected FutureBackendGetWrapper createFutureBackendGetWrapper(Future<Object> backendFuture, String key) {
		return new FutureBackendGetWrapper(this, backendFuture, key, protectAgainstBackendFailures, tracker);
	}

	protected RestoredSpeedupCacheObject restoreObjectFromAnySpeedupCache(String key) {
		RestoredSpeedupCacheObject rsco = null;

		if (invalidationKeysCache != null) {
			rsco = restoreObjectFromSpeedupCache(key, invalidationKeysCache, invalidationKeysExpirationMillis);
		}
		if ((rsco == null) && (objectsCache != null)) {
			rsco = restoreObjectFromSpeedupCache(key, objectsCache, objectsExpirationMillis);
		}
		if ((rsco == null) && (missesCache != null)) {
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

		rsco.fromCache = speedupCache;

		return rsco;
	}

}
