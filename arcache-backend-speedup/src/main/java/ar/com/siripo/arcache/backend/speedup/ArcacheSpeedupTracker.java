package ar.com.siripo.arcache.backend.speedup;

/**
 * This interface allow tracking of speedup
 * 
 * @author Mariano Santamarina
 *
 */
public interface ArcacheSpeedupTracker {
	public void trackException(String key, Exception e);

	public void trackBackendGetFailureRecovered(String key, Exception e);

	public void trackInvalidationKeysCacheHit(String key);

	public void trackObjectsCacheHit(String key);

	public void trackMissesCacheHit(String key);

	public void trackInvalidationKeysCacheMiss(String key);

	public void trackObjectsCacheMiss(String key);

	public void trackMissesCacheMiss(String key);

}
