package software.coley.llzip.util;

import java.util.Arrays;

/**
 * Array utils.
 *
 * @author Matt Coley
 */
public class Array {
	public static final int WILDCARD = Integer.MIN_VALUE;

	/**
	 * @param array
	 * 		Content to search.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static int indexOf(byte[] array, int[] pattern) {
		return indexOf(array, 0, pattern);
	}

	/**
	 * @param array
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return First index of pattern in content, or {@code -1} for no match.
	 */
	public static int indexOf(byte[] array, int offset, int[] pattern) {
		// Remaining data must be as long as pattern
		if (array == null || array.length < pattern.length || offset >= array.length)
			return -1;
		// Search from offset going forwards
		for (int i = offset; i < array.length; i++)
			if (startsWith(array, i, pattern))
				return i;
		// Not found
		return -1;
	}

	/**
	 * @param array
	 * 		Content to search.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static int lastIndexOf(byte[] array, int[] pattern) {
		return lastIndexOf(array, (array.length - pattern.length), pattern);
	}

	/**
	 * @param array
	 * 		Content to search.
	 * @param offset
	 * 		Offset to begin search at.
	 * @param pattern
	 * 		Pattern to match.
	 *
	 * @return Last index of pattern in content, or {@code -1} for no match.
	 */
	public static int lastIndexOf(byte[] array, int offset, int[] pattern) {
		// Remaining data must be as long as pattern
		if (array == null || array.length < pattern.length)
			return -1;
		// Search from offset going backwards
		for (int i = offset; i >= 0; i--)
			if (startsWith(array, i, pattern))
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
	public static boolean startsWith(byte[] array, int offset, int[] pattern) {
		// Remaining data must be as long as pattern and in the array bounds
		if (array == null || (array.length - offset) < pattern.length || offset < 0 || offset >= array.length)
			return false;
		// Check for mis-match
		for (int i = 0; i < pattern.length; i++) {
			int p = pattern[i];
			if (p == WILDCARD)
				continue;
			if (array[offset + i] != p)
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
	public static int readWord(byte[] data, int i) {
		int ch1 = 0xFF & data[i + 1];
		int ch2 = 0xFF & data[i];
		return (ch1 << 8) + ch2;
	}

	/**
	 * @param data
	 * 		Content to read from.
	 * @param i
	 * 		Index to read quad from.
	 *
	 * @return Value of quad.
	 */
	public static int readQuad(byte[] data, int i) {
		int ch1 = 0xFF & data[i + 3];
		int ch2 = 0xFF & data[i + 2];
		int ch3 = 0xFF & data[i + 1];
		int ch4 = 0xFF & data[i];
		return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
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
	public static String readString(byte[] data, int start, int len) {
		if (len == 0)
			return "";
		return new String(data, start, len);
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
	public static byte[] readArray(byte[] data, int start, int len) {
		return Arrays.copyOfRange(data, start, start + len);
	}
}
