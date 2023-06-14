package software.coley.llzip.util;

import java.io.ByteArrayOutputStream;

/**
 * Byte output stream with {@link BufferData} wrapping provided, without the array-copy of {@link #toByteArray()}.
 *
 * @author xDark
 */
public final class FastWrapOutputStream extends ByteArrayOutputStream {
	/**
	 * @return Wrapper of the current buffer.
	 */
	public BufferData wrap() {
		return BufferData.wrap(buf, 0, count);
	}
}
