package software.coley.lljzip.format.model;

import software.coley.lljzip.util.BufferData;
import software.coley.lljzip.util.NoopByteData;
import software.coley.lljzip.util.lazy.LazyByteData;
import software.coley.lljzip.util.lazy.LazyInt;
import software.coley.lljzip.util.lazy.LazyLong;

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
		versionNeededToExtract = new LazyInt(() -> 0);
		generalPurposeBitFlag = new LazyInt(() -> 0);
		lastModFileTime = new LazyInt(() -> 0);
		lastModFileDate = new LazyInt(() -> 0);
		fileNameLength = new LazyInt(entryName::length);
		fileName = new LazyByteData(() -> BufferData.wrap(entryName.getBytes()));
		fileDataLength = new LazyLong(() -> entryData.length);
		fileData = new LazyByteData(() -> BufferData.wrap(entryData));
		compressionMethod = new LazyInt(() -> 0);
		uncompressedSize = new LazyLong(() -> entryData.length);
		compressedSize = new LazyLong(() -> entryData.length);
		crc32 = new LazyInt(() -> (int) entry.getCrc());
		if (extra != null) {
			extraFieldLength = new LazyInt(() -> extra.length);
			extraField = new LazyByteData(() -> BufferData.wrap(extra));
		} else {
			extraFieldLength = new LazyInt(() -> 0);
			extraField = new LazyByteData(() -> NoopByteData.INSTANCE);
		}
		data = NoopByteData.INSTANCE;
	}
}
