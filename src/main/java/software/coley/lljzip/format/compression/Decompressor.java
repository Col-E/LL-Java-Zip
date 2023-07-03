package software.coley.lljzip.format.compression;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.util.ByteData;

import java.io.IOException;

/**
 * Outlines decompression of {@link LocalFileHeader#getFileData()}.
 *
 * @author Matt Coley
 */
public interface Decompressor {
	/**
	 * @param header
	 * 		Header containing the bytes, for any context needed.
	 * @param bytes
	 * 		Bytes to decompress.
	 *
	 * @return Decompressed bytes.
	 *
	 * @throws IOException
	 * 		Decompression failure.
	 */
	ByteData decompress(LocalFileHeader header, ByteData bytes) throws IOException;
}
