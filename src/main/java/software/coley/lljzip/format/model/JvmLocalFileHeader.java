package software.coley.lljzip.format.model;

import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.data.MemorySegmentData;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;
import java.util.NavigableSet;

/**
 * An extension of {@link LocalFileHeader} with adjustments to the file-data parse logic to support
 * how the Hotspot JVM handles parsing jar files.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmLocalFileHeader extends LocalFileHeader {
	private NavigableSet<Long> offsets;
	private long relativeDataOffsetStart;
	private long relativeDataOffsetEnd;
	private boolean foundData;

	/**
	 * @param offsets
	 * 		Set containing all local file header offsets.
	 */
	public void setOffsets(@Nonnull NavigableSet<Long> offsets) {
		this.offsets = offsets;
	}

	@Nonnull
	@Override
	protected MemorySegmentData readFileData(@Nonnull MemorySegment data, long headerOffset) {
		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header.
		long relativeDataOffsetStart = MIN_FIXED_SIZE + getFileNameLength() + getExtraFieldLength();
		long relativeDataOffsetEnd;
		Long absoluteDataOffsetEnd = offsets.ceiling(offset + relativeDataOffsetStart);

		// When the local header exposes a trustworthy size (including ZIP64 extra values), use it
		// directly and leave any trailing data descriptor outside the file-data slice.
		//
 		// For descriptor info see APPNOTE 4.3.9
		if (absoluteDataOffsetEnd != null && (getGeneralPurposeBitFlag() & 0b1000) == 0b1000) {
			long explicitFileDataLength = getExplicitFileDataLength();
			if (explicitFileDataLength >= 0L) {
				long candidateDataEnd = offset + relativeDataOffsetStart + explicitFileDataLength;
				if (candidateDataEnd <= absoluteDataOffsetEnd)
					absoluteDataOffsetEnd = candidateDataEnd;
			}
		}
		relativeDataOffsetEnd = absoluteDataOffsetEnd == null ? relativeDataOffsetStart : absoluteDataOffsetEnd - offset;

		// Update the file data ranges
		this.relativeDataOffsetStart = relativeDataOffsetStart;
		this.relativeDataOffsetEnd = relativeDataOffsetEnd;
		long fileDataLength = relativeDataOffsetEnd - relativeDataOffsetStart;

		fileData = MemorySegmentData.of(MemorySegmentUtil.readLongSlice(data, offset, relativeDataOffsetStart, fileDataLength));

		// Update sizes where possible
		if (getCompressionMethod() == ZipCompressions.STORED) {
			setUncompressedSize(fileDataLength);
			setCompressedSize(fileDataLength);
		} else {
			setCompressedSize(fileDataLength);
		}

		// If we have a size, we can assume we found some data.
		// Whether its valid, who really knows?
		foundData = fileDataLength != 0;
		return fileData;
	}

	private long getExplicitFileDataLength() {
		long length = getCompressionMethod() == ZipCompressions.STORED ? getUncompressedSize() : getCompressedSize();
		return length > 0L ? length : -1L;
	}

	@Override
	public void adoptLinkedCentralDirectoryValues() {
		CentralDirectoryFileHeader directoryFileHeader = linkedDirectoryFileHeader;
		if (directoryFileHeader == null)
			return;

		// JVM trusts central directory file header contents over local
		versionNeededToExtract = linkedDirectoryFileHeader.versionNeededToExtract;
		generalPurposeBitFlag = linkedDirectoryFileHeader.generalPurposeBitFlag;
		compressionMethod = linkedDirectoryFileHeader.compressionMethod;
		lastModFileTime = linkedDirectoryFileHeader.lastModFileTime;
		lastModFileDate = linkedDirectoryFileHeader.lastModFileDate;
		crc32 = linkedDirectoryFileHeader.crc32;
		compressedSize = linkedDirectoryFileHeader.compressedSize;
		uncompressedSize = linkedDirectoryFileHeader.uncompressedSize;
		fileNameLength = linkedDirectoryFileHeader.fileNameLength;
		fileName = linkedDirectoryFileHeader.fileName;

		// The sizes are not used by the JVM parser.
		// It just says 'go until the next header'.
		//
		//   compressedSize = directoryFileHeader.compressedSize;
		//   uncompressedSize = directoryFileHeader.uncompressedSize;
		//
		// Update file data with authoritative CEN sizes when the current range is missing or includes a descriptor.
		long fileDataLength;
		if (getCompressionMethod() == ZipCompressions.STORED) {
			fileDataLength = getUncompressedSize();
		} else {
			fileDataLength = getCompressedSize();
		}

		// Get the absolute data offsets and bounds for the file data.
		// Ensure the data range is within the file.
		long absoluteDataOffsetStart = offset + relativeDataOffsetStart;
		long availableDataEnd = data.byteSize();
		if (relativeDataOffsetEnd > relativeDataOffsetStart)
			availableDataEnd = Math.min(availableDataEnd, offset + relativeDataOffsetEnd);
		long absoluteDataOffsetEnd = absoluteDataOffsetStart + fileDataLength;

		// If we see any of the following conditions:
		//  - We didn't find any data
		//  - The general purpose bit flag indicates a data descriptor is present
		//  - The file data length doesn't match the expected length
		// Then we should realign the file data range to match the authoritative CEN sizes,
		// as what we have is invalid.
		boolean shouldRealignDataRange = !foundData ||
				(getGeneralPurposeBitFlag() & 0b1000) == 0b1000 ||
				fileData.length() != fileDataLength;
		if (shouldRealignDataRange
				&& fileDataLength >= 0L
				&& absoluteDataOffsetStart >= 0L
				&& absoluteDataOffsetEnd <= availableDataEnd) {
			fileData = MemorySegmentData.of(data, absoluteDataOffsetStart, fileDataLength);
			relativeDataOffsetEnd = relativeDataOffsetStart + fileDataLength;
			foundData = fileDataLength != 0L;
		}
	}
}
