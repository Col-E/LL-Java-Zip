package software.coley.lljzip.format.read;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.*;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;
import software.coley.lljzip.util.OffsetComparator;

import javax.annotation.Nonnull;
import java.io.IOException;
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

	/**
	 * New reader with jvm allocator.
	 */
	public JvmZipReader() {
		this(new JvmZipPartAllocator());
	}

	/**
	 * New reader with given allocator.
	 *
	 * @param allocator Allocator to use.
	 */
	public JvmZipReader(@Nonnull ZipPartAllocator allocator) {
		super(allocator);
	}

	@Override
	public void read(@Nonnull ZipArchive zip, @Nonnull ByteData data) throws IOException {
		// Read scanning backwards
		long endOfCentralDirectoryOffset = ByteDataUtil.lastIndexOfQuad(data, data.length() - 4, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		if (endOfCentralDirectoryOffset < 0L)
			throw new IOException("No Central-Directory-File-Header found!");

		// Check for a prior end, indicating a preceding ZIP file.
		long precedingEndOfCentralDirectory = ByteDataUtil.lastIndexOfQuad(data, endOfCentralDirectoryOffset - 1, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);

		// Read end header
		EndOfCentralDirectory end = newEndOfCentralDirectory();
		end.read(data, endOfCentralDirectoryOffset);
		zip.addPart(end);

		// Read central directories (going from the back to the front) up until the preceding ZIP file (if any)
		// but not surpassing the declared cen directory offset in the end of central directory header.
		long len = data.length();
		long centralDirectoryOffset = len - ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER.length;
		long maxRelativeOffset = 0;
		long centralDirectoryOffsetScanEnd = Math.max(Math.max(precedingEndOfCentralDirectory, 0), end.getCentralDirectoryOffset());
		while (centralDirectoryOffset > centralDirectoryOffsetScanEnd) {
			centralDirectoryOffset = ByteDataUtil.lastIndexOfQuad(data, centralDirectoryOffset - 1L, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD);
			if (centralDirectoryOffset >= 0L) {
				CentralDirectoryFileHeader directory = newCentralDirectoryFileHeader();
				directory.read(data, centralDirectoryOffset);
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
		if (priorZipEndWasBogus || precedingEndOfCentralDirectory == -1L) {
			// There was no match for a prior end part. We will seek forwards until finding a *VALID* PK starting header.
			jvmBaseFileOffset = ByteDataUtil.indexOfWord(data, 0, ZipPatterns.PK_WORD);
			while (jvmBaseFileOffset >= 0L) {
				// Check that the PK discovered represents a valid zip part
				try {
					if (data.getInt(jvmBaseFileOffset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
						LocalFileHeader localFileHeader = new LocalFileHeader();
						localFileHeader.read(data, jvmBaseFileOffset);
						if (localFileHeader.getVersionNeededToExtract() > 80) {
							throw new IllegalStateException("Bogus LocalFileHeader");
						}
					} else if (data.getInt(jvmBaseFileOffset) == ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD)
						new CentralDirectoryFileHeader().read(data, jvmBaseFileOffset);
					else
						throw new IllegalStateException("No match for LocalFileHeader/CentralDirectoryFileHeader");
					// Valid, we're good to go
					break;
				} catch (Exception ex) {
					// Invalid, seek forward
					jvmBaseFileOffset = ByteDataUtil.indexOfWord(data, jvmBaseFileOffset + 1L, ZipPatterns.PK_WORD);
				}
			}
		}

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
			if (data.getInt(offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
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
			if (!offsets.contains(offset) && data.getInt(offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				try {
					LocalFileHeader file = newLocalFileHeader();
					if (file instanceof JvmLocalFileHeader) {
						((JvmLocalFileHeader) file).setOffsets(entryOffsets);
					}
					file.read(data, offset);
					directory.link(file);
					file.link(directory);
					file.adoptLinkedCentralDirectoryValues();
					zip.addPart(file);
					postProcessLocalFileHeader(file);
					offsets.add(offset);
				} catch (Exception ex) {
					logger.warn("Failed to read 'local file header' at offset[{}]", offset, ex);
				}
			} else {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
			}
		}

		// Sort based on order
		zip.sortParts(new OffsetComparator());
	}
}
