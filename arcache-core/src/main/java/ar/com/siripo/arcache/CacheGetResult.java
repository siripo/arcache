package ar.com.siripo.arcache;

/**
 * Representa el resultado de una operacion de Get
 * @author Mariano Santamarina
 *
 */
public class CacheGetResult {
	
	/** El valor retornado */
	public Object value;
	
	
	public Exception cause;
	
	enum ResultType {
	       HIT,MISS,EXPIRED,INVALIDATED,TIMEOUT,ERROR
	}
	
	public ResultType result;
	
	public boolean isHit() {
		return result==ResultType.HIT;
	}
	

}
