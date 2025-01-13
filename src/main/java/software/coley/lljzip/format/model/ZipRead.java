package software.coley.lljzip.format.model;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;

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
	void read(@Nonnull MemorySegment data, long offset) throws ZipParseException;
}
