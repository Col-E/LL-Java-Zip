package software.coley.llzip.part;

import java.util.Arrays;

/**
 * An extension of {@link LocalFileHeader} with adjustments to the file-data parse logic to support
 * how the Hotspot JVM handles parsing jar files.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmLocalFileHeader extends LocalFileHeader {
	private final int nextPkPos;

	/**
	 * @param nextPkPos
	 * 		Next offset of any {@link ZipPart}.
	 * 		May also be the position of the end of the file.
	 */
	public JvmLocalFileHeader(int nextPkPos) {
		this.nextPkPos = nextPkPos;
	}

	@Override
	public void read(byte[] data, int offset) {
		super.read(data, offset);
		int start = offset + 30 + getFileNameLength() + getExtraFieldLength();
		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header/EOF.
		byte[] fileData = Arrays.copyOfRange(data, start, nextPkPos);
		setFileData(fileData);
	}
}
