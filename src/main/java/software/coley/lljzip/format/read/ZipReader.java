package software.coley.lljzip.format.read;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.foreign.MemorySegment;

/**
 * Outlines reading binary data into a ZIP data type.
 *
 * @author Matt Coley
 */
public interface ZipReader {
	/**
	 * @param zip
	 * 		Archive to read into.
	 * @param data
	 * 		Data to read.
	 *
	 * @throws IOException
	 * 		When the data cannot be read <i>(EOF, not matching expectations, etc)</i>
	 */
	void read(@Nonnull ZipArchive zip, @Nonnull MemorySegment data) throws IOException;

	/**
	 * @param file File to post-process.
	 */
	default void postProcessLocalFileHeader(@Nonnull LocalFileHeader file) {
		// no-op by default
	}
}
