package software.coley.llzip.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Byte array data.
 *
 * @author xDark
 */
public interface ByteData {

	/**
	 * Gets int at specific position.
	 *
	 * @param position
	 * 		Position to read int from.
	 *
	 * @return Read int.
	 */
	int getInt(long position);

	/**
	 * Gets short at specific position.
	 *
	 * @param position
	 * 		Position to short int from.
	 *
	 * @return Read short.
	 */
	short getShort(long position);

	/**
	 * Gets byte at specific position.
	 *
	 * @param position
	 * 		Position to read byte from.
	 *
	 * @return Read byte.
	 */
	byte get(long position);

	/**
	 * Copied array of bytes from position.
	 *
	 * @param position
	 * 		Position to copy from.
	 */
	void get(long position, byte[] b, int off, int len);

	/**
	 * Transfers data to target output stream.
	 *
	 * @param out
	 * 		Stream to transfer data to.
	 * @param buf
	 * 		Buffer to use for transferring.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	void transferTo(OutputStream out, byte[] buf) throws IOException;

	/**
	 * Makes a slice of data.
	 *
	 * @param startIndex
	 * 		Starting index.
	 * @param endIndex
	 * 		End index.
	 *
	 * @return Slice of this data.
	 */
	ByteData slice(long startIndex, long endIndex);

	/**
	 * Same as above, but with relative length.
	 *
	 * @param startIndex
	 * 		Starting index.
	 * @param length
	 * 		Slice length.
	 *
	 * @return Slice of this data.
	 */
	default ByteData sliceOf(long startIndex, long length) {
		return slice(startIndex, startIndex + length);
	}

	/**
	 * @return Length of the data.
	 */
	long length();
}
