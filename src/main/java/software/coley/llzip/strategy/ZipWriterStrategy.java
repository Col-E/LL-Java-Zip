package software.coley.llzip.strategy;

import software.coley.llzip.ZipArchive;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Outlines writing the {@link ZipArchive} back to binary data.
 *
 * @author Matt Coley
 */
public interface ZipWriterStrategy {
	/**
	 * Writes the given archive to the stream.
	 *
	 * @param archive
	 * 		Archive to write.
	 * @param os
	 * 		Stream to write to.
	 *
	 * @throws IOException
	 * 		When writing the archive failed.
	 */
	void write(ZipArchive archive, OutputStream os) throws IOException;

	/**
	 * Convenience call to {@link #write(ZipArchive, OutputStream)} that writes directly to a file path.
	 *
	 * @param archive
	 * 		Archive to write.
	 * @param path
	 * 		File to write to.
	 *
	 * @throws IOException
	 * 		When writing the archive failed, or the given path cannot be written to.
	 */
	default void writeToDisk(ZipArchive archive, Path path) throws IOException {
		try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
			write(archive, fos);
		}
	}

	/**
	 * Convenience call to {@link #write(ZipArchive, OutputStream)} that yields {@code byte[]}.
	 *
	 * @param archive
	 * 		Archive to write.
	 *
	 * @return Bytes of the written archive.
	 *
	 * @throws IOException
	 * 		When writing the archive failed.
	 */
	default byte[] writeToByteArray(ZipArchive archive) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		write(archive, baos);
		return baos.toByteArray();
	}
}
