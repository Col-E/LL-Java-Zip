package software.coley.llzip.format.read;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.llzip.format.ZipPatterns;
import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;
import software.coley.llzip.util.OffsetComparator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The standard read strategy that should work with standard zip archives.
 *
 * @author Matt Coley
 */
public class ForwardScanZipReaderStrategy implements ZipReaderStrategy {
	private static final Logger logger = LoggerFactory.getLogger(ForwardScanZipReaderStrategy.class);

	@Override
	public void read(ZipArchive zip, ByteData data) throws IOException {
		// Read scanning forwards
		long endOfCentralDirectoryOffset = ByteDataUtil.indexOfQuad(data, 0, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		if (endOfCentralDirectoryOffset < 0L)
			throw new IOException("No Central-Directory-File-Header found!");

		// Read end header
		EndOfCentralDirectory end = new EndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.getParts().add(end);

		// Used for relative offsets as a base.
		long zipStart = ByteDataUtil.indexOfQuad(data, 0, ZipPatterns.LOCAL_FILE_HEADER_QUAD);

		// Read central directories
		long len = data.length();
		long centralDirectoryOffset = zipStart + end.getCentralDirectoryOffset();
		while (centralDirectoryOffset < len && data.getInt(centralDirectoryOffset) == ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD) {
			CentralDirectoryFileHeader directory = new CentralDirectoryFileHeader();
			directory.read(data, centralDirectoryOffset);
			centralDirectoryOffset += directory.length();
			zip.getParts().add(directory);
		}

		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		Set<Long> offsets = new HashSet<>();
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			long offset = zipStart + directory.getRelativeOffsetOfLocalHeader();
			if (!offsets.contains(offset) && data.getInt(offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				LocalFileHeader file = new LocalFileHeader();
				file.read(data, offset);
				zip.getParts().add(file);
				directory.link(file);
				file.link(directory);
				postProcessLocalFileHeader(file);
				file.freeze();
				offsets.add(offset);
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}

		// Sort based on order
		zip.getParts().sort(new OffsetComparator());
	}
}
