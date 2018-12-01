package ar.com.siripo.arcache;

import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public interface ArcacheInvalidationInterface {

	/**
	 * Invalida una key, utilizando una invalidacion soft o hard segun se haya
	 * configurado, utilizando el invalidationWindow default
	 */
	public void invalidateKey(String key) throws TimeoutException, Exception;

	/**
	 * Invalida una key, utilizando una invalidacion soft o hard segun se haya
	 * configurado
	 */
	public void invalidateKey(String key, long invalidationWindowSecs) throws TimeoutException, Exception;

	/**
	 * Invalidates a key using the default invalidationWindow
	 * 
	 * @param key
	 * @param hardInvalidation
	 * @throws TimeoutException when the operation exceeds the
	 *                          DefaultOperationTimeout
	 * @throws Exception        when there is a problem with the backend operation
	 */
	public void invalidateKey(String key, boolean hardInvalidation) throws TimeoutException, Exception;

	public void invalidateKey(String key, boolean hardInvalidation, long invalidationWindowSecs)
			throws TimeoutException, Exception;

	public Future<Boolean> asyncInvalidateKey(String key, boolean hardInvalidation, long invalidationWindowSecs);

}
