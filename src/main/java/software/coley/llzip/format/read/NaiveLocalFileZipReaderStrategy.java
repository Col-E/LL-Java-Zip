package software.coley.llzip.format.read;

import software.coley.llzip.format.ZipPatterns;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;

/**
 * A naive strategy that only looks for {@link LocalFileHeader} entries.
 *
 * @author Matt Coley
 */
public class NaiveLocalFileZipReaderStrategy implements ZipReaderStrategy {
	@Override
	public void read(ZipArchive zip, ByteData data) throws IOException {
		long localFileOffset = -1;
		while ((localFileOffset = ByteDataUtil.indexOf(data, localFileOffset, ZipPatterns.LOCAL_FILE_HEADER)) >= 0) {
			LocalFileHeader file = new LocalFileHeader();
			file.read(data, localFileOffset);
			zip.getParts().add(file);
			postProcessLocalFileHeader(file);
			file.freeze();
		}
	}
}
