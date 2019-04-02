package ar.com.siripo.arcache;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public interface ArcacheConfigurationSetInterface {

	/**
	 * 
	 * @param timeoutMillis timeoutMillis the default timeout to be used in backend
	 *                      operations
	 */
	public void setDefaultOperationTimeout(long timeoutMillis);

	/**
	 * Error total en la medicion de tiempos expresado en segundos debe ser 1 +
	 * maxClockOffset + maxKeyCreationTime maxClockOffset (diferencia maxima entre 2
	 * relojes de la infraestructura involucrada) maxKeyCreationTime (Tiempo (95pt)
	 * que demanda generar un valor a ser almacenado)
	 * 
	 * @param errorSecs the time measurament error in seconds
	 */
	public void setTimeMeasurementError(long errorSecs);

	/**
	 * set the default invalidation window, this is the ammount of time to do
	 * probabilistic invalidation.
	 * 
	 * @param windowSecs invalidation window in seconds
	 */
	public void setDefaultInvalidationWindow(long windowSecs);

	/**
	 * 
	 * @param hardInvalidation default type of invalidation to be used
	 */
	public void setDefaultHardInvalidation(boolean hardInvalidation);

	/**
	 * Permite configurar un namsepace default para todas las keys De esta forma se
	 * puede evitar la colision de keys con otro servicio
	 * 
	 * @param namespace ...
	 */
	public void setKeyNamespace(String namespace);

	/**
	 * Permite configurar un delimitador default para las keys el mismo sera
	 * utilizado par separar las keys de invalidacion y el namespace
	 * 
	 * @param keyDelimiter ...
	 */
	public void setKeyDelimiter(String keyDelimiter);

	/**
	 * Sets the default value to be used to consider a cached object expired
	 * 
	 * @param expirationTimeSecs ...
	 */
	public void setDefaultExpirationTime(long expirationTimeSecs);

	/**
	 * Sets the default value to be used to set the lifetime of the object at
	 * backend level. When this time is reached the object is expected to be removed
	 * 
	 * @param removeTimeSecs ...
	 */
	public void setDefaultStoredObjectRemovalTime(long removeTimeSecs);

	public void setBackendClient(ArcacheBackendClient backendClient);

	/**
	 * Sets the function to be used to compute the probability of a key has expired
	 * 
	 * @param expirationProbabilityFunction
	 */
	public void setExpirationProbabilityFunction(ProbabilityFunction expirationProbabilityFunction);

	/**
	 * Sets the function to be used to compute the probability of a key has
	 * invalidated
	 * 
	 * @param invalidationProbabilityFunction
	 */
	public void setInvalidationProbabilityFunction(ProbabilityFunction invalidationProbabilityFunction);

}
