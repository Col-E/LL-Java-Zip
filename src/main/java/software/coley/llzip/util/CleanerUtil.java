package software.coley.llzip.util;

import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

/**
 * Utility to invoke cleaners in a {@link ByteBuffer}.
 *
 * @author xDark
 */
public final class CleanerUtil {

	private static final Method INVOKE_CLEANER;
	private static final Method GET_CLEANER;
	private static final boolean SUPPORTED;

	private CleanerUtil() {
	}

	/**
	 * Attempts to clean direct buffer.
	 *
	 * @param buffer
	 * 		Buffer to clean.
	 *
	 * @throws IllegalStateException
	 * 		If buffer is not direct, slice or duplicate, or
	 * 		cleaner failed to invoke.
	 */
	public static void invokeCleaner(ByteBuffer buffer) {
		if (!buffer.isDirect()) {
			throw new IllegalStateException("buffer is not direct");
		}
		if (!SUPPORTED) {
			return;
		}
		Method getCleaner = GET_CLEANER;
		Method invokeCleaner = INVOKE_CLEANER;
		try {
			if (getCleaner != null) {
				Object cleaner = getCleaner.invoke(buffer);
				if (cleaner == null) {
					throw new IllegalStateException("slice or duplicate");
				}
				invokeCleaner.invoke(cleaner);
			} else {
				invokeCleaner.invoke(UnsafeUtil.get(), buffer);
			}
		} catch(InvocationTargetException ex) {
			throw new IllegalStateException("Failed to invoke clean method", ex.getTargetException());
		} catch(IllegalAccessException ex) {
			throw new IllegalStateException("cleaner became inaccessible", ex);
		}
	}

	static {
		boolean supported = false;
		Method invokeCleaner;
		Method getCleaner = null;
		try {
			invokeCleaner = Unsafe.class.getDeclaredMethod("invokeCleaner", ByteBuffer.class);
			invokeCleaner.setAccessible(true);
			ByteBuffer tmp = ByteBuffer.allocateDirect(1);
			invokeCleaner.invoke(UnsafeUtil.get(), tmp);
			supported = true;
		} catch(NoSuchMethodException ignored) {
			supported = true;
			ByteBuffer tmp = ByteBuffer.allocateDirect(1);
			try {
				Class<?> directBuffer = Class.forName("sun.nio.ch.DirectBuffer");
				getCleaner = directBuffer.getDeclaredMethod("cleaner");
				invokeCleaner = getCleaner.getReturnType().getDeclaredMethod("clean");
				invokeCleaner.setAccessible(true);
				getCleaner.setAccessible(true);
				invokeCleaner.invoke(getCleaner.invoke(tmp));
			} catch(Exception ignored1) {
				invokeCleaner = null;
				getCleaner = null;
			}
		} catch(Exception ex) {
			invokeCleaner = null;
		}
		INVOKE_CLEANER = invokeCleaner;
		GET_CLEANER = getCleaner;
		SUPPORTED = supported;
	}
}
