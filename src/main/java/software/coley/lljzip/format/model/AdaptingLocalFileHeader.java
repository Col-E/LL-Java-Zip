package software.coley.lljzip.format.model;

import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.data.MemorySegmentData;
import software.coley.lljzip.util.data.StringData;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Local file header implementation that pulls values from {@link ZipEntry}.
 *
 * @author Matt Coley
 */
public class AdaptingLocalFileHeader extends LocalFileHeader {
	private static final int BUFFER_SIZE = 2048;

	/**
	 * @param archive
	 * 		Containing archive holding the entry.
	 * @param entry
	 * 		Entry to adapt.
	 *
	 * @throws IOException
	 * 		When the entry cannot be read from the archive.
	 */
	public AdaptingLocalFileHeader(@Nonnull ZipFile archive, @Nonnull ZipEntry entry) throws IOException {
		InputStream inputStream = archive.getInputStream(entry);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[BUFFER_SIZE];
		int read;
		while ((read = inputStream.read(buffer)) != -1)
			outputStream.write(buffer, 0, read);
		byte[] entryData = outputStream.toByteArray();
		String entryName = entry.getName();
		byte[] extra = entry.getExtra();

		fileNameLength = entryName.length();
		fileName = StringData.of(entryName);
		fileData = MemorySegmentData.of(entryData);
		compressionMethod = 0;
		uncompressedSize = entryData.length;
		compressedSize = entryData.length;
		crc32 = (int) entry.getCrc();
		if (extra != null && extra.length > 0) {
			extraFieldLength = extra.length;
			extraField = MemorySegmentData.of(extra);
		} else {
			extraFieldLength = 0;
			extraField = MemorySegmentData.empty();
		}
		data = MemorySegmentUtil.EMPTY;
	}
}
