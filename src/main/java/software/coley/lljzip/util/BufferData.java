package software.coley.lljzip.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File that is backed by a byte buffer.
 *
 * @author xDark
 */
public final class BufferData implements ByteData {
	private final ByteBuffer buffer;
	private final AtomicBoolean cleaned;

	private BufferData(ByteBuffer buffer, AtomicBoolean cleaned) {
		this.buffer = buffer;
		this.cleaned = cleaned;
	}

	@Override
	public int getInt(long position) {
		ensureOpen();
		return buffer.getInt(validate(position));
	}

	@Override
	public long getLong(long position) {
		ensureOpen();
		return buffer.getLong(validate(position));
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
	@SuppressWarnings("all")
	public void get(long position, byte[] b, int off, int len) {
		ensureOpen();
		// Left intentionally as unchained calls due to API differences across Java versions
		// and how the compiler changes output.
		ByteBuffer buffer = this.buffer.slice();
		buffer.order(buffer.order());
		((Buffer) buffer).position(validate(position));
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
			((Buffer)buffer).mark();
			int copyThreshold = buf.length;
			while (remaining != 0) {
				int length = Math.min(copyThreshold, remaining);
				buffer.get(buf, 0, length);
				out.write(buf, 0, length);
				remaining -= length;
			}
			((Buffer)buffer).reset();
		}
	}

	@Override
	public ByteData slice(long startIndex, long endIndex) {
		ensureOpen();
		return new BufferData(ByteDataUtil.sliceExact(buffer, validate(startIndex), validate(endIndex)), cleaned);
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
		if (!cleaned.get()) {
			synchronized (this) {
				if (cleaned.get())
					return;
				cleaned.set(true);
				ByteBuffer buffer = this.buffer;
				if (buffer.isDirect()) {
					CleanerUtil.invokeCleaner(buffer);
				}
			}
		}
	}

	@Override
	public boolean isClosed() {
		return cleaned.get();
	}

	private void ensureOpen() {
		if (cleaned.get())
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
		return new BufferData(buffer, new AtomicBoolean());
	}

	/**
	 * @param array
	 * 		Byte array to wrap.
	 *
	 * @return Buffer data.
	 */
	public static BufferData wrap(byte[] array) {
		return new BufferData(ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN), new AtomicBoolean());
	}

	/**
	 * @param array
	 * 		Byte array to wrap.
	 * @param offset
	 * 		Offset into the array to start at.
	 * @param length
	 * 		Length of content.
	 *
	 * @return Buffer data.
	 */
	public static BufferData wrap(byte[] array, int offset, int length) {
		return new BufferData(ByteBuffer.wrap(array, offset, length).order(ByteOrder.LITTLE_ENDIAN), new AtomicBoolean());
	}
}
