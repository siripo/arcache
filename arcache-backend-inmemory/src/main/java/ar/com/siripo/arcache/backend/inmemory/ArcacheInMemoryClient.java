package ar.com.siripo.arcache.backend.inmemory;

import java.util.concurrent.Future;

import org.apache.commons.collections4.map.LRUMap;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.util.DummyFuture;
import ar.com.siripo.arcache.util.ObjectSerializer;

/**
 * In memory Backend
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheInMemoryClient implements ArcacheBackendClient {

	protected LRUMap<String, MemoryObject> storage;
	protected ObjectSerializer objectSerializer;

	protected int lruMaxSize = 1000;

	public ArcacheInMemoryClient() {
		this(1000);
	}

	public ArcacheInMemoryClient(int maxSize) {
		this.lruMaxSize = maxSize;
		initialize();
	}

	private void initialize() {
		storage = new LRUMap<String, MemoryObject>(lruMaxSize);
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
		MemoryObject inMemoryObject;

		synchronized (storage) {
			inMemoryObject = storage.get(key);
		}

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

		synchronized (storage) {
			storage.put(key, inMemoryObject);
		}

		return true;
	}

	public void remove(String key) {
		synchronized (storage) {
			storage.remove(key);
		}
	}

	public void clear() {
		synchronized (storage) {
			storage.clear();
		}
	}

}
