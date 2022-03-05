package software.coley.llzip.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.llzip.ZipArchive;
import software.coley.llzip.ZipPatterns;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.EndOfCentralDirectory;
import software.coley.llzip.part.JvmLocalFileHeader;
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
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmZipReaderStrategy implements ZipReaderStrategy {
	private static final Logger logger = LoggerFactory.getLogger(JvmZipReaderStrategy.class);

	@Override
	public void read(ZipArchive zip, byte[] data) throws IOException {
		// Read scanning backwards
		int endOfCentralDirectoryOffset = Array.lastIndexOf(data, ZipPatterns.END_OF_CENTRAL_DIRECTORY);
		if (endOfCentralDirectoryOffset < 0)
			throw new IOException("No Central-Directory-File-Header found!");
		// Determine base offset for computing file header locations with.
		// - If there is a preceding block of another zip, start with that.
		int jvmBaseOffset;
		int precedingEndOfCentralDirectory = Array.lastIndexOf(data, endOfCentralDirectoryOffset - 1, ZipPatterns.END_OF_CENTRAL_DIRECTORY);
		if (precedingEndOfCentralDirectory == endOfCentralDirectoryOffset) {
			// The prior end part match is target end part, so we can't use it as a base offset.
			jvmBaseOffset = 0;
		} else if (precedingEndOfCentralDirectory == -1) {
			// There was no match for a prior end part. We will seek forwards until finding a *VALID* PK starting header.
			jvmBaseOffset = Array.indexOf(data, ZipPatterns.PK);
			while (jvmBaseOffset >= 0) {
				// Check that the PK discovered represents a valid zip part
				try {
					if (Array.startsWith(data, jvmBaseOffset, ZipPatterns.LOCAL_FILE_HEADER))
						new LocalFileHeader().read(data, jvmBaseOffset);
					else if (Array.startsWith(data, jvmBaseOffset, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER))
						new CentralDirectoryFileHeader().read(data, jvmBaseOffset);
					// Valid, we're good to go
					break;
				} catch (Exception ex) {
					// Invalid, seek forward
					jvmBaseOffset = Array.indexOf(data, jvmBaseOffset+1, ZipPatterns.PK);
				}
			}
		} else {
			// There was a prior end part, so we will seek past it's length and use that as the base offset.
			// 22 is the minimum possible size of an end part. It can be longer with comments applied, but there are almost never comments.
			jvmBaseOffset = precedingEndOfCentralDirectory + 22;
		}
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
			int offset = jvmBaseOffset + directory.getRelativeOffsetOfLocalHeader();
			if (!offsets.contains(offset) && Array.startsWith(data, offset, ZipPatterns.LOCAL_FILE_HEADER)) {
				// JVM local file header needs to be aware of where the NEXT entry is.
				int nextPk = Array.indexOf(data, offset + 1, ZipPatterns.PK);
				if (nextPk == -1) {
					nextPk = data.length;
				}
				try {
					JvmLocalFileHeader file = new JvmLocalFileHeader(nextPk);
					file.read(data, offset);
					zip.getParts().add(file);
					directory.link(file);
					offsets.add(offset);
				} catch (Exception ex) {
					logger.warn("Failed to read 'local file header' at offset[{}]", offset);
				}
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}
		// Sort based on order
		zip.getParts().sort(new OffsetComparator());
	}
}
