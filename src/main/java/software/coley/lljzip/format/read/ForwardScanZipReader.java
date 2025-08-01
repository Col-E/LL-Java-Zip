package software.coley.lljzip.format.read;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.model.ZipParseException;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.OffsetComparator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.NavigableSet;
import java.util.TreeSet;

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
	 * @param allocator
	 * 		Allocator to use.
	 */
	public ForwardScanZipReader(@Nonnull ZipPartAllocator allocator) {
		super(allocator);
	}

	@Override
	public void read(@Nonnull ZipArchive zip, @Nonnull MemorySegment data) throws IOException {
		// Read scanning forwards
		long endOfCentralDirectoryOffset = MemorySegmentUtil.indexOfQuad(data, 0, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		if (endOfCentralDirectoryOffset < 0L)
			throw new IOException("No Central-Directory-File-Header found!");

		// Read end header
		EndOfCentralDirectory end = newEndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.addPart(end);

		// Used for relative offsets as a base.
		long zipStart = MemorySegmentUtil.indexOfQuad(data, 0, ZipPatterns.LOCAL_FILE_HEADER_QUAD);

		// Read central directories
		long len = data.byteSize();
		long centralDirectoryOffset = zipStart + end.getCentralDirectoryOffset();
		while (centralDirectoryOffset < len && MemorySegmentUtil.readQuad(data, centralDirectoryOffset) == ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD) {
			CentralDirectoryFileHeader directory = new CentralDirectoryFileHeader();
			try {
				directory.read(data, centralDirectoryOffset);
			} catch (ZipParseException ex) {
				// When the CEN cannot be read, we can't recover
				throw new IOException(ex);
			}
			centralDirectoryOffset += directory.length();
			zip.addPart(directory);
		}

		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		NavigableSet<Long> offsets = new TreeSet<>();
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			long offset = zipStart + directory.getRelativeOffsetOfLocalHeader();
			if (!offsets.contains(offset)
					&& offset < data.byteSize()
					&& MemorySegmentUtil.readQuad(data, offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				LocalFileHeader file = newLocalFileHeader();
				directory.link(file);
				file.link(directory);
				try {
					file.read(data, offset);
				} catch (ZipParseException ex) {
					// Unlike the other readers, we aren't going to fall back to using CEN data, so we won't recover from this.
					throw new IOException(ex);
				}
				zip.addPart(file);
				postProcessLocalFileHeader(file);
				offsets.add(offset);
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}

		// Record any data appearing at the front of the file not associated with the ZIP file contents.
		if (!offsets.isEmpty()) {
			long firstOffset = offsets.first();
			zip.setPrefixData(data.asSlice(0, firstOffset));
		}

		// Sort based on order
		zip.sortParts(new OffsetComparator());
	}
}
