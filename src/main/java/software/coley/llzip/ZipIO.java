package software.coley.llzip;

import software.coley.llzip.format.read.ForwardScanZipReaderStrategy;
import software.coley.llzip.format.read.JvmZipReaderStrategy;
import software.coley.llzip.format.read.ZipReaderStrategy;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.BufferData;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.FileMapUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IO wrappers for reading {@link ZipArchive} contents.
 *
 * @author Matt Coley
 */
public class ZipIO {
	/**
	 * Creates an archive using the {@link ForwardScanZipReaderStrategy}.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readStandard(ByteData data) throws IOException {
		return read(data, new ForwardScanZipReaderStrategy());
	}

	/**
	 * Creates an archive using the {@link ForwardScanZipReaderStrategy}.
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
		return read(data, new ForwardScanZipReaderStrategy());
	}

	/**
	 * Creates an archive using the {@link ForwardScanZipReaderStrategy}.
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
		return read(data, new ForwardScanZipReaderStrategy());
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
	public static ZipArchive readJvm(ByteData data) throws IOException {
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
	public static ZipArchive read(ByteData data, ZipReaderStrategy strategy) throws IOException {
		if (data == null)
			throw new IOException("Data is null!");
		// The fixed size elements of a CDFH is 22 bytes (plus the variable size bits which can be 0)
		if (data.length() < 22)
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
		return read(BufferData.wrap(data), strategy);
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
		return read(FileMapUtil.map(path), strategy);
	}
}
