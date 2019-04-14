package ar.com.siripo.arcache.backend.speedup;

public class ArcacheSpeedupBasicTracker implements ArcacheSpeedupTracker {

	@Override
	public void trackException(String key, Exception e) {
	}

	@Override
	public void trackBackendGetFailureRecovered(String key, Exception e) {
	}

	@Override
	public void trackInvalidationKeysCacheHit(String key) {
	}

	@Override
	public void trackObjectsCacheHit(String key) {
	}

	@Override
	public void trackMissesCacheHit(String key) {
	}

	@Override
	public void trackInvalidationKeysCacheMiss(String key) {
	}

	@Override
	public void trackObjectsCacheMiss(String key) {
	}

	@Override
	public void trackMissesCacheMiss(String key) {
	}

}
