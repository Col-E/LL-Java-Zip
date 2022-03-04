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

/**
 * The standard read strategy that should work with standard zip archives.
 *
 * @author Matt Coley
 */
public class DefaultZipReaderStrategy implements ZipReaderStrategy {
	private static final Logger logger = LoggerFactory.getLogger(DefaultZipReaderStrategy.class);

	@Override
	public void read(ZipArchive zip, byte[] data) throws IOException {
		int endOfCentralDirectoryOffset = Array.indexOf(data, ZipPatterns.END_OF_CENTRAL_DIRECTORY);
		if (endOfCentralDirectoryOffset < 0)
			throw new IOException("No Central-Directory-File-Header found!");
		// Read end header
		EndOfCentralDirectory end = new EndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.getParts().add(end);
		// Read central directories
		int len = data.length;
		int centralDirectoryOffset = end.getCentralDirectoryOffset();
		while (centralDirectoryOffset < len && Array.startsWith(data, centralDirectoryOffset, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER)) {
			CentralDirectoryFileHeader directory = new CentralDirectoryFileHeader();
			directory.read(data, centralDirectoryOffset);
			centralDirectoryOffset += directory.length();
			zip.getParts().add(directory);
		}
		// Read local files
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			int offset = directory.getRelativeOffsetOfLocalHeader();
			if (Array.startsWith(data, offset, ZipPatterns.LOCAL_FILE_HEADER)) {
				LocalFileHeader file = new LocalFileHeader();
				file.read(data, offset);
				zip.getParts().add(file);
				directory.link(file);
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}
		// Sort based on order
		zip.getParts().sort(new OffsetComparator());
	}
}
