package software.coley.llzip.part;

import software.coley.llzip.ZipPatterns;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

/**
 * An extension of {@link LocalFileHeader} with adjustments to the file-data parse logic to support
 * how the Hotspot JVM handles parsing jar files.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmLocalFileHeader extends LocalFileHeader {
	@Override
	public void read(ByteData data, long offset) {
		super.read(data, offset);
		long start = offset + 30 + getFileNameLength() + getExtraFieldLength();
		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header/EOF.
		long nextPk = ByteDataUtil.indexOf(data, offset + 1, ZipPatterns.PK);
		if (nextPk == -1L)
			nextPk = data.length();
		ByteData slice = data.slice(start, nextPk);
		setFileData(slice);
	}
}
