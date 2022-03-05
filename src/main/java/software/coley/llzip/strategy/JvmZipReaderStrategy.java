package software.coley.llzip.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.llzip.ZipArchive;
import software.coley.llzip.ZipPatterns;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.EndOfCentralDirectory;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.util.Array;
import software.coley.llzip.util.OffsetComparator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The JVM has some edge cases in how it parses zip/jar files.
 * It allows for some tricks that most tools do not support/expect.
 * <br>
 * The primary difference from {@link DefaultZipReaderStrategy} is that the {@link EndOfCentralDirectory}
 * is scanned from the back, rather than from the front.
 *
 * @author Matt Coley
 */
public class JvmZipReaderStrategy implements ZipReaderStrategy {
	private static final Logger logger = LoggerFactory.getLogger(JvmZipReaderStrategy.class);

	@Override
	public void read(ZipArchive zip, byte[] data) throws IOException {
		// TODO: Track leading garbage offset

		// Read scanning backwards
		int endOfCentralDirectoryOffset = Array.lastIndexOf(data, ZipPatterns.END_OF_CENTRAL_DIRECTORY);
		if (endOfCentralDirectoryOffset < 0)
			throw new IOException("No Central-Directory-File-Header found!");
		// Read end header
		EndOfCentralDirectory end = new EndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.getParts().add(end);
		// Read central directories
		int len = data.length;
		int centralDirectoryOffset = len - ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER.length;
		while (centralDirectoryOffset > 0) {
			centralDirectoryOffset = Array.lastIndexOf(data, centralDirectoryOffset - 1, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER);
			if (centralDirectoryOffset >= 0) {
				CentralDirectoryFileHeader directory = new CentralDirectoryFileHeader();
				directory.read(data, centralDirectoryOffset);
				zip.getParts().add(directory);
			}
		}
		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		Set<Integer> offsets = new HashSet<>();
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			int offset = directory.getRelativeOffsetOfLocalHeader();
			if (!offsets.contains(offset) && Array.startsWith(data, offset, ZipPatterns.LOCAL_FILE_HEADER)) {
				LocalFileHeader file = new LocalFileHeader();
				file.read(data, offset);
				zip.getParts().add(file);
				directory.link(file);
				offsets.add(offset);
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}
		// Sort based on order
		zip.getParts().sort(new OffsetComparator());
	}
}
