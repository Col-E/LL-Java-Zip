package software.coley.lljzip.util;

import java.io.ByteArrayOutputStream;
import java.lang.foreign.MemorySegment;

/**
 * Byte output stream with {@link MemorySegment} wrapping provided, without the array-copy of {@link #toByteArray()}.
 *
 * @author xDark
 */
public final class FastWrapOutputStream extends ByteArrayOutputStream {
	/**
	 * @return Wrapper of the current buffer.
	 */
	public MemorySegment wrap() {
		return MemorySegment.ofArray(buf).asSlice(0, count);
	}
}
