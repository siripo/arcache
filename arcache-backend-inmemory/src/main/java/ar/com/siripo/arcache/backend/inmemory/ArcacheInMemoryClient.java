package ar.com.siripo.arcache.backend.inmemory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.util.DummyFuture;
import ar.com.siripo.arcache.util.ObjectSerializer;

/**
 * In memory Backend, util ONLY for testing. Because does not have eviction and
 * memory control
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheInMemoryClient implements ArcacheBackendClient {

	protected ConcurrentHashMap<String, MemoryObject> storage;
	protected ObjectSerializer objectSerializer;

	public ArcacheInMemoryClient() {
		initialize();
	}

	private void initialize() {
		storage = new ConcurrentHashMap<String, MemoryObject>();
		objectSerializer = new ObjectSerializer();
	}

	@Override
	public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {
		return new DummyFuture<Boolean>(set(key, ttlSeconds, value));
	}

	@Override
	public Future<Object> asyncGet(String key) {
		return new DummyFuture<Object>(get(key));
	}

	public Object get(String key) {
		MemoryObject inMemoryObject = storage.get(key);
		Object obj = null;
		if (inMemoryObject != null) {
			if (inMemoryObject.expirationTime > System.currentTimeMillis()) {
				obj = objectSerializer.deserialize(inMemoryObject.data);
			}
		}
		return obj;
	}

	public boolean set(String key, int ttlSeconds, Object value) {

		MemoryObject inMemoryObject = new MemoryObject();
		inMemoryObject.expirationTime = System.currentTimeMillis() + (ttlSeconds * 1000);
		inMemoryObject.data = objectSerializer.serializeToByteArray(value);
		storage.put(key, inMemoryObject);

		return true;
	}

	public void clear() {
		storage.clear();
	}

}
