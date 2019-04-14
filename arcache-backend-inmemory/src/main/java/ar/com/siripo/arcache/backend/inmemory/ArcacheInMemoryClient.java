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

	/**
	 * When cache is isolated, all the stored objects are serialized to be stored,
	 * And deserialized to be retrieved from cache
	 */
	protected boolean cacheIsolation;

	public ArcacheInMemoryClient() {
		this(1000, false);
	}

	public ArcacheInMemoryClient(int maxSize) {
		this(maxSize, false);
	}

	public ArcacheInMemoryClient(int maxSize, boolean cacheIsolation) {
		this.lruMaxSize = maxSize;
		this.cacheIsolation = cacheIsolation;
		initialize();
	}

	private void initialize() {
		storage = new LRUMap<String, MemoryObject>(lruMaxSize);
		if (cacheIsolation) {
			objectSerializer = new ObjectSerializer();
		}
	}

	@Override
	public Future<Boolean> asyncSet(String key, long ttlMillis, Object value) {
		return new DummyFuture<Boolean>(set(key, ttlMillis, value));
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
			if (inMemoryObject.expirationTimeMillis > System.currentTimeMillis()) {
				if (cacheIsolation) {
					obj = objectSerializer.deserialize((byte[]) inMemoryObject.data);
				} else {
					obj = inMemoryObject.data;
				}
			}
		}
		return obj;
	}

	public boolean set(String key, long ttlMillis, Object value) {

		MemoryObject inMemoryObject = new MemoryObject();
		inMemoryObject.expirationTimeMillis = System.currentTimeMillis() + ttlMillis;
		if (cacheIsolation) {
			inMemoryObject.data = objectSerializer.serializeToByteArray(value);
		} else {
			inMemoryObject.data = value;
		}

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
