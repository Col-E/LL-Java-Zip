package software.coley.llzip.part;

import software.coley.llzip.ZipCompressions;
import software.coley.llzip.util.ByteData;

import java.util.NavigableSet;


/**
 * An extension of {@link LocalFileHeader} with adjustments to the file-data parse logic to support
 * how the Hotspot JVM handles parsing jar files.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmLocalFileHeader extends LocalFileHeader {
	private final NavigableSet<Long> offsets;
	private long offset;
	private boolean foundPk;
	private ByteData data;

	/**
	 * @param offsets
	 * 		Set containing all local file header offsets.
	 */
	public JvmLocalFileHeader(NavigableSet<Long> offsets) {
		this.offsets = offsets;
	}

	@Override
	public void read(ByteData data, long offset) {
		super.read(data, offset);
		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header.
		offset += MIN_FIXED_SIZE + getFileNameLength() + getExtraFieldLength();
		this.offset = offset;
		Long nextOffset = offsets.ceiling(offset);
		if (nextOffset != null) {
			setFileData(data.slice(offset, nextOffset));
			foundPk = true;
		} else {
			this.data = data;
		}
	}

	@Override
	public void link(CentralDirectoryFileHeader directoryFileHeader) {
		super.link(directoryFileHeader);
		if (!foundPk) {
			long fileDataLength;
			if (getCompressionMethod() == ZipCompressions.STORED) {
				fileDataLength = directoryFileHeader.getUncompressedSize() & 0xffffffffL;
			} else {
				fileDataLength = directoryFileHeader.getCompressedSize() & 0xffffffffL;
			}
			setFileData(data.sliceOf(offset, fileDataLength));
			data = null;
		}
	}
}
