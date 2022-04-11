package software.coley.llzip.part;

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
		// Instead, it scans data until the next header/EOF.
		offset += MIN_FIXED_SIZE + getFileNameLength() + getExtraFieldLength();
		Long nextOffset = offsets.ceiling(offset);
		if (nextOffset != null)
			setFileData(data.slice(offset, nextOffset));
		else
			setFileData(data.slice(offset, data.length()));
	}
}
