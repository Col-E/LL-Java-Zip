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

		// While we do scan to the next header, we need to consider the optional data-descriptor component.
		// If it is present, the end range of the data should be cut by 16 bytes (the size of the data-descriptor)
		//
		// Per section 4.3.9 of the zip file spec:
		//      This descriptor MUST exist if bit 3 of the general purpose bit flag is set.
		//      It is byte aligned and immediately follows the last byte of compressed data.
		//      This descriptor SHOULD be used only when it was not possible to
		//      seek in the output ZIP file (output can be std-out, or non-seekable device)
		//
		// Thus, we subtract the length of the data descriptor section from the data end offset.
		if (absoluteDataOffsetEnd != null && (getGeneralPurposeBitFlag() & 0b1000) == 0b1000) {
			// Format:
			//  data-descriptor-header  4 bytes 08 07 4b 50
			//  crc-32                  4 bytes
			//  compressed size         4 bytes
			//  uncompressed size       4 bytes
			//
			// The JVM technically allows the header to be excluded, so we split the offset fixing
			// into two parts.
			//
			// In some WEIRD cases the bit flag can be set, but the data-descriptor will be missing.
			// When this occurs we can validate the range is currently correct by checking if the data end offset
			// is the beginning of another file header. If we find the file header, the bit flag is a lie,
			// and we do not need to manipulate our data end offset.
			if ((MemorySegmentUtil.readWord(data, absoluteDataOffsetEnd) & ZipPatterns.PK_WORD) != ZipPatterns.PK_WORD) {
				absoluteDataOffsetEnd -= 12;
				if (MemorySegmentUtil.readQuad(data, absoluteDataOffsetEnd) == ZipPatterns.DATA_DESCRIPTOR_QUAD) {
					absoluteDataOffsetEnd -= 4;
				}
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
		fileNameLength = linkedDirectoryFileHeader.fileNameLength;
		fileName = linkedDirectoryFileHeader.fileName;

		// The sizes are not used by the JVM parser.
		// It just says 'go until the next header'.
		//
		//   compressedSize = directoryFileHeader.compressedSize;
		//   uncompressedSize = directoryFileHeader.uncompressedSize;
		//
		// That being said, we want a fallback if no data is found.
		// This may occur if something with offset detection fails.
		//
		// Update file data with new compressed/uncompressed size if it was not able to be found previously
		// with only the local data available.
		if (!foundData) {
			// Extract updated length from CEN values
			long fileDataLength;
			if (getCompressionMethod() == ZipCompressions.STORED) {
				fileDataLength = getUncompressedSize() & 0xFFFFFFFFL;
			} else {
				fileDataLength = getCompressedSize() & 0xFFFFFFFFL;
			}

			// Only allow lengths that are within a sensible range.
			// Data should not be overflowing into adjacent header entries.
			// - If it is, the data here is likely intentionally tampered with to screw with parsers
			if (fileDataLength < relativeDataOffsetEnd) {
				fileData = MemorySegmentData.of(MemorySegmentUtil.readLongSlice(data, offset, relativeDataOffsetStart - offset, fileDataLength));
			}
		}
	}
}
