package software.coley.llzip.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Mapped file that is backed by byte buffer.
 *
 * @author xDark
 */
public final class BufferData implements ByteData {
	private final ByteBuffer buffer;

	private BufferData(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public int getInt(long position) {
		return buffer.getInt(validate(position));
	}

	@Override
	public short getShort(long position) {
		return buffer.getShort(validate(position));
	}

	@Override
	public byte get(long position) {
		return buffer.get(validate(position));
	}

	@Override
	public void get(long position, byte[] b, int off, int len) {
		ByteBuffer buffer = this.buffer;
		((ByteBuffer) buffer.slice()
				.order(buffer.order())
				.position(validate(position)))
				.get(b, off, len);
	}

	@Override
	public void transferTo(OutputStream out, byte[] buf) throws IOException {
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
		return new BufferData(ByteDataUtil.sliceExact(buffer, validate(startIndex), validate(endIndex)));
	}

	@Override
	public long length() {
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
