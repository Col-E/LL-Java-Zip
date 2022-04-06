package software.coley.llzip.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Buffer utils.
 *
 * @author Matt Coley
 */
public class Buffers {
	public static final int WILDCARD = Integer.MIN_VALUE;

	/**
	 * @param buffer
	 * 		Content to search.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static int indexOf(ByteBuffer buffer, int[] pattern) {
		return indexOf(buffer, 0, pattern);
	}

	/**
	 * @param buffer
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static int indexOf(ByteBuffer buffer, int offset, int[] pattern) {
		// Remaining data must be as long as pattern
		int limit;
		if (buffer == null || (limit = buffer.limit()) < pattern.length || offset >= buffer.limit())
			return -1;
		// Search from offset going forwards
		for (int i = offset; i < limit; i++)
			if (startsWith(buffer, i, pattern))
				return i;
		// Not found
		return -1;
	}

	/**
	 * @param buffer
	 * 		Content to search.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static int lastIndexOf(ByteBuffer buffer, int[] pattern) {
		return lastIndexOf(buffer, (buffer.limit() - pattern.length), pattern);
	}

	/**
	 * @param buffer
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static int lastIndexOf(ByteBuffer buffer, int offset, int[] pattern) {
		// Remaining data must be as long as pattern
		if (buffer == null || buffer.limit() < pattern.length)
			return -1;
		// Search from offset going backwards
		for (int i = offset; i >= 0; i--)
			if (startsWith(buffer, i, pattern))
				return i;
		// Not found
		return -1;
	}

	/**
	 * @param array
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin check at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return {@code true} when the content of the array at the offset matches the pattern.
	 */
	public static boolean startsWith(ByteBuffer array, int offset, int[] pattern) {
		// Remaining data must be as long as pattern and in the array bounds
		if (array == null || (array.limit() - offset) < pattern.length || offset < 0 || offset >= array.limit())
			return false;
		// Check for mis-match
		for (int i = 0; i < pattern.length; i++) {
			int p = pattern[i];
			if (p == WILDCARD)
				continue;
			if (array.get(offset + i) != p)
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
	public static int readWord(ByteBuffer data, int i) {
		return data.getShort(i);
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param i
	 * 		Index to read quad from.
	 *
	 * @return Value of quad.
	 */
	public static int readQuad(ByteBuffer data, int i) {
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
	public static String readString(ByteBuffer data, int start, int len) {
		if (len == 0)
			return "";
		ByteBuffer slice = slice(data, start, len);
		byte[] bytes = new byte[len];
		slice.get(bytes);
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
	public static byte[] readArray(ByteBuffer data, int start, int len) {
		ByteBuffer slice = slice(data, start, len);
		byte[] bytes = new byte[len];
		slice.get(bytes);
		return bytes;
	}

	/**
	 *
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
		slice = ((ByteBuffer) slice.position(start)).slice();
		slice.limit(end - start);
		return slice.order(data.order());
	}

	/**
	 *
	 * @param data
	 * 		Content to make slice of.
	 * @param start
	 * 		Start index.
	 * @param len
	 * 		Length of content to make slice of.
	 *	
	 * @return Buffer slice.
	 */
	public static ByteBuffer slice(ByteBuffer data, int start, int len) {
		ByteBuffer slice = data.slice();
		slice = ((ByteBuffer) slice.position(start)).slice();
		slice.limit(len);
		return slice.order(data.order());
	}

	/**
	 *
	 * @param data
	 * 		Content to make slice of.
	 *
	 * @return Buffer slice.
	 */
	public static ByteBuffer slice(ByteBuffer data) {
		return (ByteBuffer) data.slice().order(data.order());
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
	public static byte[] toByteArray(ByteBuffer data) {
		ByteBuffer slice = slice(data);
		byte[] bytes = new byte[length(slice)];
		slice.get(bytes);
		return bytes;
	}

	/**
	 * @param data
	 * 		Content to convert to string to.
	 *
	 * @return Buffer as a string.
	 */
	public static String toString(ByteBuffer data) {
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
	public static boolean equals(ByteBuffer a, ByteBuffer b) {
		return Objects.equals(a, b);
	}
}
