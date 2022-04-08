package software.coley.llzip.util;

/**
 * Byte array data.
 *
 * @author xDark
 */
public interface ByteData {

	int getInt(long position);

	short getShort(long position);

	byte get(long position);

	void get(long position, byte[] b, int off, int len);

	ByteData slice(long startIndex, long endIndex);

	default ByteData sliceOf(long startIndex, long length) {
		return slice(startIndex, startIndex + length);
	}

	long length();
}
