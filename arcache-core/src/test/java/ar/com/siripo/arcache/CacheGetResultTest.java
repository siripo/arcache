package ar.com.siripo.arcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ar.com.siripo.arcache.CacheGetResult.Type;

public class CacheGetResultTest {

	@Test
	public void testIsHit() {
		assertTrue(new CacheGetResult(Type.HIT).isHit());
		assertFalse(new CacheGetResult(Type.EXPIRED).isHit());
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
	public void testHasAnyTypeOfError() {
		assertTrue(new CacheGetResult(Type.ERROR).hasAnyTypeOfError());
		assertTrue(new CacheGetResult(Type.TIMEOUT).hasAnyTypeOfError());
		assertFalse(new CacheGetResult(Type.HIT).hasAnyTypeOfError());
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
		assertEquals(cr.cause, x);
	}

}
