package ar.com.siripo.arcache.backend.speedup;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public interface ArcacheSpeedupConfigurationGetInterface {

	/** Get the inner backend client */
	public ArcacheBackendClient getBackendClient();

	/** Get the maximum size of invalidationKeys Cache in number of elements */
	public int getInvalidationKeysCacheSize();

	/** Get the maximum size of objects Cache in number of elements */
	public int getObjectsCacheSize();

	/** Get the maximum size of misses Cache in number of elements */
	public int getMissesCacheSize();

	/** Get the expiration of the stored values in milliseconds */
	public long getInvalidationKeysExpirationMillis();

	/** Get the expiration of the stored values in milliseconds */
	public long getObjectsExpirationMillis();

	/** Get the expiration of the stored values in milliseconds */
	public long getMissesExpirationMillis();

	/**
	 * Allow some protection against backend failures. For example if a get backend
	 * operation fails with an TimeoutException, and the local cache has the value
	 * expired, then this is returned
	 */
	public boolean getProtectAgainstBackendFailures();

	/**
	 * Get the speedup cache ttl in milli seconds, this value must be restricted by
	 * the cache size, the ttl is allowed to make a hard expiration if its needed.
	 * By default this value is a year
	 */
	public long getSpeedupCacheTTLMillis();

	/** Get the probability function to be used in expiration evaluation */
	public ProbabilityFunction getExpirationProbabilityFunction();

	/** Get a tracker to keep control of speedup performance and malfunctions */
	public ArcacheSpeedupTracker getTracker();
}
