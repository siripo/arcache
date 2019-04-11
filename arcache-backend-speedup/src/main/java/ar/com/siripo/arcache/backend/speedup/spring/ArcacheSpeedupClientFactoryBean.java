package ar.com.siripo.arcache.backend.speedup.spring;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.backend.speedup.ArcacheSpeedupClient;
import ar.com.siripo.arcache.backend.speedup.ArcacheSpeedupConfigurationSetInterface;
import ar.com.siripo.arcache.backend.speedup.ArcacheSpeedupTracker;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public class ArcacheSpeedupClientFactoryBean implements FactoryBean<ArcacheSpeedupClient>, InitializingBean,
		DisposableBean, ArcacheSpeedupConfigurationSetInterface {

	protected ArcacheSpeedupClient client;

	public ArcacheSpeedupClientFactoryBean() {
		client = new ArcacheSpeedupClient();
	}

	@Override
	public void destroy() throws Exception {
		client = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		client.initialize();
	}

	@Override
	public ArcacheSpeedupClient getObject() throws Exception {
		return client;
	}

	@Override
	public Class<?> getObjectType() {
		return ArcacheSpeedupClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setBackendClient(ArcacheBackendClient backendClient) {
		client.setBackendClient(backendClient);
	}

	@Override
	public void setInvalidationKeysCacheSize(int cacheSize) {
		client.setInvalidationKeysCacheSize(cacheSize);
	}

	@Override
	public void setObjectsCacheSize(int cacheSize) {
		client.setObjectsCacheSize(cacheSize);
	}

	@Override
	public void setMissesCacheSize(int cacheSize) {
		client.setMissesCacheSize(cacheSize);
	}

	@Override
	public void setInvalidationKeysExpirationMillis(long expirationMillis) {
		client.setInvalidationKeysExpirationMillis(expirationMillis);
	}

	@Override
	public void setObjectsExpirationMillis(long expirationMillis) {
		client.setObjectsExpirationMillis(expirationMillis);
	}

	@Override
	public void setMissesExpirationMillis(long expirationMillis) {
		client.setMissesExpirationMillis(expirationMillis);
	}

	@Override
	public void setProtectAgainstBackendFailures(boolean protect) {
		client.setProtectAgainstBackendFailures(protect);
	}

	@Override
	public void setSpeedupCacheTTLSeconds(int ttlSeconds) {
		client.setSpeedupCacheTTLSeconds(ttlSeconds);
	}

	@Override
	public void setExpirationProbabilityFunction(ProbabilityFunction probabilityFunction) {
		client.setExpirationProbabilityFunction(probabilityFunction);
	}

	@Override
	public void setTracker(ArcacheSpeedupTracker tracker) {
		client.setTracker(tracker);
	}

}
