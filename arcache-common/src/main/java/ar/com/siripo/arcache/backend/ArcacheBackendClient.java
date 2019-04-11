package ar.com.siripo.arcache.backend;

import java.util.concurrent.Future;

public interface ArcacheBackendClient {

	/**
	 * The expected behavior is that asyncSet returns a Future.
	 * 
	 * If the value can not be serialized IllegalArgumentException is thrown.
	 * 
	 * If the key is invalid IllegalArgumentException is thrown
	 * 
	 * @param key
	 * @param ttlMillis time to live in milli seconds of the stored object
	 * @param value
	 * @return
	 */
	public Future<Boolean> asyncSet(String key, long ttlMillis, Object value);

	public Future<Object> asyncGet(String key);

}
