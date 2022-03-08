package software.coley.llzip.part;

import software.coley.llzip.ZipPatterns;
import software.coley.llzip.util.Array;

import java.util.Arrays;

/**
 * An extension of {@link LocalFileHeader} with adjustments to the file-data parse logic to support
 * how the Hotspot JVM handles parsing jar files.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmLocalFileHeader extends LocalFileHeader {
	@Override
	public void read(byte[] data, int offset) {
		super.read(data, offset);
		int start = offset + 30 + getFileNameLength() + getExtraFieldLength();
		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header/EOF.
		int nextPk = Array.indexOf(data, offset + 1, ZipPatterns.PK);
		if (nextPk == -1)
			nextPk = data.length;
		byte[] fileData = Arrays.copyOfRange(data, start, nextPk);
		setFileData(fileData);
	}
}
