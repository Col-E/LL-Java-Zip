package software.coley.llzip.strategy;

import software.coley.llzip.ZipArchive;
import software.coley.llzip.ZipCompressions;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.LocalFileHeader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Uses the Java {@link ZipOutputStream} to recompute the zip file format.
 * The only used data in this case is {@link LocalFileHeader#getFileData()} and the file name
 * which can be either {@link CentralDirectoryFileHeader#getFileName()} or {@link LocalFileHeader#getFileName()}.
 *
 * @author Matt Coley
 */
public class JavaZipWriterStategy implements ZipWriterStrategy {
	@Override
	public void write(ZipArchive archive, OutputStream os) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(os)) {
			for (LocalFileHeader fileHeader : archive.getLocalFiles()) {
				String name = Optional.ofNullable(fileHeader.getLinkedDirectoryFileHeader())
						.map(CentralDirectoryFileHeader::getFileName)
						.orElse(fileHeader.getFileName());
				zos.putNextEntry(new ZipEntry(name));
				zos.write(ZipCompressions.decompress(fileHeader));
				zos.closeEntry();
			}
		}
	}
}