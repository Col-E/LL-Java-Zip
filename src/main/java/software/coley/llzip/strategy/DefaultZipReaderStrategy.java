package software.coley.llzip.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.llzip.ZipArchive;
import software.coley.llzip.ZipPatterns;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.EndOfCentralDirectory;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.util.ByteDataUtil;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.OffsetComparator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The standard read strategy that should work with standard zip archives.
 *
 * @author Matt Coley
 */
public class DefaultZipReaderStrategy implements ZipReaderStrategy {
	private static final Logger logger = LoggerFactory.getLogger(DefaultZipReaderStrategy.class);

	@Override
	public void read(ZipArchive zip, ByteData data) throws IOException {
		// Read scanning forwards
		long endOfCentralDirectoryOffset = ByteDataUtil.indexOf(data, ZipPatterns.END_OF_CENTRAL_DIRECTORY);
		if (endOfCentralDirectoryOffset < 0L)
			throw new IOException("No Central-Directory-File-Header found!");
		// Read end header
		EndOfCentralDirectory end = new EndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.getParts().add(end);
		// Read central directories
		long len = data.length();
		long centralDirectoryOffset = end.getCentralDirectoryOffset();
		while (centralDirectoryOffset < len && ByteDataUtil.startsWith(data, centralDirectoryOffset, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER)) {
			CentralDirectoryFileHeader directory = new CentralDirectoryFileHeader();
			directory.read(data, centralDirectoryOffset);
			centralDirectoryOffset += directory.length() & 0xffffffffL; // FIXME: Could still be a lossy cast
			zip.getParts().add(directory);
		}
		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		Set<Integer> offsets = new HashSet<>();
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			int offset = directory.getRelativeOffsetOfLocalHeader();
			if (!offsets.contains(offset) && ByteDataUtil.startsWith(data, offset, ZipPatterns.LOCAL_FILE_HEADER)) {
				LocalFileHeader file = new LocalFileHeader();
				file.read(data, offset);
				zip.getParts().add(file);
				directory.link(file);
				file.link(directory);
				offsets.add(offset);
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}
		// Sort based on order
		zip.getParts().sort(new OffsetComparator());
	}
}
