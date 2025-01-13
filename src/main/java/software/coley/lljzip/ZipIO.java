package software.coley.lljzip;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.AdaptingZipReader;
import software.coley.lljzip.format.read.ForwardScanZipReader;
import software.coley.lljzip.format.read.JvmZipReader;
import software.coley.lljzip.format.read.NaiveLocalFileZipReader;
import software.coley.lljzip.format.read.ZipReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/**
 * IO wrappers for reading {@link ZipArchive} contents.
 * <ul>
 *     <li>For JAR files or anything intended to be read by the JVM use the JVM operations which use {@link JvmZipReader}.</li>
 *     <li>For regular ZIP files use {@link ForwardScanZipReader}.</li>
 *     <li>For ZIP files without {@link CentralDirectoryFileHeader} or {@link EndOfCentralDirectory} items, use {@link NaiveLocalFileZipReader}</li>
 * </ul>
 * You can fully control zip parsing via {@link #read(MemorySegment, ZipReader)} by passing a customized reader implementation.
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
	public static ZipArchive readStandard(MemorySegment data) throws IOException {
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
	 * @return Archive from path.
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
	public static ZipArchive readNaive(MemorySegment data) throws IOException {
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
	 * @return Archive from path.
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
	public static ZipArchive readJvm(MemorySegment data) throws IOException {
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
	 * @return Archive from path.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive readJvm(Path path) throws IOException {
		return read(path, new JvmZipReader());
	}

	/**
	 * Creates an archive using the {@link AdaptingZipReader} which delegates work to {@link ZipFile}.
	 *
	 * @param path
	 * 		Zip path.
	 *
	 * @return Archive from path.
	 *
	 * @throws IOException
	 * 		When the archive cannot be read.
	 */
	public static ZipArchive readAdaptingIO(Path path) throws IOException {
		ZipArchive archive = new ZipArchive();
		AdaptingZipReader.fill(archive, path.toFile());
		return archive;
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
		return read(MemorySegment.ofArray(data), strategy);
	}

	/**
	 * @param path
	 * 		Zip path.
	 * @param strategy
	 * 		Zip reader implementation.
	 *
	 * @return Archive from path.
	 *
	 * @throws IOException
	 * 		When the archive bytes cannot be read from, usually indicating a malformed zip.
	 */
	public static ZipArchive read(Path path, ZipReader strategy) throws IOException {
		if (path == null)
			throw new IOException("Data is null!");
		if (!Files.isRegularFile(path))
			throw new FileNotFoundException(path.toString());
		FileChannel fc = FileChannel.open(path);
		try {
			long size = fc.size();
			// The fixed size elements of a CDFH is 22 bytes (plus the variable size bits which can be 0)
			// - Even if we only want to read local/central file entries, those are even larger at a minimum
			if (size < 22)
				throw new IOException("Not enough bytes to read Central-Directory-File-Header, minimum=22");

			ZipArchive zip = new ZipArchive(fc);
			strategy.read(zip, fc.map(FileChannel.MapMode.READ_ONLY, 0L, size, Arena.ofAuto()));
			fc = null;
			return zip;
		} finally {
			if (fc != null) {
				fc.close();
			}
		}
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
	public static ZipArchive read(MemorySegment data, ZipReader strategy) throws IOException {
		if (data == null)
			throw new IOException("Data is null!");

		// The fixed size elements of a CDFH is 22 bytes (plus the variable size bits which can be 0)
		// - Even if we only want to read local/central file entries, those are even larger at a minimum
		if (data.byteSize() < 22)
			throw new IOException("Not enough bytes to read Central-Directory-File-Header, minimum=22");

		// Create instance
		ZipArchive zip = new ZipArchive();
		strategy.read(zip, data);
		return zip;
	}
}
