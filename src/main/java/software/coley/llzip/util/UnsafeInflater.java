package software.coley.llzip.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.zip.Inflater;

/**
 * Inflater extension providing non-synchronized resetting via unsafe.
 *
 * @author Matt Coley
 */
public class UnsafeInflater extends Inflater {
	private static final String ERR_SUFFIX = "If this is due to JDK restrictions, use DeflateDecompressor";
	private static final MethodHandles.Lookup LOOKUP = UnsafeUtil.lookup();
	private static final Unsafe UNSAFE = UnsafeUtil.get();
	private static final ByteBuffer DEFAULT_BUF = ByteBuffer.allocate(0);
	private static long off_zsRef;
	private static long off_input;
	private static long off_inputArray;
	private static long off_finished;
	private static long off_needDict;
	private static long off_bytesRead;
	private static long off_bytesWritten;
	private static MethodHandle mh_reset;
	private static MethodHandle mh_address;
	/** Indicates unsafe init failed */
	public static boolean initFail;

	static {
		// All these fields are private, so we will use hacks to access them.
		try {
			off_zsRef = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("zsRef"));
			off_input = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("input"));
			off_inputArray = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("inputArray"));
			off_finished = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("finished"));
			off_needDict = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("needDict"));
			off_bytesRead = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("bytesRead"));
			off_bytesWritten = UNSAFE.objectFieldOffset(Inflater.class.getDeclaredField("bytesWritten"));
			mh_reset = LOOKUP.findStatic(Inflater.class, "reset", MethodType.methodType(void.class, long.class));
		} catch (Exception ex) {
			initFail = true;
		}
	}

	/**
	 * @param nowrap
	 * 		if true then support GZIP compatible compression
	 */
	public UnsafeInflater(boolean nowrap) {
		super(nowrap);
	}

	/**
	 * {@link #reset()} but without synchronization.
	 */
	public void fastReset() {
		try {
			Object zsRefValue = UNSAFE.getObject(this, off_zsRef);
			if (mh_address == null) {
				MethodHandles.Lookup lookup = UnsafeUtil.lookup();
				mh_address = lookup.findGetter(zsRefValue.getClass(), "address", long.class);
			}
			long addressRet = (long) mh_address.invoke(zsRefValue);
			mh_reset.invoke(addressRet);

			UNSAFE.putObject(this, off_input, DEFAULT_BUF);
			UNSAFE.putObject(this, off_inputArray, null);
			// This is so fucking dumb, but resetting these causes a 2x slowdown. Yes, putBoolean is SLOW!
			// In testing, we don't need these values changes, so I guess we'll ignore this for now.
			//  UNSAFE.putBoolean(this, off_finished, false);
			//  UNSAFE.putBoolean(this, off_needDict, false);
			UNSAFE.putLong(this, off_bytesRead, 0);
			UNSAFE.putLong(this, off_bytesWritten, 0);
		} catch (Throwable ex) {
			initFail = true;
			throw new IllegalStateException("Failed to reset unsafely. " + ERR_SUFFIX, ex);
		}
	}
}
