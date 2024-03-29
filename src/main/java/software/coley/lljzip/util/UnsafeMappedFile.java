package software.coley.lljzip.util;

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mapped file backed by address & length.
 *
 * @author xDark
 */
final class UnsafeMappedFile implements ByteData {
	private static final boolean SWAP = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
	private static final Unsafe UNSAFE = UnsafeUtil.get();

	private final AtomicBoolean cleaned;
	private final long address;
	private final long end;
	private final Runnable deallocator;
	@SuppressWarnings("unused")
	private final Object attachment;

	private UnsafeMappedFile(Object attachment, long address, long end, AtomicBoolean cleaned) {
		this.attachment = attachment;
		this.address = address;
		this.end = end;
		this.cleaned = cleaned;
		deallocator = null;
	}

	UnsafeMappedFile(long address, long length, Runnable deallocator, AtomicBoolean cleaned) {
		this.address = address;
		this.end = address + length;
		this.deallocator = deallocator;
		this.cleaned = cleaned;
		attachment = null;
	}

	@Override
	public int getInt(long position) {
		ensureOpen();
		return swap(UNSAFE.getInt(validate(position)));
	}

	@Override
	public long getLong(long position) {
		ensureOpen();
		return swap(UNSAFE.getLong(validate(position)));
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
		return new UnsafeMappedFile(this, validate(startIndex), validate(endIndex), cleaned);
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
		if (!cleaned.get()) {
			synchronized (this) {
				if (cleaned.get())
					return;
				cleaned.set(true);
				Runnable deallocator = this.deallocator;
				if (deallocator != null)
					deallocator.run();
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

	private long validate(long position) {
		if (position < 0L) {
			throw new IllegalArgumentException();
		}
		position += address;
		if (position > end) {
			long diff = position - end;
			throw new IllegalArgumentException("positon beyond max bounds: " + position + " > " + end + " diff: " + diff);
		}
		return position;
	}

	private static long swap(long x) {
		if (SWAP)
			return Long.reverseBytes(x);
		return x;
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
