package ar.com.siripo.arcache;

import java.util.concurrent.TimeoutException;

public class ArcacheClient implements ArcacheClientInterface {
	
	protected long defaultOperationTimeoutMS=50000;
	protected long timeMeasurementErrorSecs=2;
	protected long defaultInvalidationWindowSecs=5;
	protected String keyNamespace="";
	protected String keyDelimiter="|";

	@Override
	public void setDefaultOperationTimeout(long timeoutMSecs) {
		defaultOperationTimeoutMS=timeoutMSecs;
	}

	@Override
	public void setTimeMeasurementError(long errorSecs) {
		timeMeasurementErrorSecs=errorSecs;
	}

	@Override
	public void setDefaultInvalidationWindow(long windowSecs) {
		defaultInvalidationWindowSecs=windowSecs;
	}

	@Override
	public void setKeyNamespace(String namespace) {
		keyNamespace=namespace;
	}

	@Override
	public void setKeyDelimiter(String keyDelimiter) {
		this.keyDelimiter=keyDelimiter;
	}

	@Override
	public Object get(String key) throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheGetResult getCacheObject(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CacheGetResult getCacheObject(String key, long timeoutMSecs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(String key, Object value) throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void set(String key, Object value, String[] invalidationSets) throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidateKey(String key) throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidateKey(String key, long invalidationWindowSecs) throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidateKey(String key, boolean hardInvalidation) throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidateKey(String key, boolean hardInvalidation, long invalidationWindowSecs)
			throws TimeoutException, Exception {
		// TODO Auto-generated method stub
		
	}

}
