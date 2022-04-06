package software.coley.llzip.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * {@link InputStream} wrapper for {@link ByteBuffer}.
 * 
 * @author xDark
 */
public class BufferInputStream extends InputStream {
	
	private final ByteBuffer buffer;

	/**
	 * @param buffer
	 * 		Byte buffer instance.
	 */
	public BufferInputStream(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int read() throws IOException {
		ByteBuffer buffer = this.buffer;
		return !buffer.hasRemaining() ? -1 : buffer.get() & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		ByteBuffer buffer = this.buffer;
		if (!buffer.hasRemaining())
			return -1;
		len = Math.min(len, buffer.remaining());
		buffer.get(b, off, len);
		return len;
	}
}
