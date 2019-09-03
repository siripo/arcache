package ar.com.siripo.arcache;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public interface ArcacheConfigurationSetInterface {

	/**
	 * 
	 * @param timeoutMillis timeoutMillis the default timeout to be used in backend
	 *                      operations
	 */
	public void setDefaultOperationTimeoutMillis(long timeoutMillis);

	/**
	 * In heavy loaded systems when CPU is at top, and with too many threads running
	 * concurrently, the time measurements are imperfect, because the time is spend
	 * in another thread and not in the IO operation. In that cases all the time on
	 * a Get operation is spent in the first get of the object key. No timeout is
	 * achieved but the seen time of the get operation is much higher than timeout
	 * (a signal of overload or a backend bug). In this scenario if the object has
	 * invalidation keys is a dilemma, 1. timeout the all the operation, possibly
	 * adding more load to the system or 2. relax the timeout and get the
	 * invalidation objects
	 * 
	 * @param relaxOperationTimeoutInHeavyLoadSystem (default true)
	 */
	public void setRelaxOperationTimeoutInHeavyLoadSystem(boolean relaxOperationTimeoutInHeavyLoadSystem);

	/**
	 * Error total en la medicion de tiempos expresado en mili segundos debe ser
	 * 1000 + maxClockOffset + maxKeyCreationTime maxClockOffset (diferencia maxima
	 * entre 2 relojes de la infraestructura involucrada) maxKeyCreationTime (Tiempo
	 * (95pt) que demanda generar un valor a ser almacenado)
	 * 
	 * @param errorMillis the time measurament error in milliseconds
	 */
	public void setTimeMeasurementErrorMillis(long errorMillis);

	/**
	 * set the default invalidation window, this is the ammount of time to do
	 * probabilistic invalidation.
	 * 
	 * @param windowMillis invalidation window in milliseconds
	 */
	public void setDefaultInvalidationWindowMillis(long windowMillis);

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
	 * @param expirationTimeMillis ...
	 */
	public void setDefaultExpirationTimeMillis(long expirationTimeMillis);

	/**
	 * Sets the default value to be used to set the lifetime of the object at
	 * backend level. When this time is reached the object is expected to be removed
	 * 
	 * @param removeTimeMillis ...
	 */
	public void setDefaultStoredObjectRemovalTimeMillis(long removeTimeMillis);

	/**
	 * Sets the default backend client to store objects and invalidationkeys
	 * 
	 * @param backendClient
	 */
	public void setBackendClient(ArcacheBackendClient backendClient);

	/**
	 * Set the invalidation backend client to store invalidationkeys If you null
	 * this, then the defaultBackendClient is used.
	 * 
	 * @param invalidationBackendClient
	 */
	public void setInvalidationBackendClient(ArcacheBackendClient invalidationBackendClient);

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
