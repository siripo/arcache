package ar.com.siripo.arcache.backend.jedis;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArcacheJedisClientTest {

	ArcacheJedisClient client;
	JedisCommands jedis;

	@Before
	public void setUp() throws Exception {
		Jedis jedis = new Jedis("localhost");
		this.jedis = jedis;
		client = new ArcacheJedisClient(jedis);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test001JedisConnection() throws Exception {

		String key = "TESTKEY";
		String val = "THEVALUE";
		try {
			jedis.setex(key, 10, val);
		} catch (JedisConnectionException e) {
			fail("Start Redis Locally to run the test");
		}
		String r = jedis.get(key);

		assertEquals(r, val);
	}

	@Test
	public void testGetSet() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("OTHER");

		Future<Boolean> setFuture = client.asyncSet("TESTCLIENT", 10, hs);

		if (!setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue()) {
			fail("Failed to store");
		}

		Future<Object> getFuture = client.asyncGet("TESTCLIENT");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertFalse(retrievedValue == hs);
	}

	@Test
	public void testSerialization() throws Exception {
		HashSet<String> hs = new HashSet<String>();
		hs.add("SomeString");
		hs.add("SecondString");

		String s = client.convertObjectToRedisString(hs);
		Object retrievedValue = client.convertRedisStringToObject(s);

		assertNotNull(retrievedValue);

		assertThat(retrievedValue, instanceOf(hs.getClass()));

		assertEquals(retrievedValue, hs);

		assertFalse(retrievedValue == hs);
	}
}
