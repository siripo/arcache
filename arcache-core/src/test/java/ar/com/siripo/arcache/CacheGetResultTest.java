package ar.com.siripo.arcache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import ar.com.siripo.arcache.CacheGetResult.Type;

public class CacheGetResultTest {

	@Test
	public void testIsHit() {
		assertTrue(new CacheGetResult(Type.HIT).isHit());
		assertFalse(new CacheGetResult(Type.EXPIRED).isHit());
	}
	
	@Test
	public void testIsExpired() {
		assertTrue(new CacheGetResult(Type.EXPIRED).isExpired());
		assertFalse(new CacheGetResult(Type.HIT).isExpired());
	}
	
	@Test
	public void testIsInvalidated() {
		assertTrue(new CacheGetResult(Type.INVALIDATED).isInvalidated());
		assertFalse(new CacheGetResult(Type.HIT).isInvalidated());
	}

	@Test
	public void testIsHitOrExpired() {
		assertTrue(new CacheGetResult(Type.HIT).isHitOrExpired());
		assertTrue(new CacheGetResult(Type.EXPIRED).isHitOrExpired());
		assertFalse(new CacheGetResult(Type.INVALIDATED).isHitOrExpired());
	}

	@Test
	public void testIsHitExpiredOrInvalidated() {
		assertTrue(new CacheGetResult(Type.HIT).isHitExpiredOrInvalidated());
		assertTrue(new CacheGetResult(Type.EXPIRED).isHitExpiredOrInvalidated());
		assertTrue(new CacheGetResult(Type.INVALIDATED).isHitExpiredOrInvalidated());
		assertFalse(new CacheGetResult(Type.MISS).isHitExpiredOrInvalidated());
	}

	@Test
	public void testIsAnyTypeOfError() {
		assertTrue(new CacheGetResult(Type.ERROR).isAnyTypeOfError());
		assertTrue(new CacheGetResult(Type.TIMEOUT).isAnyTypeOfError());
		assertFalse(new CacheGetResult(Type.HIT).isAnyTypeOfError());
	}

	@Test
	public void testCacheGetResultType() {
		CacheGetResult cr = new CacheGetResult(Type.ERROR);
		assertEquals(cr.type, Type.ERROR);
	}

	@Test
	public void testCacheGetResultTypeException() {
		Exception x = new Exception();
		CacheGetResult cr = new CacheGetResult(Type.ERROR, x);
		assertEquals(cr.type, Type.ERROR);
		assertEquals(cr.errorCause, x);
	}

	@Test
	public void testIsMiss() {
		assertTrue(new CacheGetResult(Type.MISS).isMiss());
		assertFalse(new CacheGetResult(Type.HIT).isMiss());
	}

	@Test
	public void testBuilderAndGetters() {
		Exception errorCause = new Exception();
		String invalidatedKey = "invk";
		String invalidationKeys[] = new String[] { "hola" };
		long storeTimestamp = 12345L;
		Object value = new HashSet<String>();
		CacheGetResult cgr = new CacheGetResult.Builder(Type.HIT).withType(Type.EXPIRED).withErrorCause(errorCause)
				.withInvalidatedKey(invalidatedKey).withInvalidationKeys(invalidationKeys)
				.withStoreTimestamp(storeTimestamp).withValue(value).build();

		assertNotEquals(cgr.getType(), Type.HIT);
		assertEquals(cgr.getType(), Type.EXPIRED);
		assertEquals(cgr.getErrorCause(), errorCause);
		assertEquals(cgr.getInvalidatedKey(), invalidatedKey);
		assertArrayEquals(cgr.getInvalidationKeys(), invalidationKeys);
		assertEquals(cgr.getStoreTimestamp(), storeTimestamp);
		assertEquals(cgr.getValue(), value);

		cgr = new CacheGetResult.Builder(cgr).withInvalidatedKey("invk2").withType(Type.HIT).build();

		assertEquals(cgr.getType(), Type.HIT);
		assertNotEquals(cgr.getType(), Type.EXPIRED);
		assertEquals(cgr.getErrorCause(), errorCause);
		assertNotEquals(cgr.getInvalidatedKey(), invalidatedKey);
		assertEquals(cgr.getInvalidatedKey(), "invk2");
		assertArrayEquals(cgr.getInvalidationKeys(), invalidationKeys);
		assertEquals(cgr.getStoreTimestamp(), storeTimestamp);
		assertEquals(cgr.getValue(), value);
	}

	@Test
	public void testErrorBuilder() {
		Exception errorCause = new Exception();

		CacheGetResult cgr = new CacheGetResult.ErrorBuilder(errorCause).build();

		assertEquals(cgr.getType(), Type.ERROR);
		assertEquals(cgr.getErrorCause(), errorCause);

	}

}
