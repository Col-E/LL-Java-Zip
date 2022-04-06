package software.coley.llzip.part;

import software.coley.llzip.ZipPatterns;
import software.coley.llzip.util.Buffers;

import java.nio.ByteBuffer;

/**
 * An extension of {@link LocalFileHeader} with adjustments to the file-data parse logic to support
 * how the Hotspot JVM handles parsing jar files.
 *
 * @author Matt Coley
 * @author Wolfie / win32kbase <i>(Reverse engineering JVM specific zip handling)</i>
 */
public class JvmLocalFileHeader extends LocalFileHeader {
	@Override
	public void read(ByteBuffer data, int offset) {
		super.read(data, offset);
		int start = offset + 30 + getFileNameLength() + getExtraFieldLength();
		// JVM file data reading does NOT use the compressed/uncompressed fields.
		// Instead, it scans data until the next header/EOF.
		int nextPk = Buffers.indexOf(data, offset + 1, ZipPatterns.PK);
		if (nextPk == -1)
			nextPk = Buffers.length(data);
		ByteBuffer slice = Buffers.sliceExact(data, start, nextPk);
		setFileData(slice);
	}
}
