package software.coley.lljzip.util;

import software.coley.lljzip.format.compression.DeflateEntry;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Used to more efficiently handle {@link Inflater} operations, without synchronization locks.
 *
 * @author xDark
 */
public final class InflaterHackery {
	private static final Unsafe UNSAFE = UnsafeUtil.get();
	private static final boolean NEW_INFLATE;
	private static final boolean OLD_INFLATE;
	private static final MethodHandle INFLATE;
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
			if (OLD_INFLATE)
				MH_FINISHED.invokeExact(inflater, false);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	/**
	 * @param entry
	 * 		Entry with data.
	 * @param in
	 * 		Input array.
	 * @param len
	 * 		Length of array.
	 * @param out
	 * 		Output buffer.
	 *
	 * @return Number of compressed bytes.
	 *
	 * @throws DataFormatException
	 * 		When any issue in inflation occurs.
	 */
	public static int inflate(DeflateEntry entry, byte[] in, int len, byte[] out) throws DataFormatException {
		try {
			Inflater inflater = entry.inflater;
			if (NEW_INFLATE) {
				Unsafe u = UNSAFE;
				// Taken from 'Inflater.inflate' logic
				long address = u.getLong(u.getObject(inflater, zRef_offset), zRef_address_offset);
				int offset = entry.offset;
				long packed = (long) INFLATE.invokeExact(inflater, address, in, offset, len - offset, out, 0, out.length);
				int read = (int) (packed & 0x7fff_ffffL);
				int written = (int) (packed >>> 31 & 0x7fff_ffffL);
				int finished = (int) (packed >>> 62 & 1);
				if (finished == 0 && written == 0 && len == offset) // hack to break out of infinite deflates
					finished = 1;
				int newOffset = offset + read;
				entry.state = finished << 1 | (((newOffset - in.length) >>> 31) ^ 1);
				entry.offset = newOffset;
				return written;
			} else {
				int written = inflater.inflate(out);
				boolean finished = inflater.finished();
				if (!finished && written == 0 && len == entry.offset) // hack to break out of infinite deflates
					finished = true;
				entry.state = finished ? 2 : written == 0 ? 1 : 0;
				return written;
			}
		} catch (OutOfMemoryError | StackOverflowError | DataFormatException e) {
			throw e;
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
			boolean newInflate = true;
			MethodHandle inflate;
			try {
				inflate = l.findVirtual(Inflater.class, "inflateBytesBytes", MethodType.methodType(long.class, long.class, byte[].class, int.class, int.class, byte[].class, int.class, int.class));
			} catch (NoSuchMethodException ignored) {
				newInflate = false;
				inflate = null;
			}
			NEW_INFLATE = newInflate;
			OLD_INFLATE = !newInflate;
			INFLATE = inflate;
			MH_RESET = l.findStatic(Inflater.class, "reset", MethodType.methodType(void.class, long.class));
			MH_FINISHED = l.findSetter(Inflater.class, "finished", boolean.class);
		} catch (ReflectiveOperationException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}