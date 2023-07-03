package software.coley.lljzip.format.read;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;
import software.coley.lljzip.util.OffsetComparator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The standard read strategy that should work with standard zip archives.
 *
 * @author Matt Coley
 */
public class ForwardScanZipReader extends AbstractZipReader {
	private static final Logger logger = LoggerFactory.getLogger(ForwardScanZipReader.class);

	/**
	 * New reader with simple allocator.
	 */
	public ForwardScanZipReader() {
		this(new SimpleZipPartAllocator());
	}

	/**
	 * New reader with given allocator.
	 *
	 * @param allocator Allocator to use.
	 */
	public ForwardScanZipReader(@Nonnull ZipPartAllocator allocator) {
		super(allocator);
	}

	@Override
	public void read(@Nonnull ZipArchive zip, @Nonnull ByteData data) throws IOException {
		// Read scanning forwards
		long endOfCentralDirectoryOffset = ByteDataUtil.indexOfQuad(data, 0, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		if (endOfCentralDirectoryOffset < 0L)
			throw new IOException("No Central-Directory-File-Header found!");

		// Read end header
		EndOfCentralDirectory end = newEndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.addPart(end);

		// Used for relative offsets as a base.
		long zipStart = ByteDataUtil.indexOfQuad(data, 0, ZipPatterns.LOCAL_FILE_HEADER_QUAD);

		// Read central directories
		long len = data.length();
		long centralDirectoryOffset = zipStart + end.getCentralDirectoryOffset();
		while (centralDirectoryOffset < len && data.getInt(centralDirectoryOffset) == ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD) {
			CentralDirectoryFileHeader directory = new CentralDirectoryFileHeader();
			directory.read(data, centralDirectoryOffset);
			centralDirectoryOffset += directory.length();
			zip.addPart(directory);
		}

		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		Set<Long> offsets = new HashSet<>();
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			long offset = zipStart + directory.getRelativeOffsetOfLocalHeader();
			if (!offsets.contains(offset) && data.getInt(offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				LocalFileHeader file = newLocalFileHeader();
				file.read(data, offset);
				zip.addPart(file);
				directory.link(file);
				file.link(directory);
				postProcessLocalFileHeader(file);
				offsets.add(offset);
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}

		// Sort based on order
		zip.sortParts(new OffsetComparator());
	}
}
