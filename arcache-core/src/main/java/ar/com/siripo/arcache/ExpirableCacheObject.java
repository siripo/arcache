package ar.com.siripo.arcache;

/**
 *	Es el objeto que será almacenado en el backend para representar valores convencionales.
 * 
 * @author Mariano Santamarina
 *
 */
public class ExpirableCacheObject {
	/** The application domain value to be remembered and restored */
	public Object value;
	
	/** Unix timestamp del momento en que es almacenado este valor (obviamente en segundos) */
	public long timestamp;
	
	/** TTL total de la key, a partir de ese momento se considera Expirado */
	public long maxTtlSecs;
	
	/** ttl minimo, a partir de ese momento inicia la posiblidad de expiracion */
	public long minTtlSecs;
	
	/** lista de conjuntos a los cuales pertenece esta clave, los cuales permiten expirar la misma */
	public String[] invalidationKeys;
	
}
