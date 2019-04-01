package ar.com.siripo.arcache.backend.jedis;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
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
	public void testGetMiss() throws Exception {
		Future<Object> getFuture = client.asyncGet("MISS");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertNull(retrievedValue);
	}

	@Test
	public void testSetFail() throws Exception {
		Jedis jedis = new Jedis("localhost") {
			public String setex(String key, int seconds, String value) {
				return ("FAIL");
			}
		};
		ArcacheJedisClient clientx = new ArcacheJedisClient(jedis);

		Future<Boolean> setFuture = clientx.asyncSet("TESTCLIENT", 10, "qweqwe");

		assertFalse("Expect failed to store", setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue());
	}

	@Test
	public void testShardedJedisConstructor() throws Exception {
		List<JedisShardInfo> jedisShards = new Vector<JedisShardInfo>();
		jedisShards.add(new JedisShardInfo("localhost"));
		ShardedJedis shardedJedis = new ShardedJedis(jedisShards);
		ArcacheJedisClient clientx = new ArcacheJedisClient(shardedJedis);

		Future<Boolean> setFuture = clientx.asyncSet("KEYSH", 10, "hello");

		if (!setFuture.get(50, TimeUnit.MILLISECONDS).booleanValue()) {
			fail("Failed to store");
		}

		Future<Object> getFuture = clientx.asyncGet("KEYSH");

		Object retrievedValue = getFuture.get(50, TimeUnit.MILLISECONDS);

		assertEquals(retrievedValue, "hello");
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

	@Test
	public void testSerializationNull() throws Exception {
		String s = client.convertObjectToRedisString(null);
		Object retrievedValue = client.convertRedisStringToObject(s);

		assertNull(retrievedValue);
	}

	@Test
	public void testSerializationStringType() throws Exception {
		String s = client.convertObjectToRedisString("hola que tal");
		Object retrievedValue = client.convertRedisStringToObject(s);

		assertEquals(retrievedValue, "hola que tal");
	}

	@Test
	public void testSerializationNonSerializable() throws Exception {
		final class NonSerializableClass {
		}

		try {
			client.convertObjectToRedisString(new NonSerializableClass());
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void testSerializationNonDeserializableBadBase64() throws Exception {
		try {
			client.convertRedisStringToObject("a");
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}
	}
}
