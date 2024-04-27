package software.coley.lljzip.format.read;

import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.MemorySegmentUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.foreign.MemorySegment;

/**
 * A naive strategy that only looks for {@link LocalFileHeader} entries.
 *
 * @author Matt Coley
 */
public class NaiveLocalFileZipReader extends AbstractZipReader {
	/**
	 * New reader with simple allocator.
	 */
	public NaiveLocalFileZipReader() {
		super(new SimpleZipPartAllocator());
	}

	/**
	 * New reader with given allocator.
	 *
	 * @param allocator
	 * 		Allocator to use.
	 */
	public NaiveLocalFileZipReader(@Nonnull ZipPartAllocator allocator) {
		super(allocator);
	}

	@Override
	public void read(@Nonnull ZipArchive zip, @Nonnull MemorySegment data) throws IOException {
		long localFileOffset = MemorySegmentUtil.indexOfQuad(data, 0, ZipPatterns.LOCAL_FILE_HEADER_QUAD);
		if (localFileOffset < 0) return;
		if (localFileOffset > 0) {
			// The first offset containing archive data is not at the first byte.
			// Record whatever content is at the front.
			zip.setPrefixData(data.asSlice(0, localFileOffset));
		}
		do {
			LocalFileHeader file = newLocalFileHeader();
			file.read(data, localFileOffset);
			zip.addPart(file);
			postProcessLocalFileHeader(file);
		} while ((localFileOffset = MemorySegmentUtil.indexOfQuad(data, localFileOffset + 1, ZipPatterns.LOCAL_FILE_HEADER_QUAD)) >= 0);
	}
}
