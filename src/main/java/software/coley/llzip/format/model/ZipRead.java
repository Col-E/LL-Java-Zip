package software.coley.llzip.format.model;

import software.coley.llzip.util.ByteData;

import javax.annotation.Nonnull;

/**
 * IO operations for children of {@link ZipPart}.
 *
 * @author Matt Coley
 */
public interface ZipRead {
	/**
	 * @param data
	 * 		Data to read from.
	 * @param offset
	 * 		Initial offset in data to start at.
	 */
	void read(@Nonnull ByteData data, long offset);
}
