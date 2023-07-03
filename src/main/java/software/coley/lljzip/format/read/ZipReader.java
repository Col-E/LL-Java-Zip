package software.coley.lljzip.format.read;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ByteData;

import javax.annotation.Nonnull;
import java.io.IOException;

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
	void read(@Nonnull ZipArchive zip, @Nonnull ByteData data) throws IOException;

	/**
	 * @param file File to post-process.
	 */
	default void postProcessLocalFileHeader(@Nonnull LocalFileHeader file) {
		// no-op by default
	}
}
