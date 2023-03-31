package software.coley.llzip.util;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;

/**
 * Mapped file backed by address & length.
 *
 * @author xDark
 */
final class UnsafeMappedFile implements ByteData {
	private static final boolean SWAP = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
	private static final Unsafe UNSAFE = UnsafeUtil.get();

	private volatile boolean cleaned;
	private final long address;
	private final long end;
	private final Runnable deallocator;
	@SuppressWarnings("unused")
	private final Object attachment;

	private UnsafeMappedFile(Object attachment, long address, long end) {
		this.attachment = attachment;
		this.address = address;
		this.end = end;
		deallocator = null;
	}

	UnsafeMappedFile(long address, long length, Runnable deallocator) {
		this.address = address;
		this.end = address + length;
		this.deallocator = deallocator;
		attachment = null;
	}

	@Override
	public int getInt(long position) {
		ensureOpen();
		return swap(UNSAFE.getInt(validate(position)));
	}

	@Override
	public short getShort(long position) {
		ensureOpen();
		return swap(UNSAFE.getShort(validate(position)));
	}

	@Override
	public byte get(long position) {
		ensureOpen();
		return UNSAFE.getByte(validate(position));
	}

	@Override
	public void get(long position, byte[] buffer, int off, int len) {
		ensureOpen();
		long address = validate(position);
		if (address + len > end)
			throw new IllegalArgumentException();
		UNSAFE.copyMemory(null, address, buffer, Unsafe.ARRAY_BYTE_BASE_OFFSET + off, len);
	}

	@Override
	public void transferTo(OutputStream out, byte[] buffer) throws IOException {
		ensureOpen();
		int copyThreshold = buffer.length;
		long address = this.address;
		long remaining = end - address;
		while (remaining != 0L) {
			int length = (int) Math.min(copyThreshold, remaining);
			UNSAFE.copyMemory(null, address, buffer, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
			remaining -= length;
			address += length;
			out.write(buffer, 0, length);
		}
	}

	@Override
	public ByteData slice(long startIndex, long endIndex) {
		ensureOpen();
		if (startIndex > endIndex)
			throw new IllegalArgumentException();
		return new UnsafeMappedFile(this, validate(startIndex), validate(endIndex));
	}

	@Override
	public long length() {
		ensureOpen();
		return end - address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof UnsafeMappedFile)) return false;

		UnsafeMappedFile that = (UnsafeMappedFile) o;

		if (address != that.address) return false;
		return end == that.end;
	}

	@Override
	public int hashCode() {
		long address = this.address;
		int result = Long.hashCode(address);
		result = 31 * result + Long.hashCode((end - address));
		return result;
	}

	@Override
	public void close() {
		if (!cleaned) {
			synchronized (this) {
				if (cleaned)
					return;
				cleaned = true;
				Runnable deallocator = this.deallocator;
				if (deallocator != null)
					deallocator.run();
			}
		}
	}

	private void ensureOpen() {
		if (cleaned)
			throw new IllegalStateException("Cannot access data after close");
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	private long validate(long position) {
		if (position < 0L) {
			throw new IllegalArgumentException();
		}
		position += address;
		if (position > end) {
			throw new IllegalArgumentException(Long.toString(position));
		}
		return position;
	}

	private static int swap(int x) {
		if (SWAP)
			return Integer.reverseBytes(x);
		return x;
	}

	private static short swap(short x) {
		if (SWAP)
			return (short) (((x >> 8) & 0xFF) | (x << 8));
		return x;
	}
}
