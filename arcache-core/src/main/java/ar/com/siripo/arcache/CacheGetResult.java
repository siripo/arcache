package ar.com.siripo.arcache;

/**
 * Representa el resultado de una operacion de Get
 * 
 * @author Mariano Santamarina
 *
 */
public class CacheGetResult {

	/** The user stored value */
	public Object value;

	public Exception cause;

	enum Type {
		HIT, // When the value is found and it is not expired
		MISS, // When the value not found or it is hard invalidated
		EXPIRED, // When the value is expired
		INVALIDATED, // When the value is soft invalidated
		TIMEOUT, // When the operation timeouts
		ERROR // When an error other than timeout ocurrs
	}

	public Type type;

	public String invalidationKeys[]; // If the cached value has invalidation Keys, are set here

	public String invalidatedByKey; // If it is INVALIDATED here is set the key that invalidates

	public long storeTimestamp; // Timestamp in seconds where the value was stored

	public boolean isHit() {
		return type == Type.HIT;
	}

	protected CacheGetResult(Type type) {
		this.type = type;
	}

	protected CacheGetResult(Type type, Exception cause) {
		this(type);
		this.cause = cause;
	}

}
