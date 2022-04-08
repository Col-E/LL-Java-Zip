package software.coley.llzip.util;

import sun.misc.Unsafe;

import java.nio.ByteOrder;

/**
 * Mapped file backed by address & length.
 *
 * @author xDark
 */
final class UnsafeMappedFile implements ByteData {

	private static final boolean SWAP = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN;
	private static final Unsafe UNSAFE = UnsafeUtil.get();

	private final long address, end;
	private final Runnable deallocator;
	private Object attachment;

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
	}

	@Override
	public int getInt(long position) {
		return swap(UNSAFE.getInt(validate(position)));
	}

	@Override
	public short getShort(long position) {
		return swap(UNSAFE.getShort(validate(position)));
	}

	@Override
	public byte get(long position) {
		return UNSAFE.getByte(validate(position));
	}

	@Override
	public void get(long position, byte[] b, int off, int len) {
		long address = validate(position);
		if (address + len >= end)
			throw new IllegalArgumentException();
		UNSAFE.copyMemory(null, address, b, Unsafe.ARRAY_BYTE_BASE_OFFSET + off, len);
	}

	@Override
	public ByteData slice(long startIndex, long endIndex) {
		if (startIndex > endIndex)
			throw new IllegalArgumentException();
		return new UnsafeMappedFile(this, validate(startIndex), validate(endIndex));
	}

	@Override
	public long length() {
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

	@SuppressWarnings("deprecation")
	@Override
	protected void finalize() throws Throwable {
		Runnable deallocator = this.deallocator;
		if (deallocator != null)
			try {
				deallocator.run();
			} finally {
				super.finalize();
			}
	}

	private long validate(long position) {
		if (position < 0L) {
			throw new IllegalArgumentException();
		}
		if (position >= end) {
			throw new IllegalArgumentException(Long.toString(position));
		}
		return address + position;
	}

	private static int swap(int ч) {
		if (SWAP)
			return Integer.reverseBytes(ч);
		return ч;
	}

	private static short swap(short x) {
		if (SWAP)
			return (short) (((x >> 8) & 0xFF) | (x << 8));
		return x;
	}
}
