package ar.com.siripo.arcache.spring;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import ar.com.siripo.arcache.ArcacheClient;

public class ArcacheClientFactoryBeanTest {

	ArcacheClientFactoryBean factoryBean;
	
	@Before
	public void setUp() throws Exception {
		factoryBean=new ArcacheClientFactoryBean();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		factoryBean.afterPropertiesSet();
	}

	@Test
	public void testGetObject() throws Exception {
		assertTrue(factoryBean.getObject() instanceof ArcacheClient);
	}

	@Test
	public void testGetObjectType() {
		assertTrue(factoryBean.getObjectType() == ArcacheClient.class);
	}

	@Test
	public void testIsSingleton() {
		assertTrue(factoryBean.isSingleton());
	}

	@Test
	public void testSetDefaultOperationTimeout() throws Exception {
		factoryBean.setDefaultOperationTimeout(20);
		assertEquals(factoryBean.getObject().getDefaultOperationTimeout(),20);
	}

}
