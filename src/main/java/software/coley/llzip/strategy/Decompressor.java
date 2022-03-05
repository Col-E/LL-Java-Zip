package software.coley.llzip.strategy;

import software.coley.llzip.part.LocalFileHeader;

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
	byte[] decompress(LocalFileHeader header, byte[] bytes) throws IOException;
}
