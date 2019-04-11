package ar.com.siripo.arcache;

import java.io.Serializable;

/**
 * This object is the one that will be stored in the backend to contain the
 * domain objects to be cached. It also has the necessary attributes to
 * determine the expiration and invalidation.
 * 
 * @author Mariano Santamarina
 *
 */
public class ExpirableCacheObject implements Serializable {

	private static final long serialVersionUID = 20180410001L;

	/** The application domain value to be remembered and restored */
	public Object value;

	/** Unix timestamp (in milliseconds) of the moment where the value was stored */
	public long timestampMillis;

	/**
	 * Expiration TTL in milliseconds, after that the key must be considered expired
	 */
	public long expirationTTLMillis;

	/**
	 * list of sets to which this key belongs, which allow the invalidation of
	 * groups
	 */
	public String[] invalidationKeys;

}
