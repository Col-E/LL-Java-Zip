package software.coley.llzip.strategy;

import software.coley.llzip.part.LocalFileHeader;

import java.io.IOException;
import java.nio.ByteBuffer;

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
	ByteBuffer decompress(LocalFileHeader header, ByteBuffer bytes) throws IOException;
}
