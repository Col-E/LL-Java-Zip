package software.coley.lljzip.format.read;

import software.coley.lljzip.format.model.AdaptingLocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A zip reader implementation delegating all the work to {@link ZipFile}.
 * <br>
 * This will write temporary files to disk in order to satisfy constructor requirements if you use
 * the standard {@link ZipReader} methods. Instead directly call {@link #fill(ZipArchive, File)} or
 * {@link #fill(ZipArchive, ZipFile)}.
 *
 * @author Matt Coley
 */
public class AdaptingZipReader implements ZipReader {
	@Override
	public void read(@Nonnull ZipArchive zip, @Nonnull ByteData data) throws IOException {
		// Java's ZipFile requires the data be on-disk
		File temp = File.createTempFile("lljzip", ".tempzip");
		try {
			Files.write(temp.toPath(), ByteDataUtil.toByteArray(data));
			fill(zip, temp);
		} finally {
			temp.delete();
		}
	}

	/**
	 * @param to
	 * 		Archive to fill.
	 * @param from
	 * 		Archive to adapt from.
	 *
	 * @throws IOException
	 * 		When the zip file cannot be read.
	 */
	public static void fill(@Nonnull ZipArchive to, @Nonnull File from) throws IOException {
		try (ZipFile zipFile = new ZipFile(from)) {
			fill(to, zipFile);
		}
	}

	/**
	 * @param to
	 * 		Archive to fill.
	 * @param from
	 * 		Archive to adapt from.
	 *
	 * @throws IOException
	 * 		When the zip file cannot be read.
	 */
	public static void fill(@Nonnull ZipArchive to, @Nonnull ZipFile from) throws IOException {
		Enumeration<? extends ZipEntry> zipEntries = from.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry entry = zipEntries.nextElement();
			to.addPart(new AdaptingLocalFileHeader(from, entry));
		}
	}
}
