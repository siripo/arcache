package ar.com.siripo.arcache;

import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.com.siripo.arcache.CacheGetResult.Type;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.AdjustedExponentialProbabilityFunction;
import ar.com.siripo.arcache.math.ProbabilityFunction;
import ar.com.siripo.arcache.util.DummyFuture;

public class ArcacheClient implements ArcacheClientInterface, BackendKeyBuilder {

	protected long defaultOperationTimeoutMillis = 500;
	protected boolean relaxOperationTimeoutInHeavyLoadSystem = true;
	protected long timeMeasurementErrorMillis = 3000;
	protected long defaultInvalidationWindowMillis = 5000;
	protected boolean defaultHardInvalidation = true;
	protected String keyNamespace = null;
	protected String keyDelimiter = "|";
	protected String invalidationKeyPrefix = "InvKey";
	protected long defaultExpirationTimeMillis = 3600000;
	protected long defaultStoredObjectRemovalTimeMillis = 86400000;
	protected ProbabilityFunction expirationProbabilityFunction;
	protected ProbabilityFunction invalidationProbabilityFunction;

	protected ArcacheBackendClient backendClient;
	/**
	 * The userConfiguredInvalidationBackendClient is what the user sets, the
	 * effective backend client used is at effectiveInvalidationBackendClient
	 */
	protected ArcacheBackendClient userConfiguredInvalidationBackendClient;
	protected ArcacheBackendClient effectiveInvalidationBackendClient;

	protected Random randomGenerator;

	public ArcacheClient() {
		randomGenerator = new Random();
		expirationProbabilityFunction = new AdjustedExponentialProbabilityFunction(0.5, 11);
		invalidationProbabilityFunction = new AdjustedExponentialProbabilityFunction(0, 11);
	}

	protected ArcacheClient(ArcacheBackendClient backendClient) {
		this();
		setBackendClient(backendClient);
	}

	@Override
	public void setBackendClient(ArcacheBackendClient backendClient) {
		this.backendClient = backendClient;
		if (this.userConfiguredInvalidationBackendClient == null) {
			this.effectiveInvalidationBackendClient = backendClient;
		}
	}

	@Override
	public ArcacheBackendClient getBackendClient() {
		return backendClient;
	}

	@Override
	public void setInvalidationBackendClient(ArcacheBackendClient invalidationBackendClient) {
		this.userConfiguredInvalidationBackendClient = invalidationBackendClient;
		if (this.userConfiguredInvalidationBackendClient == null) {
			this.effectiveInvalidationBackendClient = this.backendClient;
		} else {
			this.effectiveInvalidationBackendClient = this.userConfiguredInvalidationBackendClient;
		}
	}

	@Override
	public ArcacheBackendClient getInvalidationBackendClient() {
		return userConfiguredInvalidationBackendClient;
	}

	@Override
	public void setDefaultOperationTimeoutMillis(final long timeoutMillis) {
		if (timeoutMillis <= 0) {
			throw new IllegalArgumentException();
		}
		defaultOperationTimeoutMillis = timeoutMillis;
	}

	@Override
	public long getDefaultOperationTimeoutMillis() {
		return defaultOperationTimeoutMillis;
	}

	@Override
	public void setRelaxOperationTimeoutInHeavyLoadSystem(boolean relaxOperationTimeoutInHeavyLoadSystem) {
		this.relaxOperationTimeoutInHeavyLoadSystem = relaxOperationTimeoutInHeavyLoadSystem;
	}

	@Override
	public boolean getRelaxOperationTimeoutInHeavyLoadSystem() {
		return relaxOperationTimeoutInHeavyLoadSystem;
	}

	@Override
	public void setTimeMeasurementErrorMillis(final long errorMillis) {
		if (errorMillis < 0) {
			throw new IllegalArgumentException();
		}
		timeMeasurementErrorMillis = errorMillis;
	}

	@Override
	public long getTimeMeasurementErrorMillis() {
		return timeMeasurementErrorMillis;
	}

	@Override
	public void setDefaultInvalidationWindowMillis(final long windowMillis) {
		if (windowMillis < 0) {
			throw new IllegalArgumentException();
		}
		defaultInvalidationWindowMillis = windowMillis;
	}

	@Override
	public long getDefaultInvalidationWindowMillis() {
		return defaultInvalidationWindowMillis;
	}

	@Override
	public void setDefaultHardInvalidation(final boolean hardInvalidation) {
		defaultHardInvalidation = hardInvalidation;
	}

	@Override
	public boolean getDefaultHardInvalidation() {
		return defaultHardInvalidation;
	}

	@Override
	public void setKeyNamespace(final String namespace) {
		keyNamespace = namespace;
		if ("".equals(keyNamespace)) {
			keyNamespace = null;
		}
	}

	@Override
	public String getKeyNamespace() {
		return keyNamespace;
	}

	@Override
	public void setKeyDelimiter(final String keyDelimiter) {
		if ((keyDelimiter == null) || (keyDelimiter.equals(""))) {
			throw new IllegalArgumentException("The key delimiter must be a non empty String");
		}
		this.keyDelimiter = keyDelimiter;
	}

	@Override
	public String getKeyDelimiter() {
		return this.keyDelimiter;
	}

	@Override
	public void setDefaultExpirationTimeMillis(final long expirationTimeMillis) {
		if (expirationTimeMillis <= 0) {
			throw new IllegalArgumentException();
		}
		this.defaultExpirationTimeMillis = expirationTimeMillis;
	}

	@Override
	public long getDefaultExpirationTimeMillis() {
		return this.defaultExpirationTimeMillis;
	}

	@Override
	public void setDefaultStoredObjectRemovalTimeMillis(final long removeTimeMillis) {
		if (removeTimeMillis <= 0) {
			throw new IllegalArgumentException();
		}
		this.defaultStoredObjectRemovalTimeMillis = removeTimeMillis;
	}

	@Override
	public long getDefaultStoredObjectRemovalTimeMillis() {
		return defaultStoredObjectRemovalTimeMillis;
	}

	public void setExpirationProbabilityFunction(final ProbabilityFunction expirationProbabilityFunction) {
		this.expirationProbabilityFunction = expirationProbabilityFunction;
	}

	public ProbabilityFunction getExpirationProbabilityFunction() {
		return this.expirationProbabilityFunction;
	}

	public void setInvalidationProbabilityFunction(final ProbabilityFunction invalidationProbabilityFunction) {
		this.invalidationProbabilityFunction = invalidationProbabilityFunction;
	}

	public ProbabilityFunction getInvalidationProbabilityFunction() {
		return this.invalidationProbabilityFunction;
	}

	@Override
	public Object get(final String key) throws TimeoutException, Exception {
		return get(key, defaultOperationTimeoutMillis);
	}

	@Override
	public Object get(final String key, final long timeoutMillis) throws TimeoutException, Exception {
		final CacheGetResult cacheGetResult = getCacheObject(key, timeoutMillis);
		if (cacheGetResult != null) {
			switch (cacheGetResult.type) {
			case HIT:
				return cacheGetResult.value;
			case ERROR:
			case TIMEOUT:
				throw cacheGetResult.errorCause;
			case MISS:
			case EXPIRED:
			case INVALIDATED:
				return null;
			}
		}
		throw new IllegalStateException("Unhandled cacheResultType");
	}

	@Override
	public CacheGetResult getCacheObject(final String key) {
		return getCacheObject(key, defaultOperationTimeoutMillis);
	}

	@Override
	public CacheGetResult getCacheObject(final String key, final long timeoutMillis) {
		try {
			Future<CacheGetResult> getFuture = asyncGetCacheObject(key);
			CacheGetResult r = getFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
			if (r == null) {
				throw new NullPointerException();
			}
			return r;
		} catch (TimeoutException tx) {
			CacheGetResult err = new CacheGetResult(Type.TIMEOUT, tx);
			return err;
		} catch (ExecutionException ee) {
			CacheGetResult err;
			if (ee.getCause() instanceof Exception) {
				err = new CacheGetResult(Type.ERROR, (Exception) ee.getCause());
			} else {
				err = new CacheGetResult(Type.ERROR, ee);
			}
			return err;
		} catch (Exception e) {
			CacheGetResult err = new CacheGetResult(Type.ERROR, e);
			return err;
		}
	}

	@Override
	public Future<CacheGetResult> asyncGetCacheObject(final String key) {
		try {
			if (key == null || key.equals("")) {
				throw new IllegalArgumentException();
			}
			return buildCacheGetterTask(key);
		} catch (Exception e) {
			return DummyFuture.createWithException(e);
		}
	}

	protected Future<CacheGetResult> buildCacheGetterTask(final String key) {
		return new CacheGetterTask(key, backendClient, effectiveInvalidationBackendClient, (BackendKeyBuilder) this,
				(ArcacheConfigurationGetInterface) this, this.randomGenerator);
	}

	@Override
	public void set(final String key, final Object value) throws TimeoutException, Exception {
		set(key, value, null);
	}

	@Override
	public void set(final String key, final Object value, final String[] invalidationKeys)
			throws TimeoutException, Exception {
		Future<Boolean> future = asyncSet(key, value, invalidationKeys);
		try {
			future.get(defaultOperationTimeoutMillis, TimeUnit.MILLISECONDS);
		} catch (ExecutionException ee) {
			if (ee.getCause() instanceof Exception) {
				throw (Exception) ee.getCause();
			}
			throw ee;
		}
	}

	@Override
	public Future<Boolean> asyncSet(final String key, final Object value) {
		return asyncSet(key, value, null);
	}

	@Override
	public Future<Boolean> asyncSet(final String key, final Object value, final String[] invalidationKeys) {
		try {
			if (key == null || key.equals("")) {
				throw new IllegalArgumentException();
			}
			ExpirableCacheObject expObj = new ExpirableCacheObject();
			expObj.timestampMillis = System.currentTimeMillis();
			expObj.value = value;
			expObj.invalidationKeys = invalidationKeys;
			expObj.expirationTTLMillis = defaultExpirationTimeMillis;
			String backendKey = createBackendKey(key);
			return backendClient.asyncSet(backendKey, defaultStoredObjectRemovalTimeMillis, expObj);

		} catch (Exception e) {
			return DummyFuture.createWithException(e);
		}
	}

	@Override
	public void invalidateKey(final String key) throws TimeoutException, Exception {
		invalidateKey(key, defaultInvalidationWindowMillis);
	}

	@Override
	public void invalidateKey(final String key, final long invalidationWindowMillis)
			throws TimeoutException, Exception {
		invalidateKey(key, defaultHardInvalidation, invalidationWindowMillis);
	}

	@Override
	public void invalidateKey(final String key, final boolean hardInvalidation) throws TimeoutException, Exception {
		invalidateKey(key, hardInvalidation, defaultInvalidationWindowMillis);
	}

	@Override
	public void invalidateKey(final String key, final boolean hardInvalidation, final long invalidationWindowMillis)
			throws TimeoutException, Exception {
		Future<Boolean> future = asyncInvalidateKey(key, hardInvalidation, invalidationWindowMillis);
		try {
			future.get(defaultOperationTimeoutMillis, TimeUnit.MILLISECONDS);
		} catch (ExecutionException ee) {
			if (ee.getCause() instanceof Exception) {
				throw (Exception) ee.getCause();
			}
			throw ee;
		}
	}

	@Override
	public Future<Boolean> asyncInvalidateKey(final String key, final boolean hardInvalidation,
			final long invalidationWindowMillis) {
		try {
			if (invalidationWindowMillis < 0) {
				throw new IllegalArgumentException();
			}
			if (key == null || key.equals("")) {
				throw new IllegalArgumentException();
			}
			return buildInvalidateKeyTask(key, hardInvalidation, invalidationWindowMillis);
		} catch (Exception e) {
			return DummyFuture.createWithException(e);
		}
	}

	protected Future<Boolean> buildInvalidateKeyTask(final String key, final boolean hardInvalidation,
			final long invalidationWindowMillis) {
		return new InvalidateKeyTask(key, hardInvalidation, invalidationWindowMillis,
				effectiveInvalidationBackendClient, (BackendKeyBuilder) this, (ArcacheConfigurationGetInterface) this);
	}

	/** Create the key to be used in the backend client */
	public String createBackendKey(final String userKey) {
		if (keyNamespace == null) {
			return userKey;
		}
		return keyNamespace + keyDelimiter + userKey;
	}

	public String createInvalidationBackendKey(final String invalidationKey) {
		return createBackendKey(invalidationKeyPrefix + keyDelimiter + invalidationKey);
	}

}
