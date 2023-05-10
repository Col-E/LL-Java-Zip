package software.coley.llzip.format.model;

import software.coley.llzip.util.ByteData;

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
	void read(ByteData data, long offset);

	// TODO: Write conventions, then rename to 'ZipReadWrite' and tie into a new ZipWriterStrategy
	//        - similar to read, but with data-output-stream writes bytes to stream
	//        - obviously requires offset data to be correct for a 'valid' output
	//            - but still allows invalid output to facilitate crafting intentional malformed zips/jars
}
