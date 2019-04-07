package ar.com.siripo.arcache.backend.speedup;

import java.io.Serializable;

public class SpeedupCacheObject implements Serializable {
	private static final long serialVersionUID = 20180407001L;

	long storeTimeMillis;
	Object cachedObject;
}
