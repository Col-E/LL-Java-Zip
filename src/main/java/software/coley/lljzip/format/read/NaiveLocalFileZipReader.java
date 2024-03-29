package software.coley.lljzip.format.read;

import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;

import javax.annotation.Nonnull;
import java.io.IOException;

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
	public void read(@Nonnull ZipArchive zip, @Nonnull ByteData data) throws IOException {
		long localFileOffset = -1;
		while ((localFileOffset = ByteDataUtil.indexOfQuad(data, localFileOffset + 1, ZipPatterns.LOCAL_FILE_HEADER_QUAD)) >= 0) {
			LocalFileHeader file = newLocalFileHeader();
			file.read(data, localFileOffset);
			zip.addPart(file);
			postProcessLocalFileHeader(file);
		}
	}
}
