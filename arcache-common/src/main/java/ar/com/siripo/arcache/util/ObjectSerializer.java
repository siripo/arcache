package ar.com.siripo.arcache.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This class provides object serialization methods.
 * 
 * @author Mariano Santamarina
 *
 */
public class ObjectSerializer {

	/**
	 * Get the bytes representing the given serialized object. Original Source:
	 * net.spy.memcached.transcoders.BaseSerializingTranscoder
	 */
	public byte[] serializeToByteArray(final Object o) {
		if (o == null) {
			return null;
		}
		byte[] rv = null;
		ByteArrayOutputStream bos = null;
		ObjectOutputStream os = null;
		try {
			bos = new ByteArrayOutputStream();
			os = new ObjectOutputStream(bos);
			os.writeObject(o);
			os.close();
			os = null;

			rv = bos.toByteArray();

			bos.close();
			bos = null;
		} catch (IOException e) {
			throw new IllegalArgumentException("Non-serializable object", e);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Exception e) {
				}
			}
			if (bos != null) {
				try {
					bos.close();
				} catch (Exception e) {
				}
			}
		}
		return rv;
	}

	/**
	 * Get the object represented by the given serialized bytes. Original Source:
	 * net.spy.memcached.transcoders.BaseSerializingTranscoder
	 */
	public Object deserialize(final byte[] in) {
		Object rv = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream is = null;
		try {
			if (in != null) {
				bis = new ByteArrayInputStream(in);
				is = new ObjectInputStream(bis);
				rv = is.readObject();
				is.close();
				bis.close();
			}
		} catch (IOException e) {
		} catch (ClassNotFoundException e) {
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
			try {
				bis.close();
			} catch (Exception e) {
			}
		}
		return rv;
	}
}
