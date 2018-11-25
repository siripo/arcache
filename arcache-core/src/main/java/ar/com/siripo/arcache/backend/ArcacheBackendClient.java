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
	 * @param ttlSeconds
	 * @param value
	 * @return
	 */
	public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value);

	public Future<Object> asyncGet(String key);

}
