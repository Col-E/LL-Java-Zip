package software.coley.lljzip.format.write;

import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ByteDataUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Uses the Java {@link ZipOutputStream} to recompute the zip file format.
 * The only used data in this case is {@link LocalFileHeader#getFileData() local file data} and the
 * {@link LocalFileHeader#getFileName() local file name}.
 *
 * @author Matt Coley
 */
public class JavaZipWriter implements ZipWriter {
	private final boolean createDirectoryEntries;

	/**
	 * New writer, which will create directories.
	 */
	public JavaZipWriter() {
		this(false);
	}

	/**
	 * New writer.
	 *
	 * @param createDirectoryEntries
	 *        {@code true} to create directory entries.
	 * 		Some ZIP tools will make entries for directory paths, though this is not strictly required.
	 */
	public JavaZipWriter(boolean createDirectoryEntries) {
		this.createDirectoryEntries = createDirectoryEntries;
	}

	@Override
	public void write(@Nonnull ZipArchive archive, @Nonnull OutputStream os) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			for (LocalFileHeader fileHeader : archive.getLocalFiles()) {
				String name = fileHeader.getFileNameAsString();
				if (fileHeader.getFileData().length() > 0L) {
					zos.putNextEntry(new ZipEntry(name));
					zos.write(ByteDataUtil.toByteArray(ZipCompressions.decompress(fileHeader)));
					zos.closeEntry();
				} else if (createDirectoryEntries) {
					// Directory, don't need to write anything
					zos.putNextEntry(new ZipEntry(name));
					zos.closeEntry();
				}
			}
		}
	}
}
