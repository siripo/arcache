package ar.com.siripo.arcache.backend.speedup;

import ar.com.siripo.arcache.backend.inmemory.ArcacheInMemoryClient;

public class RestoredSpeedupCacheObject {

	protected SpeedupCacheObject speedupCacheObject;
	protected boolean expired;
	protected ArcacheInMemoryClient fromCache;
}
