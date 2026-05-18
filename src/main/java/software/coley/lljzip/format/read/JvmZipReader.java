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
import javax.annotation.Nullable;
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

	private static final long MAX_END_SEARCH = EndOfCentralDirectory.END_HEADER_LENGTH + 0xFFFFL;

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
		// JLI/ZipFile do not trust the last raw END signature they see.
		// Crafted ZIPs can embed fake END/CEN signatures in comments, file data, or trailing junk
		// so we first resolve a structurally valid END and then keep all subsequent parsing bounded to it.
		EndInfo endInfo = findEndOfCentralDirectory(data);
		EndOfCentralDirectory end = endInfo.end();
		long endOfCentralDirectoryOffset = end.offset();
		zip.addPart(end);

		long len = data.byteSize();

		// Parse central-directory entries only inside the END-declared bounds we just validated.
		readCentralDirectories(zip, data, endInfo.centralDirectoryStart(), endOfCentralDirectoryOffset);
		long jvmBaseFileOffset = endInfo.baseOffset();

		// If the END-derived base offset does not appear usable, fall back to the older local-header scan.
		if (!hasAnyLinkedLocalHeader(data, zip, jvmBaseFileOffset) && allowBasicJvmBaseOffsetZeroCheck) {
			Long fallbackBaseOffset = scanForLocalHeaderBaseOffset(data, end);
			if (fallbackBaseOffset != null && hasAnyLinkedLocalHeader(data, zip, fallbackBaseOffset)) {
				jvmBaseFileOffset = fallbackBaseOffset;
			}
		}

		// Read local files
		// - Set to prevent duplicate file header entries for the same offset
		Set<Long> offsets = new HashSet<>();
		TreeSet<Long> entryOffsets = new TreeSet<>();
		long earliestCdfh = Long.MAX_VALUE;
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			// Update earliest central-directory offset to cap the final local entry's data scan.
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

			// Avoid emitting duplicate locals when multiple CEN entries point at the same LOC record.
			boolean isNewOffset = offsets.add(offset);
			if (!isNewOffset) {
				logger.warn("Central-Directory-File-Header's offset[{}] was already visited", offset);
				if (skipRevisitedCenToLocalLinks)
					continue;
			}

			// Leave malformed mappings as CEN-only entries instead of forcing a broken local entry into the model.
			if (offset >= 0 && offset <= len - 4 && MemorySegmentUtil.readQuad(data, offset) != ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				logger.warn("Central-Directory-File-Header's offset[{}] to Local-File-Header does not match the Local-File-Header magic!", offset);
				continue;
			}

			try {
				LocalFileHeader file = newLocalFileHeader();
				if (file instanceof JvmLocalFileHeader jvmFile)
					jvmFile.setOffsets(entryOffsets);
				try {
					// Read the local header when it is in bounds, then let CEN adoption repair JVM-trusted fields.
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

	/**
	 * Scans backwards for a structurally valid {@link EndOfCentralDirectory} entry.
	 * <p>
	 * This exists because hostile or malformed ZIPs frequently contain stray {@link ZipPatterns#END_OF_CENTRAL_DIRECTORY}
	 * byte sequences in comments, file data, or trailing junk. Simply picking the last signature
	 * match is enough to make the parser lock onto a fake END record and derive bogus CEN/LOC
	 * offsets from it.
	 *
	 * @param data
	 * 		ZIP bytes.
	 *
	 * @return Validated END information used to anchor all remaining parsing.
	 *
	 * @throws IOException
	 * 		When no candidate END record can be validated.
	 */
	@Nonnull
	private EndInfo findEndOfCentralDirectory(@Nonnull MemorySegment data) throws IOException {
		long fileLength = data.byteSize();
		long minOffset = Math.max(0L, fileLength - MAX_END_SEARCH);
		long offset = MemorySegmentUtil.lastIndexOfQuad(data, fileLength - 4, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);

		// Walk backward through every candidate in the legal END search window until one validates.
		while (offset >= minOffset && offset >= 0L) {
			EndInfo endInfo = tryReadEndOfCentralDirectory(data, offset);
			if (endInfo != null)
				return endInfo;
			offset = MemorySegmentUtil.lastIndexOfQuad(data, offset - 1L, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		}
		throw new IOException("No valid End-Of-Central-Directory found!");
	}

	/**
	 * Reads and validates an END candidate at the given offset.
	 * <p>
	 * This method exists so that END discovery can reject signature matches that decode into
	 * impossible archive layouts. For a well-formed END record, either its comment length reaches
	 * the physical end of the file, or its computed CEN/LOC positions must point at real ZIP
	 * structures in the same way the JDK's {@code findEND()} logic double-checks suspicious cases.
	 *
	 * @param data
	 * 		ZIP bytes.
	 * @param offset
	 * 		Offset of a candidate END signature.
	 *
	 * @return Validated END information, or {@code null} when the candidate is bogus.
	 */
	@Nullable
	private EndInfo tryReadEndOfCentralDirectory(@Nonnull MemorySegment data, long offset) {
		EndOfCentralDirectory end = newEndOfCentralDirectory();
		try {
			// Decode the END fields first; many bogus matches fail immediately on bounds checks.
			end.read(data, offset);
		} catch (RuntimeException ex) {
			return null;
		}

		long fileLength = data.byteSize();
		long archiveEnd = offset + EndOfCentralDirectory.END_HEADER_LENGTH + end.getZipCommentLength();
		boolean exactEndMatch = archiveEnd == fileLength;

		// Prefer the normal EOF-aligned case, but fall back to structural validation for suspicious matches.
		if (!exactEndMatch && !isValidEndHeader(data, offset, end.getCentralDirectorySize(), end.getCentralDirectoryOffset(), end.getNumEntries()))
			return null;

		// Convert the validated END fields into absolute offsets for bounded CEN/LOC parsing.
		long centralDirectoryStart = offset - end.getCentralDirectorySize();
		long baseOffset = centralDirectoryStart - end.getCentralDirectoryOffset();
		if (centralDirectoryStart < 0L || centralDirectoryStart > offset)
			return null;

		return new EndInfo(end, centralDirectoryStart, baseOffset);
	}

	/**
	 * Performs the structural fallback validation used when an END candidate's declared comment
	 * length does not naturally terminate at the end of the file.
	 * <p>
	 * Specially crafted ZIPs can place fake END signatures inside compressed file contents, inside
	 * comments, or inside trailing junk appended after the real archive. Those fake records can
	 * still decode into seemingly reasonable numeric fields. To avoid being fooled by that, this
	 * check verifies that the END candidate's computed central-directory start lands on a real CEN
	 * header, that the first CEN entry's computed LOC offset lands on a real LOC header, and that
	 * both headers agree on the entry name length. If those structures do not line up, the END
	 * candidate is treated as a decoy.
	 *
	 * @param data
	 * 		ZIP bytes.
	 * @param endPos
	 * 		Offset of the candidate END record.
	 * @param centralDirectorySize
	 * 		CEN byte length declared by the candidate END.
	 * @param centralDirectoryOffset
	 * 		Relative CEN offset declared by the candidate END.
	 * @param entryCount
	 * 		Entry count declared by the candidate END. Included here for future ZIP64-aware validation.
	 *
	 * @return {@code true} when the candidate END describes a coherent archive layout.
	 */
	private boolean isValidEndHeader(@Nonnull MemorySegment data, long endPos, long centralDirectorySize,
	                                 long centralDirectoryOffset, long entryCount) {
		// Reject obviously impossible numeric values before attempting any derived offset math.
		if (centralDirectorySize < 0L || centralDirectoryOffset < 0L || entryCount < 0L)
			return false;

		long centralDirectoryStart = endPos - centralDirectorySize;
		long baseOffset = centralDirectoryStart - centralDirectoryOffset;

		// The declared central-directory block must fit entirely before the END record.
		if (centralDirectoryStart < 0L || centralDirectoryStart > endPos)
			return false;

		// Empty central directories are only valid when they begin exactly where the END record begins.
		if (centralDirectorySize == 0L)
			return centralDirectoryStart == endPos;

		// The computed central-directory start must land on a real CEN header.
		if (centralDirectoryStart > data.byteSize() - CentralDirectoryFileHeader.MIN_FIXED_SIZE)
			return false;
		if (MemorySegmentUtil.readQuad(data, centralDirectoryStart) != ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD)
			return false;

		// The first CEN entry must also map to a real LOC header through the derived base offset.
		long localOffset = baseOffset + MemorySegmentUtil.readMaskedLongQuad(data, centralDirectoryStart, 42);
		if (localOffset < 0L || localOffset > data.byteSize() - LocalFileHeader.MIN_FIXED_SIZE)
			return false;
		if (MemorySegmentUtil.readQuad(data, localOffset) != ZipPatterns.LOCAL_FILE_HEADER_QUAD)
			return false;

		// Finally, verify that the linked CEN and LOC agree on the name length for the same entry.
		return MemorySegmentUtil.readWord(data, centralDirectoryStart, 28) == MemorySegmentUtil.readWord(data, localOffset, 26);
	}

	/**
	 * Reads central directory entries sequentially within the bounds declared by the validated END.
	 * <p>
	 * This exists because scanning the whole tail of the file for {@link ZipPatterns#CENTRAL_DIRECTORY_FILE_HEADER}
	 * signatures is too permissive. A crafted archive can place fake CEN signatures inside comments or payload data
	 * so that an unbounded parser walks past the real CEN and tries to decode arbitrary bytes as more headers.
	 *
	 * @param zip
	 * 		Archive receiving parsed CEN entries.
	 * @param data
	 * 		ZIP bytes.
	 * @param centralDirectoryStart
	 * 		Absolute start of the CEN block, derived from the validated END.
	 * @param endOffset
	 * 		Absolute start of the END record, used as the exclusive upper bound for CEN parsing.
	 *
	 * @throws IOException
	 * 		When any CEN entry falls outside the validated bounds or cannot be decoded.
	 */
	private void readCentralDirectories(@Nonnull ZipArchive zip, @Nonnull MemorySegment data,
	                                    long centralDirectoryStart, long endOffset) throws IOException {
		long offset = centralDirectoryStart;

		// Decode CEN entries sequentially and stop exactly at the validated END boundary.
		while (offset < endOffset) {
			long remaining = endOffset - offset;
			if (remaining < CentralDirectoryFileHeader.MIN_FIXED_SIZE)
				throw new IOException("Invalid central directory: trailing bytes before End-Of-Central-Directory");
			if (MemorySegmentUtil.readQuad(data, offset) != ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD)
				throw new IOException("Invalid central directory header signature at offset[" + offset + "]");

			CentralDirectoryFileHeader directory = newCentralDirectoryFileHeader();
			try {
				directory.read(data, offset);
			} catch (ZipParseException ex) {
				throw new IOException(ex);
			}

			// Each decoded entry must remain fully inside the bounded central-directory region.
			long nextOffset = offset + directory.length();
			if (nextOffset > endOffset)
				throw new IOException("Invalid central directory length at offset[" + offset + "]");

			zip.addPart(directory);
			offset = nextOffset;
		}

		// Any trailing bytes mean we did not consume the validated CEN region cleanly.
		if (offset != endOffset)
			throw new IOException("Invalid central directory bounds");
	}

	/**
	 * Checks whether the current base-offset assumption maps at least one CEN entry onto a real LOC
	 * header.
	 * <p>
	 * This exists as a lightweight sanity check before falling back to the older local-header scan.
	 * In valid prepended-data layouts the END-derived base offset should still resolve real LOC
	 * headers. If it resolves nothing at all, another recovery path may be needed for malformed
	 * legacy samples.
	 *
	 * @param data
	 * 		ZIP bytes.
	 * @param zip
	 * 		Archive containing already parsed CEN entries.
	 * @param baseOffset
	 * 		Candidate absolute base offset for converting CEN-relative LOC offsets into file offsets.
	 *
	 * @return {@code true} when at least one CEN entry maps to a valid LOC header.
	 */
	private boolean hasAnyLinkedLocalHeader(@Nonnull MemorySegment data, @Nonnull ZipArchive zip, long baseOffset) {
		long length = data.byteSize();

		// Probe whether this base offset resolves at least one CEN-relative offset to a real LOC header.
		for (CentralDirectoryFileHeader directory : zip.getCentralDirectories()) {
			long offset = baseOffset + directory.getRelativeOffsetOfLocalHeader();
			if (offset >= 0L && offset <= length - LocalFileHeader.MIN_FIXED_SIZE &&
					MemorySegmentUtil.readQuad(data, offset) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Secondary recovery path that scans for the first plausible ZIP header when the END-derived
	 * base offset does not map any CEN entry onto a readable LOC header.
	 * <p>
	 * This method exists to preserve compatibility with older malformed samples that the project
	 * already accepted. The validated END/CEN path remains authoritative, but some fixtures still
	 * need a best-effort search for the starting ZIP block after prepended junk or inconsistent END
	 * metadata.
	 *
	 * @param data
	 * 		ZIP bytes.
	 * @param end
	 * 		Validated END record.
	 *
	 * @return Replacement base offset, or {@code null} when no plausible header can be found.
	 */
	@Nullable
	private Long scanForLocalHeaderBaseOffset(@Nonnull MemorySegment data, @Nonnull EndOfCentralDirectory end) {
		// Fast-path normal archives that begin with a local header and clearly contain another one later on.
		if (allowBasicJvmBaseOffsetZeroCheck && MemorySegmentUtil.readQuad(data, 0L) == ZipPatterns.LOCAL_FILE_HEADER_QUAD) {
			long nextOffset = MemorySegmentUtil.indexOfQuad(data, 1L, ZipPatterns.LOCAL_FILE_HEADER_QUAD);
			if (nextOffset > LocalFileHeader.MIN_FIXED_SIZE)
				return 0L;
		}

		long baseOffset = (end.offset() - end.getCentralDirectorySize()) - end.getCentralDirectoryOffset();
		long offset = MemorySegmentUtil.indexOfWord(data, baseOffset, ZipPatterns.PK_WORD);

		// Otherwise scan forward from the END-derived guess until the first plausible ZIP structure appears.
		while (offset >= 0L) {
			try {
				int signature = MemorySegmentUtil.readQuad(data, offset);
				if (signature == ZipPatterns.LOCAL_FILE_HEADER_QUAD || signature == ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD)
					return offset;
			} catch (RuntimeException ex) {
				// Continue scanning.
			}
			offset = MemorySegmentUtil.indexOfWord(data, offset + 1L, ZipPatterns.PK_WORD);
		}
		return null;
	}

	private record EndInfo(@Nonnull EndOfCentralDirectory end, long centralDirectoryStart, long baseOffset) {}
}
