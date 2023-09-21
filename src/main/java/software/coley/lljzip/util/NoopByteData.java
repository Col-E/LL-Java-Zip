package software.coley.lljzip.util;

import java.io.OutputStream;

/**
 * Empty file that yields {@code 0} for everything.
 *
 * @author Matt Coley
 */
public class NoopByteData implements ByteData {
	/**
	 * Shared instance.
	 */
	public static final NoopByteData INSTANCE = new NoopByteData();

	private NoopByteData() {
		// deny construction
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public int getInt(long position) {
		return 0;
	}

	@Override
	public long getLong(long position) {
		return 0;
	}

	@Override
	public short getShort(long position) {
		return 0;
	}

	@Override
	public byte get(long position) {
		return 0;
	}

	@Override
	public void get(long position, byte[] buffer, int off, int len) {
	}

	@Override
	public void transferTo(OutputStream out, byte[] buffer) {
	}

	@Override
	public ByteData slice(long startIndex, long endIndex) {
		return this;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public void close() {
	}
}
