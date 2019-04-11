package ar.com.siripo.arcache;

/**
 * Representa el resultado de una operacion de Get
 * 
 * @author Mariano Santamarina
 *
 */
public class CacheGetResult {

	public enum Type {
		HIT, // When the value is found and it is not expired
		MISS, // When the value not found or it is hard invalidated
		EXPIRED, // When the value is expired
		INVALIDATED, // When the value is soft invalidated
		TIMEOUT, // When the operation timeouts
		ERROR // When an error other than timeout ocurrs
	}

	/** The user stored value */
	protected Object value;

	protected Exception errorCause;

	protected Type type;

	protected String invalidationKeys[]; // If the cached value has invalidation Keys, are set here

	protected String invalidatedKey; // If it is INVALIDATED here is set the key that invalidates

	protected long storeTimestampMillis; // Timestamp in milliseconds where the value was stored

	public boolean isHit() {
		return type == Type.HIT;
	}

	public boolean isMiss() {
		return type == Type.MISS;
	}

	public boolean isExpired() {
		return type == Type.EXPIRED;
	}

	public boolean isInvalidated() {
		return type == Type.INVALIDATED;
	}

	public boolean isHitOrExpired() {
		return type == Type.HIT || type == Type.EXPIRED;
	}

	public boolean isHitExpiredOrInvalidated() {
		return type == Type.HIT || type == Type.EXPIRED || type == Type.INVALIDATED;
	}

	public boolean isAnyTypeOfError() {
		return type == Type.TIMEOUT || type == Type.ERROR;
	}

	protected CacheGetResult(Type type) {
		this.type = type;
	}

	protected CacheGetResult(Type type, Exception cause) {
		this(type);
		this.errorCause = cause;
	}

	public Object getValue() {
		return value;
	}

	public Exception getErrorCause() {
		return errorCause;
	}

	public Type getType() {
		return type;
	}

	public String[] getInvalidationKeys() {
		return invalidationKeys;
	}

	public String getInvalidatedKey() {
		return invalidatedKey;
	}

	public long getStoreTimestampMillis() {
		return storeTimestampMillis;
	}

	public static class Builder {
		private CacheGetResult build;

		public Builder(Type type) {
			build = new CacheGetResult(type);
		}

		public Builder(CacheGetResult ref) {
			this(ref.type);
			build.errorCause = ref.errorCause;
			build.invalidatedKey = ref.invalidatedKey;
			build.invalidationKeys = ref.invalidationKeys;
			build.storeTimestampMillis = ref.storeTimestampMillis;
			build.value = ref.value;
		}

		public Builder withType(Type type) {
			build.type = type;
			return this;
		}

		public Builder withValue(Object value) {
			build.value = value;
			return this;
		}

		public Builder withErrorCause(Exception errorCause) {
			build.errorCause = errorCause;
			return this;
		}

		public Builder withInvalidationKeys(String invalidationKeys[]) {
			build.invalidationKeys = invalidationKeys;
			return this;
		}

		public Builder withInvalidatedKey(String invalidatedKey) {
			build.invalidatedKey = invalidatedKey;
			return this;
		}

		public Builder withStoreTimestampMillis(long storeTimestampMillis) {
			build.storeTimestampMillis = storeTimestampMillis;
			return this;
		}

		public CacheGetResult build() {
			return build;
		}
	}

	public static class ErrorBuilder extends Builder {

		public ErrorBuilder() {
			super(Type.ERROR);
		}

		public ErrorBuilder(Exception errorCause) {
			this();
			withErrorCause(errorCause);
		}
	}
}
