package software.coley.lljzip.util;

import software.coley.lljzip.format.model.ZipPart;
import software.coley.lljzip.util.lazy.LazyMemorySegment;
import software.coley.lljzip.util.lazy.LazyInt;
import software.coley.lljzip.util.lazy.LazyLong;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * {@link MemorySegment} utils.
 *
 * @author Matt Coley
 */
public class MemorySegmentUtil {
	public static final int WILDCARD = Integer.MIN_VALUE;
	private static final ValueLayout.OfInt LITTLE_INT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private static final ValueLayout.OfShort LITTLE_SHORT = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

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
	public static long indexOfWord(MemorySegment data, long offset, int pattern) {
		if (offset < 0) return -1;
		long len = data.byteSize() - 2;
		for (long i = offset; i < len; i++) {
			if (pattern == (data.get(LITTLE_SHORT, i) & 0xFFFF))
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
	public static long indexOfQuad(MemorySegment data, long offset, int pattern) {
		if (offset < 0) return -1;
		long len = data.byteSize() - 4;
		long i = offset;
		while (i < len) {
			int value = data.get(LITTLE_INT, i);
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
	public static long lastIndexOfWord(MemorySegment data, long offset, int pattern) {
		long limit;
		if (offset < 0 || data == null || (limit = data.byteSize()) < 2 || offset >= limit)
			return -1;
		for (long i = offset; i >= 0; i--) {
			if (pattern == data.get(LITTLE_SHORT, i))
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
	public static long lastIndexOfQuad(MemorySegment data, long offset, int pattern) {
		long limit;
		if (offset < 0 || data == null || (limit = data.byteSize()) < 4 || offset >= limit)
			return -1;
		long i = offset;
		while (i >= 0) {
			int value = data.get(LITTLE_INT, i);
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
	public static long indexOf(MemorySegment data, int[] pattern) {
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
	public static long indexOf(MemorySegment data, long offset, int[] pattern) {
		// Remaining data must be as long as pattern and in bounds
		long limit;
		if (offset < 0 || data == null || (limit = data.byteSize()) < pattern.length || offset >= limit)
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
	public static long lastIndexOf(MemorySegment data, int[] pattern) {
		return lastIndexOf(data, (data.byteSize() - pattern.length), pattern);
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
	public static long lastIndexOf(MemorySegment data, long offset, int[] pattern) {
		// Remaining data must be as long as pattern and in bounds
		if (offset < 0 || data == null || data.byteSize() < pattern.length)
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
	public static boolean startsWith(MemorySegment data, long offset, int[] pattern) {
		// Remaining data must be as long as pattern and in the array bounds
		if (data == null || (data.byteSize() - offset) < pattern.length || offset < 0 || offset >= data.byteSize())
			return false;

		// Check for mis-match
		for (int i = 0; i < pattern.length; i++) {
			int p = pattern[i];
			if (p == WILDCARD)
				continue;
			if ((data.get(ValueLayout.JAVA_BYTE, offset + i) & 0xff) != p)
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
	public static int readWord(MemorySegment data, long i) {
		return data.get(LITTLE_SHORT, i) & 0xFFFF;
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param i
	 * 		Index to read quad from.
	 *
	 * @return Value of quad.
	 */
	public static int readQuad(MemorySegment data, long i) {
		return data.get(LITTLE_INT, i);
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
	public static String readString(MemorySegment data, long start, long len) {
		if (len == 0)
			return "";
		byte[] bytes = data.asSlice(start, len).toArray(ValueLayout.JAVA_BYTE);
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
	public static byte[] readArray(MemorySegment data, long start, long len) {
		return data.asSlice(start, len).toArray(ValueLayout.JAVA_BYTE);
	}

	/**
	 * @param data
	 * 		Content to get bytes from.
	 *
	 * @return Buffer as byte array.
	 */
	public static byte[] toByteArray(MemorySegment data) {
		long length = data.byteSize();
		if (length > Integer.MAX_VALUE - 8) {
			throw new IllegalStateException("Data too big!");
		}
		return data.toArray(ValueLayout.JAVA_BYTE);
	}

	/**
	 * @param data
	 * 		Content to convert to string to.
	 *
	 * @return Buffer as a string.
	 */
	public static String toString(MemorySegment data) {
		return new String(toByteArray(data), StandardCharsets.UTF_8);
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
	public static LazyInt readLazyWord(MemorySegment data, long headerOffset, int localOffset) {
		return new LazyInt(() -> {
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
	public static LazyInt readLazyQuad(MemorySegment data, long headerOffset, int localOffset) {
		return new LazyInt(() -> {
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
	public static LazyInt readLazyMaskedQuad(MemorySegment data, long headerOffset, int localOffset) {
		return new LazyInt(() -> {
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
	public static LazyLong readLazyLongWord(MemorySegment data, long headerOffset, int localOffset) {
		return new LazyLong(() -> {
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
	public static LazyLong readLazyMaskedLongQuad(MemorySegment data, long headerOffset, int localOffset) {
		return new LazyLong(() -> {
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
	public static LazyMemorySegment readLazySlice(MemorySegment data, long headerOffset, LazyInt localOffset, LazyInt length) {
		return new LazyMemorySegment(() -> {
			return data.asSlice(headerOffset + localOffset.get(), length.get());
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
	public static LazyMemorySegment readLazyLongSlice(MemorySegment data, long headerOffset, LazyInt localOffset, LazyLong length) {
		return new LazyMemorySegment(() -> {
			return data.asSlice(headerOffset + localOffset.get(), length.get());
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
	public static LazyMemorySegment readLazyLongSlice(MemorySegment data, long headerOffset, LazyLong localOffset, LazyLong length) {
		return new LazyMemorySegment(() -> {
			return data.asSlice(headerOffset + localOffset.get(), length.get());
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
	public static LazyMemorySegment readLazyLongSlice(MemorySegment data, long headerOffset, LazyInt localOffset, long length) {
		return new LazyMemorySegment(() -> {
			return data.asSlice(headerOffset + localOffset.get(), length);
		});
	}
}
