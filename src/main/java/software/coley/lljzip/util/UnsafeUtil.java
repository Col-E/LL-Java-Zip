package software.coley.lljzip.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Unsafe accessor.
 *
 * @author XDark
 */
class UnsafeUtil {
	private static final Unsafe UNSAFE;
	private static final MethodHandles.Lookup LOOKUP;

	/**
	 * @return Unsafe instance.
	 */
	public static Unsafe get() {
		return UNSAFE;
	}

	/**
	 * @return Unsafe instance.
	 */
	public static MethodHandles.Lookup lookup() {
		return LOOKUP;
	}

	static {
		try {
			Field f = Unsafe.class.getDeclaredField("theUnsafe");
			f.setAccessible(true);
			Unsafe u = UNSAFE = (Unsafe) f.get(null);
			f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
			MethodHandles.publicLookup();
			LOOKUP = (MethodHandles.Lookup) u.getObject(u.staticFieldBase(f), u.staticFieldOffset(f));
		} catch (NoSuchFieldException | IllegalAccessException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
