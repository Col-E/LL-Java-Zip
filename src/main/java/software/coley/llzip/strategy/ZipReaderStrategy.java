package software.coley.llzip.strategy;

import software.coley.llzip.ZipArchive;

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
	 * @param data
	 * 		Data to read.
	 *
	 * @throws IOException
	 * 		When the data cannot be read <i>(EOF, not matching expectations, etc)</i>
	 */
	void read(ZipArchive zip, byte[] data) throws IOException;
}
