package ar.com.siripo.arcache;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.math.ProbabilityFunction;

public interface ArcacheConfigurationGetInterface {

	public long getDefaultOperationTimeoutMillis();

	public boolean getRelaxOperationTimeoutInHeavyLoadSystem();

	public long getTimeMeasurementErrorMillis();

	public long getDefaultInvalidationWindowMillis();

	public boolean getDefaultHardInvalidation();

	public String getKeyNamespace();

	public String getKeyDelimiter();

	public long getDefaultExpirationTimeMillis();

	public long getDefaultStoredObjectRemovalTimeMillis();

	public ArcacheBackendClient getBackendClient();

	public ArcacheBackendClient getInvalidationBackendClient();

	public ProbabilityFunction getExpirationProbabilityFunction();

	public ProbabilityFunction getInvalidationProbabilityFunction();
}
