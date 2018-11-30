package ar.com.siripo.arcache;

interface BackendKeyBuilder {

	String createBackendKey(String userKey);

	String createInvalidationBackendKey(String invalidationKey);
}
