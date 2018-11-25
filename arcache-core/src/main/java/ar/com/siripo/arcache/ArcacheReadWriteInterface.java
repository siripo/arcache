package ar.com.siripo.arcache;

import java.util.concurrent.TimeoutException;

public interface ArcacheReadWriteInterface {
	/**
	 * Metodo de acceso mas simple y básico.
	 * Retorna null ante un miss, expired, invalidated
	 * 
	 * @param key
	 * @return
	 * @throws TimeoutException
	 * @throws Exception
	 */
	public Object get(String key) throws TimeoutException,Exception;
	
	
	public CacheGetResult getCacheObject(String key);
	public CacheGetResult getCacheObject(String key,long timeoutMSecs);
	
	/**
	 * Permite leer de forma asincrona un objeto de la cache.
	 * @param key
	 * @return
	 */
	//TODO implementar métodos de acceso asincronos
	//public Future<CacheGetResult> asyncGetCacheObject(String key);
	
	
	
	/** Almacena un valor, como timeout utiliza el default, en caso de falla retornará una exepcion */
	public void set(String key,Object value) throws TimeoutException,Exception;
	
	/** Almacena un valor y define las claves de invalidacion */
	public void set(String key,Object value,String[] invalidationSets) throws TimeoutException,Exception;
	
}
