package ar.com.siripo.arcache.backend.speedup;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public interface ArcacheSpeedupConfigurationSetInterface {

	/** Set the inner backend client */
	public void setBackendClient(ArcacheBackendClient backendClient);

	/** Set the maximum size of invalidationKeys Cache in number of elements */
	public void setInvalidationKeysCacheSize(int cacheSize);

	/** Set the maximum size of objects Cache in number of elements */
	public void setObjectsCacheSize(int cacheSize);

	/** Set the maximum size of misses Cache in number of elements */
	public void setMissesCacheSize(int cacheSize);

	/** Set the expiration of the stored values in milliseconds */
	public void setInvalidationKeysExpirationMillis(long expirationMillis);

	/** Set the expiration of the stored values in milliseconds */
	public void setObjectsExpirationMillis(long expirationMillis);

	/** Set the expiration of the stored values in milliseconds */
	public void setMissesExpirationMillis(long expirationMillis);

	/**
	 * Allow some protection against backend failures. For example if a get backend
	 * operation fails with an TimeoutException, and the local cache has the value
	 * expired, then this is returned
	 */
	public void setProtectAgainstBackendFailures(boolean protect);

	/**
	 * Set the speedup cache ttl in seconds, this value must be restricted by the
	 * cache size, the ttl is allowed to make a hard expiration if its needed. By
	 * default this value is a year
	 */
	public void setSpeedupCacheTTLSeconds(int ttlSeconds);

	/** Set the probability function to be used in expiration evaluation */
	public void setExpirationProbabilityFunction(ProbabilityFunction probabilityFunction);

}
