package ar.com.siripo.arcache.backend.jedis;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import org.apache.commons.codec.binary.Base64;

import ar.com.siripo.arcache.backend.ArcacheBackendClient;
import ar.com.siripo.arcache.util.DummyFuture;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.ShardedJedis;

/**
 * Adaptador de la interfaz a Redis via Jedis
 * 
 * @author Mariano Santamarina
 *
 */
public class ArcacheJedisClient implements ArcacheBackendClient {

	private JedisCommands jedisCommands;

	private SerializingTranscoder serializingTranscoder;

	public ArcacheJedisClient(Jedis jedis) {
		this.jedisCommands = jedis;
		initialize();
	}

	public ArcacheJedisClient(ShardedJedis shardedJedis) {
		this.jedisCommands = shardedJedis;
		initialize();
	}

	private void initialize() {
		serializingTranscoder = new SerializingTranscoder();
	}

	@Override
	public Future<Boolean> asyncSet(String key, int ttlSeconds, Object value) {

		String vals = convertObjectToRedisString(value);
		String ret = jedisCommands.setex(key, ttlSeconds, vals);
		if (!"OK".equals(ret)) {
			return new DummyFuture<Boolean>(false);
		}
		return new DummyFuture<Boolean>(true);
	}

	@Override
	public Future<Object> asyncGet(String key) {
		final String r = jedisCommands.get(key);

		Object o = null;
		if (r != null) {
			o = convertRedisStringToObject(r);
		}

		return new DummyFuture<Object>(o);
	}

	protected String convertObjectToRedisString(final Object vobj) {
		try {
			CachedData cd = serializingTranscoder.encode(vobj);
			byte q[] = cd.getData();
			int flags = cd.getFlags();

			ByteBuffer bb = ByteBuffer.allocate(q.length + 4);
			bb.putInt(flags);
			bb.put(q);

			return Base64.encodeBase64String(bb.array());

		} catch (Exception e) {
			throw new IllegalArgumentException("Error enconding the value", e);
		}
	}

	protected Object convertRedisStringToObject(final String redisString) {
		try {
			byte barr[] = Base64.decodeBase64(redisString);
			ByteBuffer bb = ByteBuffer.wrap(barr);
			int flags = bb.getInt();

			byte dbyte[] = new byte[bb.remaining()];
			System.arraycopy(barr, bb.position(), dbyte, 0, bb.remaining());

			CachedData cd = new CachedData(flags, dbyte, CachedData.MAX_SIZE);

			return serializingTranscoder.decode(cd);

		} catch (Exception e) {
			throw new IllegalArgumentException("Error deconding the value", e);
		}
	}

}
