package ar.com.siripo.arcache.backend.speedup;

import java.util.concurrent.Future;

import ar.com.siripo.arcache.CacheInvalidationObjectType;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;

/**
 * This is a local memory proxy of another backend. The purpose is speed up the
 * access to a slow backend.
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheSpeedupClient implements ArcacheBackendClient {

	protected ArcacheBackendClient backendClient;

	protected int invalidationKeysCacheSize = 0;
	protected int objectsCacheSize = 0;

	protected long invalidationKeysExpirationMillis = 0;
	protected long objectsExpirationMillis = 0;
	protected boolean speedupMisses = true;

	protected ArcacheInMemoryClient invalidationKeysCache = null;
	protected ArcacheInMemoryClient objectsCache = null;
	protected boolean initialized = false;

	public ArcacheSpeedupClient() {
	}

	/** Set the inner backend client */
	public void setBackendClient(ArcacheBackendClient backendClient) {
		this.backendClient = backendClient;
	}

	public ArcacheBackendClient getBackendClient() {
		return backendClient;
	}

	/** Set the size of invalidation keys in max number of elements */
	public void setInvalidationKeysCacheSize(int cachesize) {
		this.invalidationKeysCacheSize = cachesize;
	}

	public int getInvalidationKeysCacheSize() {
		return invalidationKeysCacheSize;
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

		if ((objectsCache == null) && (invalidationKeysCache == null)) {
			throw new IllegalArgumentException("it was not configured neither invalidationKeys nor objects policy");
		}

		if (backendClient == null) {
			throw new IllegalArgumentException("Backend Client was not configured");
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
	}

	@Override
	public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
		try {
			if (value instanceof CacheInvalidationObjectType) {
				if (invalidationKeysCache != null) {
					invalidationKeysCache.set(key, ttlSeconds, createSpeedupCacheObject(value));
				}
			} else {
				if (objectsCache != null) {
					objectsCache.set(key, ttlSeconds, createSpeedupCacheObject(value));
				}
			}
		} catch (Exception e) {
			// This catch is here because the speedup is a wish functionality, if it does
			// not work must do not break the inner backend access
		}
		return backendClient.asyncSet(key, ttlSeconds, value);
	}

	@Override
	public Future<Object> asyncGet(String key) {
		// If its configured invalidation cache, try to restore from that cache.
		// If it exists, check expiration against proba function.
		// If its OK. returns a dummy future using the returned value.

		// Oherwise use objects cache

		// otherwise use the backend, but wrapper arround with a getter that in case of
		// HIT stores locally the value, and in miss also. and in error or timeout
		// returns the expired local value if any

		return null;
	}

	protected SpeedupCacheObject createSpeedupCacheObject(Object value) {
		SpeedupCacheObject sco = new SpeedupCacheObject();
		sco.storeTimeMillis = System.currentTimeMillis();
		sco.cachedObject = value;
		return sco;
	}

}
