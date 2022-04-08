package software.coley.llzip.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Unsafe accessor.
 *
 * @author XDark
 */
class UnsafeUtil {

	private static final Unsafe UNSAFE;

	/**
	 * @return Unsafe instance.
	 */
	public static Unsafe get() {
		return UNSAFE;
	}

	static {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			UNSAFE = (Unsafe) f.get(null);
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
