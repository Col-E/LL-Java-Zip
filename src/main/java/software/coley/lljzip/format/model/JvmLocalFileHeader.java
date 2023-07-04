package software.coley.lljzip.format.model;

import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;
import software.coley.lljzip.util.lazy.LazyLong;

import javax.annotation.Nonnull;
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
	private long dataOffsetStart;
	private long dataOffsetEnd;
	private boolean foundData;

	/**
	 * @param offsets
	 * 		Set containing all local file header offsets.
	 */
	public void setOffsets(@Nonnull NavigableSet<Long> offsets) {
		this.offsets = offsets;
	}

	@Override
	public void read(@Nonnull ByteData data, long offset) {
		super.read(data, offset);

		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header.
		long dataOffsetStart = offset + MIN_FIXED_SIZE + getFileNameLength() + getExtraFieldLength();
		Long rawDataOffsetEnd = offsets.ceiling(dataOffsetStart);

		// Per section 4.3.9 of the zip file spec:
		//      This descriptor MUST exist if bit 3 of the general purpose bit flag is set.
		//      It is byte aligned and immediately follows the last byte of compressed data.
		//      This descriptor SHOULD be used only when it was not possible to
		//      seek in the output ZIP file (output can be std-out, or non-seekable device)
		//
		// Thus, we subtract the length of the data descriptor section from the data end offset.
		if (rawDataOffsetEnd != null && (getGeneralPurposeBitFlag() & 8) == 8) {
			rawDataOffsetEnd -= 12;
			if (data.getInt(rawDataOffsetEnd - 4) == ZipPatterns.DATA_DESCRIPTOR_QUAD) {
				rawDataOffsetEnd -= 4;
			}
		}

		final Long dataOffsetEnd = rawDataOffsetEnd;
		this.dataOffsetStart = dataOffsetStart;
		this.dataOffsetEnd = dataOffsetEnd == null ? -1 : dataOffsetEnd;
		if (dataOffsetEnd != null) {
			// Valid data range found, map back to (localOffset, range)
			fileData = ByteDataUtil.readLazyLongSlice(data, offset,
					new LazyLong(() -> dataOffsetStart - offset),
					new LazyLong(() -> dataOffsetEnd - dataOffsetStart));
			foundData = true;
		}
	}

	@Override
	public void link(CentralDirectoryFileHeader directoryFileHeader) {
		super.link(directoryFileHeader);

		// JVM trusts central directory file header contents over local
		//  - Using fields as this maintains the lazy model
		compressionMethod = directoryFileHeader.compressionMethod;
		compressedSize = directoryFileHeader.compressedSize;
		uncompressedSize = directoryFileHeader.uncompressedSize;
		fileName = directoryFileHeader.fileName;
		generalPurposeBitFlag = directoryFileHeader.generalPurposeBitFlag;
		crc32 = directoryFileHeader.crc32;
		lastModFileDate = directoryFileHeader.lastModFileDate;
		lastModFileTime = directoryFileHeader.lastModFileTime;

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
			if (fileDataLength + offset < dataOffsetEnd) {
				fileData = ByteDataUtil.readLazyLongSlice(data, offset,
						new LazyLong(() -> dataOffsetStart - offset),
						new LazyLong(() -> fileDataLength));
				data = null;
			}
		}
	}
}
