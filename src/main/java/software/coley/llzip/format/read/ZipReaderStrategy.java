package software.coley.llzip.format.read;

import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.ByteData;

import java.io.IOException;

/**
 * Outlines reading binary data into a ZIP data type.
 *
 * @author Matt Coley
 */
public interface ZipReaderStrategy {
	/**
	 * @param zip
	 * 		Archive to read into.
	 * @param buffer
	 * 		Data to read.
	 *
	 * @throws IOException
	 * 		When the data cannot be read <i>(EOF, not matching expectations, etc)</i>
	 */
	void read(ZipArchive zip, ByteData buffer) throws IOException;

	default void postProcessLocalFileHeader(LocalFileHeader file) {
		// no-op by default
	}
}
