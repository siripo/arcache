package ar.com.siripo.arcache.backend.memcached;

import java.util.concurrent.Future;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import net.spy.memcached.MemcachedClientIF;

/**
 * Adaptador de la interfaz a memcached
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheMemcachedClient implements ArcacheBackendClient {

	private MemcachedClientIF memcachedClient;

	public ArcacheMemcachedClient(MemcachedClientIF memcachedClient) {
		this.memcachedClient = memcachedClient;
	}

	@Override
	public Future<Boolean> asyncSet(String key, long ttlMillis, Object value) {
		return memcachedClient.set(key, (int) ((ttlMillis + 999) / 1000), value);
	}

	@Override
	public Future<Object> asyncGet(String key) {
		return memcachedClient.asyncGet(key);
	}

}
