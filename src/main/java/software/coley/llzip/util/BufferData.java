package software.coley.llzip.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * File that is backed by a byte buffer.
 *
 * @author xDark
 */
public final class BufferData implements ByteData {
	private final ByteBuffer buffer;
	private volatile boolean cleaned;

	private BufferData(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int getInt(long position) {
		ensureOpen();
		return buffer.getInt(validate(position));
	}

	@Override
	public short getShort(long position) {
		ensureOpen();
		return buffer.getShort(validate(position));
	}

	@Override
	public byte get(long position) {
		ensureOpen();
		return buffer.get(validate(position));
	}

	@Override
	public void get(long position, byte[] b, int off, int len) {
		ensureOpen();
		// Left intentionally as unchained calls due to API differences across Java versions
		// and how the compiler changes output.
		ByteBuffer buffer = this.buffer;
		buffer.slice();
		buffer.order(buffer.order());
		buffer.position(validate(position));
		buffer.get(b, off, len);
	}

	@Override
	public void transferTo(OutputStream out, byte[] buf) throws IOException {
		ensureOpen();
		ByteBuffer buffer = this.buffer;
		int remaining = buffer.remaining();
		if (buffer.hasArray()) {
			out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), remaining);
		} else {
			buffer.mark();
			int copyThreshold = buf.length;
			while (remaining != 0) {
				int length = Math.min(copyThreshold, remaining);
				buffer.get(buf, 0, length);
				out.write(buf, 0, length);
				remaining -= length;
			}
			buffer.reset();
		}
	}

	@Override
	public ByteData slice(long startIndex, long endIndex) {
		ensureOpen();
		return new BufferData(ByteDataUtil.sliceExact(buffer, validate(startIndex), validate(endIndex)));
	}

	@Override
	public long length() {
		ensureOpen();
		return ByteDataUtil.length(buffer);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BufferData)) return false;
		BufferData that = (BufferData) o;
		return buffer.equals(that.buffer);
	}

	@Override
	public int hashCode() {
		return buffer.hashCode();
	}

	@Override
	public void close() {
		if (!cleaned) {
			synchronized (this) {
				if (cleaned)
					return;
				cleaned = true;
				ByteBuffer buffer = this.buffer;
				if (buffer.isDirect()) {
					CleanerUtil.invokeCleaner(buffer);
				}
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	private void ensureOpen() {
		if (cleaned)
			throw new IllegalStateException("Cannot access data after close");
	}

	private static int validate(long v) {
		if (v < 0L || v > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(Long.toString(v));
		}
		return (int) v;
	}

	/**
	 * @param buffer
	 * 		Byte buffer to wrap.
	 *
	 * @return Buffer data.
	 */
	public static BufferData wrap(ByteBuffer buffer) {
		return new BufferData(buffer);
	}

	/**
	 * @param array
	 * 		Byte array to wrap.
	 *
	 * @return Buffer data.
	 */
	public static BufferData wrap(byte[] array) {
		return new BufferData(ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN));
	}
}
