package software.coley.llzip.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.zip.Inflater;

/**
 * Used to more efficiently reset a {@link Inflater} than the default impl of {@link Inflater#reset()}.
 *
 * @author xDark
 */
public final class InflaterHackery {
	private static final Unsafe UNSAFE = UnsafeUtil.get();
	private static final MethodHandle MH_RESET;
	private static final MethodHandle MH_FINISHED;
	private static final long zRef_offset;
	private static final long zRef_address_offset;

	private InflaterHackery() {
	}

	/**
	 * @param inflater
	 * 		Inflater to reset.
	 */
	public static void reset(Inflater inflater) {
		Unsafe u = UNSAFE;
		long address = u.getLong(u.getObject(inflater, zRef_offset), zRef_address_offset);
		try {
			MH_RESET.invokeExact(address);
			MH_FINISHED.invokeExact(inflater, false);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	static {
		try {
			Unsafe u = UNSAFE;
			Field f = Inflater.class.getDeclaredField("zsRef");
			Class<?> zrefClass = f.getType();
			zRef_offset = u.objectFieldOffset(f);
			f = zrefClass.getDeclaredField("address");
			zRef_address_offset = u.objectFieldOffset(f);
			MethodHandles.Lookup l = UnsafeUtil.lookup();
			MH_RESET = l.findStatic(Inflater.class, "reset", MethodType.methodType(void.class, long.class));
			MH_FINISHED = l.findSetter(Inflater.class, "finished", boolean.class);
		} catch (ReflectiveOperationException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}