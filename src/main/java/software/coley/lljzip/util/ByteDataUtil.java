package software.coley.lljzip.util;

import software.coley.lljzip.format.model.ZipPart;
import software.coley.lljzip.util.lazy.LazyByteData;
import software.coley.lljzip.util.lazy.LazyInt;
import software.coley.lljzip.util.lazy.LazyLong;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Buffer utils.
 *
 * @author Matt Coley
 */
public class ByteDataUtil {
	public static final int WILDCARD = Integer.MIN_VALUE;

	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static long indexOfWord(ByteData data, long offset, int pattern) {
		long len = data.length() - 2;
		for (long i = offset; i < len; i++) {
			if (pattern == data.getShort(i))
				return i;
		}
		return -1;
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static long indexOfQuad(ByteData data, long offset, int pattern) {
		long len = data.length() - 4;
		long i = offset;
		while (i < len) {
			int value = data.getInt(i);
			if (pattern == value)
				return i;

			// Move i forwards as far as possible, where we can assume that distance
			// will never contain a match.
			if ((pattern & 0xFF_FF_FF) == ((value & 0xFF_FF_FF_00) >>> 8)) {
				i += 1;
			} else if ((pattern & 0xFF_FF) == ((value & 0xFF_FF_00_00) >>> 16)) {
				i += 2;
			} else if ((pattern & 0xFF) == ((value & 0xFF_00_00_00)) >>> 24) {
				i += 3;
			} else {
				i += 4;
			}
		}
		return -1;
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static long lastIndexOfWord(ByteData data, long offset, int pattern) {
		long limit;
		if (data == null || (limit = data.length()) < 2 || offset >= limit)
			return -1;
		for (long i = offset; i >= 0; i--) {
			if (pattern == data.getShort(i))
				return i;
		}
		return -1;
	}


	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static long lastIndexOfQuad(ByteData data, long offset, int pattern) {
		long limit;
		if (data == null || (limit = data.length()) < 4 || offset >= limit)
			return -1;
		long i = offset;
		while (i >= 0) {
			int value = data.getInt(i);
			if (pattern == value)
				return i;

			// Move i backwards as far back as possible, where we can assume that distance
			// will never contain a match.
			if (((pattern & 0xFF_FF_FF_00) >>> 8) == (value & 0x00_FF_FF_FF)) {
				i -= 1;
			} else if (((pattern & 0xFF_FF_00_00) >>> 16) == (value & 0x00_00_FF_FF)) {
				i -= 2;
			} else if (((pattern & 0xFF_FF_FF_00) >>> 24) == (value & 0x00_00_00_FF)) {
				i -= 3;
			} else {
				i -= 4;
			}
		}
		return -1;
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static long indexOf(ByteData data, int[] pattern) {
		return indexOf(data, 0, pattern);
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static long indexOf(ByteData data, long offset, int[] pattern) {
		// Remaining data must be as long as pattern
		long limit;
		if (data == null || (limit = data.length()) < pattern.length || offset >= limit)
			return -1;

		// Search from offset going forwards
		for (long i = offset; i < limit; i++)
			if (startsWith(data, i, pattern))
				return i;

		// Not found
		return -1;
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static long lastIndexOf(ByteData data, int[] pattern) {
		return lastIndexOf(data, (data.length() - pattern.length), pattern);
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static long lastIndexOf(ByteData data, long offset, int[] pattern) {
		// Remaining data must be as long as pattern
		if (data == null || data.length() < pattern.length)
			return -1;

		// Search from offset going backwards
		for (long i = offset; i >= 0; i--)
			if (startsWith(data, i, pattern))
				return i;

		// Not found
		return -1;
	}

	/**
	 * @param data
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin check at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return {@code true} when the content of the array at the offset matches the pattern.
	 */
	public static boolean startsWith(ByteData data, long offset, int[] pattern) {
		// Remaining data must be as long as pattern and in the array bounds
		if (data == null || (data.length() - offset) < pattern.length || offset < 0 || offset >= data.length())
			return false;

		// Check for mis-match
		for (int i = 0; i < pattern.length; i++) {
			int p = pattern[i];
			if (p == WILDCARD)
				continue;
			if (data.get(offset + i) != p)
				return false;
		}

		// No mis-match, array starts with pattern
		return true;
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param i
	 * 		Index to read word from.
	 *
	 * @return Value of word.
	 */
	public static int readWord(ByteData data, long i) {
		return data.getShort(i) & 0xFFFF;
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param i
	 * 		Index to read quad from.
	 *
	 * @return Value of quad.
	 */
	public static int readQuad(ByteData data, long i) {
		return data.getInt(i);
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param start
	 * 		Start position of string.
	 * @param len
	 * 		Length of string.
	 *
	 * @return Value of string.
	 */
	public static String readString(ByteData data, long start, int len) {
		if (len == 0)
			return "";
		byte[] bytes = new byte[len];
		data.get(start, bytes, 0, len);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param start
	 * 		Start index.
	 * @param len
	 * 		Length of content to copy.
	 *
	 * @return Copy of range.
	 */
	public static byte[] readArray(ByteData data, int start, int len) {
		byte[] bytes = new byte[len];
		data.get(start, bytes, 0, len);
		return bytes;
	}

	/**
	 * @param data
	 * 		Content to make slice of.
	 * @param start
	 * 		Start index.
	 * @param end
	 * 		Endex index.
	 *
	 * @return Buffer slice.
	 */
	public static ByteBuffer sliceExact(ByteBuffer data, int start, int end) {
		ByteBuffer slice = data.slice();
		slice = ((ByteBuffer) ((Buffer)slice).position(start)).slice();
		((Buffer)slice).limit(end - start);
		return slice.order(data.order());
	}

	/**
	 * @param data
	 * 		Content to make slice of.
	 * @param start
	 * 		Start index.
	 * @param len
	 * 		Length of content to make slice of.
	 *
	 * @return Buffer slice.
	 */
	public static ByteData slice(ByteData data, long start, long len) {
		return data.slice(start, start + len);
	}

	/**
	 * @param data
	 * 		Content to get length of.
	 *
	 * @return Buffer length.
	 */
	public static int length(ByteBuffer data) {
		return data.remaining() - data.position();
	}

	/**
	 * @param data
	 * 		Content to get bytes from.
	 *
	 * @return Buffer as byte array.
	 */
	public static byte[] toByteArray(ByteData data) {
		long length = data.length();
		if (length > Integer.MAX_VALUE - 8) {
			throw new IllegalStateException("Data too big!");
		}
		byte[] bytes = new byte[(int) length];
		data.get(0L, bytes, 0, bytes.length);
		return bytes;
	}

	/**
	 * @param data
	 * 		Content to convert to string to.
	 *
	 * @return Buffer as a string.
	 */
	public static String toString(ByteData data) {
		return new String(toByteArray(data), StandardCharsets.UTF_8);
	}

	/**
	 * @param a
	 * 		First buffer.
	 * @param b
	 * 		Second buffer.
	 *
	 * @return {@code true} if buffers are equal.
	 */
	public static boolean equals(ByteData a, ByteData b) {
		return Objects.equals(a, b);
	}

	/**
	 * @param data
	 * 		Content to get word from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated word.
	 */
	public static LazyInt readLazyWord(ByteData data, long headerOffset, int localOffset) {
		return new LazyInt(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return readWord(data, headerOffset + localOffset);
		});
	}

	/**
	 * @param data
	 * 		Content to get quad from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated quad.
	 */
	public static LazyInt readLazyQuad(ByteData data, long headerOffset, int localOffset) {
		return new LazyInt(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return readQuad(data, headerOffset + localOffset);
		});
	}

	/**
	 * @param data
	 * 		Content to get masked quad from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated masked quad.
	 */
	public static LazyInt readLazyMaskedQuad(ByteData data, long headerOffset, int localOffset) {
		return new LazyInt(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return readQuad(data, headerOffset + localOffset) & 0xFFFF;
		});
	}

	/**
	 * @param data
	 * 		Content to get long word from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated long word.
	 */
	public static LazyLong readLazyLongWord(ByteData data, long headerOffset, int localOffset) {
		return new LazyLong(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return readWord(data, headerOffset + localOffset);
		});
	}

	/**
	 * @param data
	 * 		Content to get masked long quad from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated masked long quad.
	 */
	public static LazyLong readLazyMaskedLongQuad(ByteData data, long headerOffset, int localOffset) {
		return new LazyLong(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return readQuad(data, headerOffset + localOffset) & 0xFFFFFFFFL;
		});
	}

	/**
	 * @param data
	 * 		Content to get slice from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated slice.
	 */
	public static LazyByteData readLazySlice(ByteData data, long headerOffset, LazyInt localOffset, LazyInt length) {
		return new LazyByteData(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return data.sliceOf(headerOffset + localOffset.get(), length.get());
		});
	}

	/**
	 * @param data
	 * 		Content to get long slice from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated long slice.
	 */
	public static LazyByteData readLazyLongSlice(ByteData data, long headerOffset, LazyInt localOffset, LazyLong length) {
		return new LazyByteData(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return data.sliceOf(headerOffset + localOffset.get(), length.get());
		});
	}

	/**
	 * @param data
	 * 		Content to get long slice from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated long slice.
	 */
	public static LazyByteData readLazyLongSlice(ByteData data, long headerOffset, LazyLong localOffset, LazyLong length) {
		return new LazyByteData(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return data.sliceOf(headerOffset + localOffset.get(), length.get());
		});
	}

	/**
	 * @param data
	 * 		Content to get long slice from.
	 * @param headerOffset
	 * 		Offset of {@link ZipPart} header.
	 * @param localOffset
	 * 		Local offset from the header offset.
	 *
	 * @return Lazily populated long slice.
	 */
	public static LazyByteData readLazyLongSlice(ByteData data, long headerOffset, LazyInt localOffset, long length) {
		return new LazyByteData(() -> {
			if (data.isClosed())
				throw new IllegalStateException("Cannot read from closed data source");
			return data.sliceOf(headerOffset + localOffset.get(), length);
		});
	}
}
