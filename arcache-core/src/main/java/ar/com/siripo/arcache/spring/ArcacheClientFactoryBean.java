package ar.com.siripo.arcache.spring;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import ar.com.siripo.arcache.ArcacheClient;
import ar.com.siripo.arcache.ArcacheConfigurationSetInterface;
import ar.com.siripo.arcache.backend.ArcacheBackendClient;

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
	public void setDefaultOperationTimeout(long timeoutMillis) {
		client.setDefaultOperationTimeout(timeoutMillis);
	}

	@Override
	public void setTimeMeasurementError(long errorSecs) {
		client.setTimeMeasurementError(errorSecs);
	}

	@Override
	public void setDefaultInvalidationWindow(long windowSecs) {
		client.setDefaultInvalidationWindow(windowSecs);
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
	public void setDefaultExpirationTime(long expirationTimeSecs) {
		client.setDefaultExpirationTime(expirationTimeSecs);
	}

	@Override
	public void setDefaultStoredObjectRemovalTime(long removeTimeSecs) {
		client.setDefaultStoredObjectRemovalTime(removeTimeSecs);
	}

	@Override
	public void setBackendClient(ArcacheBackendClient backendClient) {
		client.setBackendClient(backendClient);
	}

}
