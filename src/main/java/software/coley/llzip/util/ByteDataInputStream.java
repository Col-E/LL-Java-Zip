package software.coley.llzip.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link InputStream} wrapper for {@link ByteData}.
 * 
 * @author xDark
 */
public class ByteDataInputStream extends InputStream {
	
	private final ByteData data;
	private long offset;

	/**
	 * @param data
	 * 		Data to read.
	 */
	public ByteDataInputStream(ByteData data) {
		this.data = data;
	}

	@Override
	public int read() throws IOException {
		ByteData buffer = this.data;
		long offset = this.offset;
		if (offset == buffer.length()) {
			return -1;
		}
		return buffer.get(this.offset++) & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ByteData buffer = this.data;
		long offset = this.offset;
		long length = buffer.length();
		if (offset == length) {
			return -1;
		}
		long left = len - offset;
		if (left < len) {
			len = (int) left;
		}
		buffer.get(offset, b, off, len);
		this.offset += left;
		return len;
	}
}
