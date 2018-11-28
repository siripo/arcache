package ar.com.siripo.arcache.backend.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.util.DummyFuture;

/**
 * In memory Backend, util for testing
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheInMemoryClient implements ArcacheBackendClient {

	private ConcurrentHashMap<String, ArcacheInMemoryObject> storage;

	public ArcacheInMemoryClient() {
		initialize();
	}

	private void initialize() {
		storage = new ConcurrentHashMap<String, ArcacheInMemoryObject>();
	}

	@Override
	public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {

		ArcacheInMemoryObject inMemoryObject = new ArcacheInMemoryObject();
		inMemoryObject.expirationTime = System.currentTimeMillis() + (ttlSeconds * 1000);
		inMemoryObject.data = serialize(value);
		storage.put(key, inMemoryObject);

		return new DummyFuture<Boolean>(true);
	}

	@Override
	public Future<Object> asyncGet(String key) {
		ArcacheInMemoryObject inMemoryObject = storage.get(key);
		Object obj = null;
		if (inMemoryObject != null) {
			if (inMemoryObject.expirationTime > System.currentTimeMillis()) {
				obj = deserialize(inMemoryObject.data);
			}
		}
		return new DummyFuture<Object>(obj);
	}

	public void clear() {
		storage.clear();
	}

	/**
	 * Get the bytes representing the given serialized object. From:
	 * {@link net.spy.memcached.transcoders.BaseSerializingTranscoder}
	 */
	protected byte[] serialize(Object o) {
		if (o == null) {
			throw new NullPointerException("Can't serialize null");
		}
		byte[] rv = null;
		ByteArrayOutputStream bos = null;
		ObjectOutputStream os = null;
		try {
			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			os.writeObject(o);
			os.close();
			bos.close();
			rv = bos.toByteArray();
		} catch (IOException e) {
			throw new IllegalArgumentException("Non-serializable object", e);
		} finally {
			try {
				os.close();
			} catch (Exception e) {
			}
			try {
				bos.close();
			} catch (Exception e) {
			}
		}
		return rv;
	}

	/**
	 * Get the object represented by the given serialized bytes. From:
	 * {@link net.spy.memcached.transcoders.BaseSerializingTranscoder}
	 */
	protected Object deserialize(byte[] in) {
		Object rv = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream is = null;
		try {
			if (in != null) {
				bis = new ByteArrayInputStream(in);
				is = new ObjectInputStream(bis);
				rv = is.readObject();
				is.close();
				bis.close();
			}
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
			try {
				bis.close();
			} catch (Exception e) {
			}
		}
		return rv;
	}

}
