package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class BackendKeyBuilderTest {

	BackendKeyBuilder keyBuilder;
	ArcacheConfigurationSetInterface config;
	String invalidationKeyPrefix;

	@Before
	public void setUp() throws Exception {
		ArcacheClient ac = new ArcacheClient(null);
		keyBuilder = ac;
		config = ac;
		invalidationKeyPrefix = ac.invalidationKeyPrefix;
	}

	@Test
	public void testCreateBackendKey() {
		config.setKeyNamespace(null);
		config.setKeyDelimiter("//");

		assertEquals(keyBuilder.createBackendKey("juan"), "juan");

		config.setKeyNamespace("");
		assertEquals(keyBuilder.createBackendKey("juan"), "juan");

		config.setKeyNamespace("namespace");
		assertEquals(keyBuilder.createBackendKey("pili"), "namespace//pili");
	}

	@Test
	public void testCreateInvalidationBackendKey() {
		config.setKeyNamespace(null);
		config.setKeyDelimiter("//");

		assertEquals(keyBuilder.createInvalidationBackendKey("juan"), invalidationKeyPrefix + "//juan");

		config.setKeyNamespace("nnn");
		assertEquals(keyBuilder.createInvalidationBackendKey("juan"), "nnn//" + invalidationKeyPrefix + "//juan");
	}

}
