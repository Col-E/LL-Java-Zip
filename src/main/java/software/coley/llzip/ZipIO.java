package software.coley.llzip;

import software.coley.llzip.strategy.DefaultZipReaderStrategy;
import software.coley.llzip.strategy.JvmZipReaderStrategy;
import software.coley.llzip.strategy.ZipReaderStrategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * IO wrappers for reading {@link ZipArchive} contents.
 *
 * @author Matt Coley
 */
public class ZipIO {
	/**
	 * Creates an archive using the {@link DefaultZipReaderStrategy}.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readStandard(ByteBuffer data) throws IOException {
		return read(data, new DefaultZipReaderStrategy());
	}

	/**
	 * Creates an archive using the {@link DefaultZipReaderStrategy}.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readStandard(byte[] data) throws IOException {
		return read(data, new DefaultZipReaderStrategy());
	}
	/**
	 * Creates an archive using the {@link DefaultZipReaderStrategy}.
	 *
	 * @param data
	 * 		Zip path.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readStandard(Path data) throws IOException {
		return read(data, new DefaultZipReaderStrategy());
	}

	/**
	 * Creates an archive using the {@link JvmZipReaderStrategy} which handles some edge cases not usually
	 * expected from zip files.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readJvm(ByteBuffer data) throws IOException {
		return read(data, new JvmZipReaderStrategy());
	}

	/**
	 * Creates an archive using the {@link JvmZipReaderStrategy} which handles some edge cases not usually
	 * expected from zip files.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readJvm(byte[] data) throws IOException {
		return read(data, new JvmZipReaderStrategy());
	}

	/**
	 * Creates an archive using the {@link JvmZipReaderStrategy} which handles some edge cases not usually
	 * expected from zip files.
	 *
	 * @param path
	 * 		Zip path.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readJvm(Path path) throws IOException {
		return read(path, new JvmZipReaderStrategy());
	}

	/**
	 * @param data
	 * 		Zip bytes.
	 * @param strategy
	 * 		Zip reader implementation.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive read(ByteBuffer data, ZipReaderStrategy strategy) throws IOException {
		if (data == null)
			throw new IOException("Data is null!");
		// The fixed size elements of a CDFH is 22 bytes (plus the variable size bits which can be 0)
		if (data.remaining() < 22)
			throw new IOException("Not enough bytes to read Central-Directory-File-Header, minimum=22");
		// Create instance
		ZipArchive zip = new ZipArchive();
		strategy.read(zip, data);
		return zip;
	}

	/**
	 * @param data
	 * 		Zip bytes.
	 * @param strategy
	 * 		Zip reader implementation.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive read(byte[] data, ZipReaderStrategy strategy) throws IOException {
		if (data == null)
			throw new IOException("Data is null!");
		return read(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), strategy);
	}

	/**
	 * @param path
	 * 		Zip path.
	 * @param strategy
	 * 		Zip reader implementation.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive read(Path path, ZipReaderStrategy strategy) throws IOException {
		if (path == null)
			throw new IOException("Data is null!");
		if (!Files.isRegularFile(path))
			throw new FileNotFoundException(path.toString());
		ByteBuffer buffer;
		try (FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
			buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0L, fc.size());
		}
		return read(buffer.order(ByteOrder.LITTLE_ENDIAN), strategy);
	}
}
