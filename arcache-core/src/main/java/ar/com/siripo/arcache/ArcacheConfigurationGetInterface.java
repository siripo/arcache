package ar.com.siripo.arcache;

public interface ArcacheConfigurationGetInterface {

	public long getDefaultOperationTimeout();

	public long getTimeMeasurementError();

	public long getDefaultInvalidationWindow();

	public boolean getDefaultHardInvalidation();

	public String getKeyNamespace();

	public String getKeyDelimiter();

	public long getDefaultExpirationTime();

	public long getDefaultStoredObjectRemovalTime();
}
