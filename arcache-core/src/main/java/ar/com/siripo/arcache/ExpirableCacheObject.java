package ar.com.siripo.arcache;

import java.io.Serializable;

/**
 * Es el objeto que será almacenado en el backend para representar valores
 * convencionales.
 * 
 * @author Mariano Santamarina
 *
 */
public class ExpirableCacheObject implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The application domain value to be remembered and restored */
	public Object value;

	/**
	 * Unix timestamp (in seconds) of the moment where the value is stored
	 */
	public long timestamp;

	/** TTL total de la key, a partir de ese momento se considera Expirado */
	public long maxTTLSecs;

	/** ttl minimo, a partir de ese momento inicia la posiblidad de expiracion */
	public long minTTLSecs;

	/**
	 * lista de conjuntos a los cuales pertenece esta clave, los cuales permiten
	 * expirar la misma
	 */
	public String[] invalidationKeys;

}
