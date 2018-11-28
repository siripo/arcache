package ar.com.siripo.arcache.backend.inmemory;

public class ArcacheInMemoryObject {
	// Expiration time expressed in milliseconds
	long expirationTime;

	byte[] data;

	protected ArcacheInMemoryObject() {
	}
}
