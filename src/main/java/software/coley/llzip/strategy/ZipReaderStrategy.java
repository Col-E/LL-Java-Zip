package software.coley.llzip.strategy;

import software.coley.llzip.ZipArchive;

import java.io.IOException;
import java.nio.ByteBuffer;

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
	void read(ZipArchive zip, ByteBuffer buffer) throws IOException;
}
