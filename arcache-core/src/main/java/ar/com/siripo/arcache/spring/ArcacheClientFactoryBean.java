package ar.com.siripo.arcache.spring;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import ar.com.siripo.arcache.ArcacheClient;
import ar.com.siripo.arcache.ArcacheConfigurationSetInterface;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public class ArcacheClientFactoryBean
		implements FactoryBean<ArcacheClient>, InitializingBean, DisposableBean, ArcacheConfigurationSetInterface {

	private ArcacheClient client;

	public ArcacheClientFactoryBean() {
		client = new ArcacheClient();
	}

	@Override
	public void destroy() throws Exception {
		client = null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public ArcacheClient getObject() throws Exception {
		return client;
	}

	@Override
	public Class<?> getObjectType() {
		return ArcacheClient.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void setDefaultOperationTimeoutMillis(long timeoutMillis) {
		client.setDefaultOperationTimeoutMillis(timeoutMillis);
	}

	@Override
	public void setTimeMeasurementErrorMillis(long errorMillis) {
		client.setTimeMeasurementErrorMillis(errorMillis);
	}

	@Override
	public void setDefaultInvalidationWindowMillis(long windowMillis) {
		client.setDefaultInvalidationWindowMillis(windowMillis);
	}

	@Override
	public void setDefaultHardInvalidation(boolean hardInvalidation) {
		client.setDefaultHardInvalidation(hardInvalidation);
	}

	@Override
	public void setKeyNamespace(String namespace) {
		client.setKeyNamespace(namespace);
	}

	@Override
	public void setKeyDelimiter(String keyDelimiter) {
		client.setKeyDelimiter(keyDelimiter);
	}

	@Override
	public void setDefaultExpirationTimeMillis(long expirationTimeMillis) {
		client.setDefaultExpirationTimeMillis(expirationTimeMillis);
	}

	@Override
	public void setDefaultStoredObjectRemovalTimeMillis(long removeTimeMillis) {
		client.setDefaultStoredObjectRemovalTimeMillis(removeTimeMillis);
	}

	@Override
	public void setBackendClient(ArcacheBackendClient backendClient) {
		client.setBackendClient(backendClient);
	}

	@Override
	public void setExpirationProbabilityFunction(ProbabilityFunction expirationProbabilityFunction) {
		client.setExpirationProbabilityFunction(expirationProbabilityFunction);

	}

	@Override
	public void setInvalidationProbabilityFunction(ProbabilityFunction invalidationProbabilityFunction) {
		client.setInvalidationProbabilityFunction(invalidationProbabilityFunction);
	}

}
