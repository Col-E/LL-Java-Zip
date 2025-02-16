package software.coley.lljzip.format.read;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.JvmLocalFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.model.ZipParseException;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.OffsetComparator;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * The JVM has some edge cases in how it parses zip/jar files.
 * It allows for some tricks that most tools do not support/expect.
 * <p>
 * The primary difference from {@link ForwardScanZipReader} is that the {@link EndOfCentralDirectory}
 * is scanned from the back, rather than from the front.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmZipReader extends AbstractZipReader {
	private static final Logger logger = LoggerFactory.getLogger(JvmZipReader.class);
	private final boolean skipRevisitedCenToLocalLinks;
	private final boolean allowBasicJvmBaseOffsetZeroCheck;

	/**
	 * New reader with jvm allocator.
	 */
	public JvmZipReader() {
		this(new JvmZipPartAllocator(), true, true);
	}

	/**
	 * New reader with jvm allocator.
	 *
	 * @param skipRevisitedCenToLocalLinks
	 * 		Flag to skip creating duplicate {@link LocalFileHeader} entries if multiple
	 *        {@link CentralDirectoryFileHeader} point to the same location.
	 * @param allowBasicJvmBaseOffsetZeroCheck
	 * 		Flag to check for {@code jvmBaseFileOffset == 0} before using the logic adapted from {@code ZipFile.Source#findEND()}.
	 */
	public JvmZipReader(boolean skipRevisitedCenToLocalLinks, boolean allowBasicJvmBaseOffsetZeroCheck) {
		this(new JvmZipPartAllocator(), skipRevisitedCenToLocalLinks, allowBasicJvmBaseOffsetZeroCheck);
	}

	/**
	 * New reader with given allocator.
	 *
	 * @param allocator
	 * 		Allocator to use.
	 * @param skipRevisitedCenToLocalLinks
	 * 		Flag to skip creating duplicate {@link LocalFileHeader} entries if multiple
	 *        {@link CentralDirectoryFileHeader} point to the same location.
	 * @param allowBasicJvmBaseOffsetZeroCheck
	 * 		Flag to check for {@code jvmBaseFileOffset == 0} before using the logic adapted from {@code ZipFile.Source#findEND()}.
	 */
	public JvmZipReader(@Nonnull ZipPartAllocator allocator, boolean skipRevisitedCenToLocalLinks, boolean allowBasicJvmBaseOffsetZeroCheck) {
		super(allocator);
		this.skipRevisitedCenToLocalLinks = skipRevisitedCenToLocalLinks;
		this.allowBasicJvmBaseOffsetZeroCheck = allowBasicJvmBaseOffsetZeroCheck;
	}

	@Override
	public void read(@Nonnull ZipArchive zip, @Nonnull MemorySegment data) throws IOException {
		// Read scanning backwards
		long endOfCentralDirectoryOffset = MemorySegmentUtil.lastIndexOfQuad(data, data.byteSize() - 4, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		if (endOfCentralDirectoryOffset < 0L)
			throw new IOException("No Central-Directory-File-Header found!");

		// Check for a prior end, indicating a preceding ZIP file.
		long precedingEndOfCentralDirectory = MemorySegmentUtil.lastIndexOfQuad(data, endOfCentralDirectoryOffset - 1, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);

		// Read end header
		EndOfCentralDirectory end = newEndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.addPart(end);

		// Read central directories (going from the back to the front) up until the preceding ZIP file (if any)
		// but not surpassing the declared cen directory offset in the end of central directory header.
		long len = data.byteSize();
		long centralDirectoryOffset = len - ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER.length;
		long maxRelativeOffset = 0;
		long centralDirectoryOffsetScanEnd = Math.max(Math.max(precedingEndOfCentralDirectory, 0), end.getCentralDirectoryOffset());
		while (centralDirectoryOffset > centralDirectoryOffsetScanEnd) {
			centralDirectoryOffset = MemorySegmentUtil.lastIndexOfQuad(data, centralDirectoryOffset - 1L, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD);
			if (centralDirectoryOffset >= 0L) {
				CentralDirectoryFileHeader directory = newCentralDirectoryFileHeader();
				try {
					directory.read(data, centralDirectoryOffset);
				} catch (ZipParseException ex) {
					// We cannot recover from the CEN reading encountering failures.
					throw new IOException(ex);
				}
				zip.addPart(directory);
				if (directory.getRelativeOffsetOfLocalHeader() > maxRelativeOffset)
					maxRelativeOffset = directory.getRelativeOffsetOfLocalHeader();
			}
		}

		// Determine base offset for computing file header locations with.
		long jvmBaseFileOffset = 0;
		boolean priorZipEndWasBogus = false;

		// If there is a preceding block of another zip, start with that.
		if (precedingEndOfCentralDirectory != -1) {
			// There was a prior end part, so we will seek past it's length and use that as the base offset.
			try {
				// Make sure it isn't bogus before we use it as a reference point
				EndOfCentralDirectory tempEnd = new EndOfCentralDirectory();
				tempEnd.read(data, precedingEndOfCentralDirectory);

				// If we use this as a point of reference there must be enough data remaining
				// to read the largest offset specified by our central directories.
				long hypotheticalJvmBaseOffset = precedingEndOfCentralDirectory + tempEnd.length();
				if (len <= hypotheticalJvmBaseOffset + maxRelativeOffset)
					throw new IllegalStateException();

				// TODO: Double check 'precedingEndOfCentralDirectory' points to a EndOfCentralDirectory that isn't bogus
				//  like some shit defined as a fake comment in another ZipPart.
				//   - Needs to be done in such a way where we do not get tricked by the '-trick.jar' samples
				jvmBaseFileOffset = precedingEndOfCentralDirectory + tempEnd.length();
			} catch (Exception ex) {
				// It's bogus and the sig-match was a coincidence.
				priorZipEndWasBogus = true;
			}
		}

		// Search for the first valid PK header if there was either no prior ZIP file
		// or if the prior ZIP detection was bogus.
		scan:
		{
			if (priorZipEndWasBogus || precedingEndOfCentralDirectory == -1L) {
				// If the start of the file is valid, we don't have to actually do much more anti-bogus work.
				// This whole 'scan' block is incredibly yucky but seems to work for all of our edge case inputs.
				if (allowBasicJvmBaseOffsetZeroCheck && MemorySegmentUtil.readQuad(data, 0) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
					if (MemorySegmentUtil.indexOfQuad(data, 1, ZipPatterns.LOCAL_FILE_HEADER_QUAD) > LocalFileHeader.MIN_FIXED_SIZE) {
						break scan;
					}
				}

				// There was no match for a prior end part. We will seek forwards until finding a *VALID* PK starting header.
				//  - Java's zip parser does not always start from zero. It uses the computation:
				//      locpos = (end.endpos - end.cenlen) - end.cenoff;
				//  - This computation is taken from: ZipFile.Source#initCEN
				long endPos = end.offset();
				long cenLen = end.getCentralDirectorySize();
				long cenOff = end.getCentralDirectoryOffset();
				jvmBaseFileOffset = (endPos - cenLen) - cenOff;

				// Now that we have the start offset, scan forward. We can match the current value as well.
				jvmBaseFileOffset = MemorySegmentUtil.indexOfWord(data, jvmBaseFileOffset, ZipPatterns.PK_WORD);
				while (jvmBaseFileOffset >= 0L) {
					// Check that the PK discovered represents a valid zip part
					try {
						if (MemorySegmentUtil.readQuad(data, jvmBaseFileOffset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD)
							break;
						else if (MemorySegmentUtil.readQuad(data, jvmBaseFileOffset) == ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD)
							break;

						// Not an expected value...
						throw new IllegalStateException("No match for LocalFileHeader/CentralDirectoryFileHeader");
					} catch (Exception ex) {
						// Invalid, seek forward
						jvmBaseFileOffset = MemorySegmentUtil.indexOfWord(data, jvmBaseFileOffset + 1L, ZipPatterns.PK_WORD);
					}
				}
			}
		}

		// Normalize for when the 'indexOfWord' checks above yield no results.
		jvmBaseFileOffset = Math.max(0, jvmBaseFileOffset);

		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		Set<Long> offsets = new HashSet<>();
		TreeSet<Long> entryOffsets = new TreeSet<>();
		long earliestCdfh = Long.MAX_VALUE;
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			// Update earliest central offset
			if (directory.offset() < earliestCdfh)
				earliestCdfh = directory.offset();

			// Add associated local file header offset
			long offset = jvmBaseFileOffset + directory.getRelativeOffsetOfLocalHeader();
			if (offset >= 0 && offset < len - 4 && MemorySegmentUtil.readQuad(data, offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				entryOffsets.add(offset);
			}
		}

		// Add the earliest central directory offset, which serves as the upper bound to search against for the
		// last local file header entry's file data contents.
		entryOffsets.add(earliestCdfh);

		// Add the end of central directory
		entryOffsets.add(endOfCentralDirectoryOffset);

		// Create the local file entries
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			long relative = directory.getRelativeOffsetOfLocalHeader();
			long offset = jvmBaseFileOffset + relative;
			boolean isNewOffset = offsets.add(offset);
			if (!isNewOffset) {
				logger.warn("Central-Directory-File-Header's offset[{}] was already visited", offset);
				if (skipRevisitedCenToLocalLinks)
					continue;
			}

			if (offset >= 0 && offset <= len - 4 && MemorySegmentUtil.readQuad(data, offset) != ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
				continue;
			}

			try {
				LocalFileHeader file = newLocalFileHeader();
				if (file instanceof JvmLocalFileHeader jvmFile)
					jvmFile.setOffsets(entryOffsets);
				try {
					if (offset <= len - LocalFileHeader.MIN_FIXED_SIZE)
						file.read(data, offset);
				} catch (IndexOutOfBoundsException t) {
					// Its intended that if this fails the adopting of CEN values below will work instead.
				}
				directory.link(file);
				file.link(directory);
				file.adoptLinkedCentralDirectoryValues();
				zip.addPart(file);
				postProcessLocalFileHeader(file);
			} catch (Exception ex) {
				logger.warn("Failed to read 'local file header' at offset[{}]", offset, ex);
			}
		}

		// Record any data appearing at the front of the file not associated with the ZIP file contents.
		if (!entryOffsets.isEmpty()) {
			long firstOffset = entryOffsets.first();
			if (firstOffset > 0)
				zip.setPrefixData(data.asSlice(0, firstOffset));
		}

		// Sort based on order
		zip.sortParts(new OffsetComparator());
	}
}
