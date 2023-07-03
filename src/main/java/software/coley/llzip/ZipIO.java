package software.coley.llzip;

import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.format.read.ForwardScanZipReader;
import software.coley.llzip.format.read.JvmZipReader;
import software.coley.llzip.format.read.NaiveLocalFileZipReader;
import software.coley.llzip.format.read.ZipReader;
import software.coley.llzip.util.BufferData;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.FileMapUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IO wrappers for reading {@link ZipArchive} contents.
 * <ul>
 *     <li>For JAR files or anything intended to be read by the JVM use the JVM operations which use {@link JvmZipReader}.</li>
 *     <li>For regular ZIP files use {@link ForwardScanZipReader}.</li>
 *     <li>For ZIP files without {@link CentralDirectoryFileHeader} or {@link EndOfCentralDirectory} items, use {@link NaiveLocalFileZipReader}</li>
 * </ul>
 * You can fully control zip parsing via {@link #read(ByteData, ZipReader)} by passing a customized reader implementation.
 *
 * @author Matt Coley
 */
public class ZipIO {
	/**
	 * Creates an archive using the {@link ForwardScanZipReader}.
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
		return read(data, new ForwardScanZipReader());
	}

	/**
	 * Creates an archive using the {@link ForwardScanZipReader}.
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
		return read(data, new ForwardScanZipReader());
	}

	/**
	 * Creates an archive using the {@link ForwardScanZipReader}.
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
		return read(data, new ForwardScanZipReader());
	}

	/**
	 * Creates an archive using the {@link NaiveLocalFileZipReader}.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readNaive(ByteData data) throws IOException {
		return read(data, new NaiveLocalFileZipReader());
	}

	/**
	 * Creates an archive using the {@link NaiveLocalFileZipReader}.
	 *
	 * @param data
	 * 		Zip bytes.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readNaive(byte[] data) throws IOException {
		return read(data, new NaiveLocalFileZipReader());
	}

	/**
	 * Creates an archive using the {@link NaiveLocalFileZipReader}.
	 *
	 * @param data
	 * 		Zip path.
	 *
	 * @return Archive from bytes.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readNaive(Path data) throws IOException {
		return read(data, new NaiveLocalFileZipReader());
	}

	/**
	 * Creates an archive using the {@link JvmZipReader} which handles some edge cases not usually
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
		return read(data, new JvmZipReader());
	}

	/**
	 * Creates an archive using the {@link JvmZipReader} which handles some edge cases not usually
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
		return read(data, new JvmZipReader());
	}

	/**
	 * Creates an archive using the {@link JvmZipReader} which handles some edge cases not usually
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
		return read(path, new JvmZipReader());
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
	public static ZipArchive read(byte[] data, ZipReader strategy) throws IOException {
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
	public static ZipArchive read(Path path, ZipReader strategy) throws IOException {
		if (path == null)
			throw new IOException("Data is null!");
		if (!Files.isRegularFile(path))
			throw new FileNotFoundException(path.toString());
		return read(FileMapUtil.map(path), strategy);
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
	public static ZipArchive read(ByteData data, ZipReader strategy) throws IOException {
		if (data == null)
			throw new IOException("Data is null!");

		// The fixed size elements of a CDFH is 22 bytes (plus the variable size bits which can be 0)
		// - Even if we only want to read local/central file entries, those are even larger at a minimum
		if (data.length() < 22)
			throw new IOException("Not enough bytes to read Central-Directory-File-Header, minimum=22");

		// Create instance
		ZipArchive zip = new ZipArchive(data);
		strategy.read(zip, data);
		return zip;
	}
}
