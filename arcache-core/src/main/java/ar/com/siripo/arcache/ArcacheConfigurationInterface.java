package ar.com.siripo.arcache;

public interface ArcacheConfigurationInterface {
	
	/** Permite configurar el timeout default de la conexion */
	public void setDefaultOperationTimeout(long timeoutMSecs);
	
	/** Error total en la medicion de tiempos expresado en segundos
	 * debe ser 1 + maxClockOffset + maxKeyCreationTime
	 * maxClockOffset (diferencia maxima entre 2 relojes de la infraestructura involucrada)
	 * maxKeyCreationTime (Tiempo (95pt) que demanda generar un valor a ser almacenado) 
	 * @param error_s
	 */
	public void setTimeMeasurementError(long errorSecs);
	
	/** Ventana de invalidacion default */
	public void setDefaultInvalidationWindow(long windowSecs);
	
	
	/** Permite configurar un namsepace default para todas las keys
	 * De esta forma se puede evitar la colision de keys con otro servicio
	 * @param namespace
	 */
	public void setKeyNamespace(String namespace);
	
	/** Permite configurar un delimitador default para las keys
	 * el mismo sera utilizado par separar las keys de invalidacion y el namespace
	 * @param keyDelimiter
	 */
	public void setKeyDelimiter(String keyDelimiter);
	
}
