package software.coley.lljzip.format.read;

import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.util.MemorySegmentUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.foreign.MemorySegment;

/**
 * Shared ZIP64 parsing utilities used by the concrete ZIP readers.
 *
 * @author Matt Coley
 */
public final class Zip64Support {
	private static final long ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_LENGTH = 20L;
	private static final long ZIP64_END_OF_CENTRAL_DIRECTORY_MIN_LENGTH = 56L;

	private Zip64Support() {}

	/**
	 * Resolves the real central-directory bounds associated with a previously decoded legacy END record.
	 *
	 * @param data
	 * 		ZIP bytes.
	 * @param end
	 * 		Previously decoded legacy END record.
	 *
	 * @return Absolute central-directory bounds and base-offset information
	 * derived from either the classic END or the ZIP64 END record.
	 *
	 * @throws IOException
	 * 		When a ZIP64 trailer encodes an unsupported split archive
	 * 		or otherwise exposes values the current model cannot represent.
	 */
	@Nonnull
	public static ResolvedEnd resolveEndOfCentralDirectory(@Nonnull MemorySegment data,
	                                                       @Nonnull EndOfCentralDirectory end) throws IOException {
		long endOffset = end.offset();
		Zip64EndInfo zip64Info = tryReadZip64EndInfo(data, endOffset);
		if (zip64Info != null) {
			if (zip64Info.entriesOnThisDisk() > Integer.MAX_VALUE || zip64Info.entryCount() > Integer.MAX_VALUE)
				throw new IOException("Unsupported ZIP64 entry count range");

			// Normalize the legacy END object so downstream code can keep reading from the same model.
			end.setDiskNumber((int) zip64Info.diskNumber());
			end.setCentralDirectoryStartDisk((int) zip64Info.centralDirectoryStartDisk());
			end.setCentralDirectoryStartOffset((int) zip64Info.entriesOnThisDisk());
			end.setNumEntries((int) zip64Info.entryCount());
			end.setCentralDirectorySize(zip64Info.centralDirectorySize());
			end.setCentralDirectoryOffset(zip64Info.centralDirectoryOffset());

			// In ZIP64 layouts the central directory ends where the ZIP64 END record begins, not where
			// the legacy END begins.
			long centralDirectoryStart = zip64Info.recordOffset() - zip64Info.centralDirectorySize();
			long baseOffset = centralDirectoryStart - zip64Info.centralDirectoryOffset();
			return new ResolvedEnd(centralDirectoryStart, baseOffset, zip64Info.recordOffset(), zip64Info);
		}

		long centralDirectoryStart = endOffset - end.getCentralDirectorySize();
		long baseOffset = centralDirectoryStart - end.getCentralDirectoryOffset();
		return new ResolvedEnd(centralDirectoryStart, baseOffset, endOffset, null);
	}

	/**
	 * Reads the ZIP64 locator and ZIP64 END record that may precede a legacy END record.
	 *
	 * @param data
	 * 		ZIP bytes.
	 * @param endOffset
	 * 		Offset of the legacy END record.
	 *
	 * @return ZIP64 trailer information, or {@code null} when the locator/record pair is absent or invalid.
	 */
	@Nullable
	static Zip64EndInfo tryReadZip64EndInfo(@Nonnull MemorySegment data, long endOffset) {
		// Must have valid locator immediately before the END record.
		long locatorOffset = endOffset - ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_LENGTH;
		if (locatorOffset < 0L || locatorOffset > data.byteSize() - ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_LENGTH)
			return null;
		if (MemorySegmentUtil.readQuad(data, locatorOffset) != ZipPatterns.ZIP64_END_OF_CENTRAL_DIRECTORY_LOCATOR_QUAD)
			return null;

		// Must have a valid ZIP64 END record at the offset specified by the locator,
		// and that record must fit between the locator and the legacy END.
		long locatorStartDisk = MemorySegmentUtil.readMaskedLongQuad(data, locatorOffset, 4);
		long zip64EndOffset = MemorySegmentUtil.readLong(data, locatorOffset, 8);
		long totalDisks = MemorySegmentUtil.readMaskedLongQuad(data, locatorOffset, 16);
		if (zip64EndOffset < 0L || zip64EndOffset > locatorOffset - ZIP64_END_OF_CENTRAL_DIRECTORY_MIN_LENGTH)
			return null;
		if (MemorySegmentUtil.readQuad(data, zip64EndOffset) != ZipPatterns.ZIP64_END_OF_CENTRAL_DIRECTORY_QUAD)
			return null;

		// The record stores its payload size excluding the 12-byte signature + size prefix.
		long zip64RecordSize = MemorySegmentUtil.readLong(data, zip64EndOffset, 4);
		long zip64RecordLength = 12L + zip64RecordSize;
		if (zip64RecordSize < 44L || zip64EndOffset + zip64RecordLength > locatorOffset)
			return null;

		long diskNumber = MemorySegmentUtil.readMaskedLongQuad(data, zip64EndOffset, 16);
		long centralDirectoryStartDisk = MemorySegmentUtil.readMaskedLongQuad(data, zip64EndOffset, 20);
		long entriesOnThisDisk = MemorySegmentUtil.readLong(data, zip64EndOffset, 24);
		long entryCount = MemorySegmentUtil.readLong(data, zip64EndOffset, 32);
		long centralDirectorySize = MemorySegmentUtil.readLong(data, zip64EndOffset, 40);
		long centralDirectoryOffset = MemorySegmentUtil.readLong(data, zip64EndOffset, 48);
		if (entriesOnThisDisk < 0L || entryCount < 0L || centralDirectorySize < 0L || centralDirectoryOffset < 0L)
			return null;

		return new Zip64EndInfo(zip64EndOffset, locatorStartDisk, totalDisks, diskNumber,
				centralDirectoryStartDisk, entriesOnThisDisk, entryCount, centralDirectorySize, centralDirectoryOffset);
	}

	/**
	 * Internal carrier for the resolved absolute CEN bounds derived from a legacy END record
	 * and its optional ZIP64 trailer.
	 *
	 * @param centralDirectoryStart
	 * 		Absolute start offset of the central directory.
	 * @param baseOffset
	 * 		Base offset used to translate central-directory-relative local offsets into file offsets.
	 * @param centralDirectoryEnd
	 * 		Exclusive upper bound of the central directory.
	 * @param zip64Info
	 * 		Backing ZIP64 trailer details when present, otherwise {@code null}.
	 */
	public record ResolvedEnd(long centralDirectoryStart, long baseOffset, long centralDirectoryEnd,
	                          @Nullable Zip64EndInfo zip64Info) {}

	/**
	 * Internal carrier for the subset of ZIP64 END fields used by the readers.
	 *
	 * @param recordOffset
	 * 		Absolute offset of the ZIP64 END record.
	 * @param locatorStartDisk
	 * 		Disk number encoded by the ZIP64 locator.
	 * @param totalDisks
	 * 		Total number of disks encoded by the ZIP64 locator.
	 * @param diskNumber
	 * 		Disk number containing the ZIP64 END record.
	 * @param centralDirectoryStartDisk
	 * 		Disk number where the central directory starts.
	 * @param entriesOnThisDisk
	 * 		Number of entries reported on the current disk.
	 * @param entryCount
	 * 		Total number of entries in the central directory.
	 * @param centralDirectorySize
	 * 		Central-directory size in bytes.
	 * @param centralDirectoryOffset
	 * 		Central-directory offset relative to the archive base.
	 */
	public record Zip64EndInfo(long recordOffset, long locatorStartDisk, long totalDisks,
	                           long diskNumber, long centralDirectoryStartDisk,
	                           long entriesOnThisDisk, long entryCount,
	                           long centralDirectorySize, long centralDirectoryOffset) {}
}
