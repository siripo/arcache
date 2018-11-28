package ar.com.siripo.arcache;

import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public interface ArcacheReadWriteInterface {

	/**
	 * This is the most basic get method.
	 * 
	 * It uses the defaultOperationTimeoutMillis.
	 * 
	 * In case of MISS, EXPIRED, INVALIDATED returns null.
	 * 
	 * In case of TIMEOUT throws TimeoutException.
	 * 
	 * In case of ERROR throws the Exception.
	 * 
	 * @param key
	 * @return
	 * @throws TimeoutException
	 * @throws Exception
	 */
	public Object get(String key) throws TimeoutException, Exception;

	public Object get(String key, long timeoutMillis) throws TimeoutException, Exception;

	/**
	 * @param key
	 * @return getCacheObject(key, defaultOperationTimeoutMillis);
	 */
	public CacheGetResult getCacheObject(String key);

	/**
	 * This method never throws an exception, in case of an exception it is returned
	 * inside the returned object
	 * 
	 * @param key
	 * @param timeoutMillis
	 * @return
	 */
	public CacheGetResult getCacheObject(String key, long timeoutMillis);

	/**
	 * Allow async get
	 * 
	 * @param key
	 * @return
	 */
	public Future<CacheGetResult> asyncGetCacheObject(String key);

	/**
	 * Almacena un valor, como timeout utiliza el default, en caso de falla
	 * retornará una exepcion
	 */
	public void set(String key, Object value) throws TimeoutException, Exception;

	/** Almacena un valor y define las claves de invalidacion */
	public void set(String key, Object value, String[] invalidationKeys) throws TimeoutException, Exception;

	public Future<Boolean> asyncSet(String key, Object value);

	public Future<Boolean> asyncSet(String key, Object value, String[] invalidationKeys);

}
