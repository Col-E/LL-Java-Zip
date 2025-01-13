package software.coley.lljzip.format.transform;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import javax.annotation.Nonnull;

/**
 * Patches file paths of class files ending with a trailing slash.
 * The JVM allows for this case for <i>some</i> reason.
 *
 * @author Matt Coley
 */
public class JvmClassDirectoryMapper extends DelegatingZipPartMapper {
	/**
	 * @param delegate
	 * 		Part mapper to delegate to.
	 */
	public JvmClassDirectoryMapper(@Nonnull ZipPartMapper delegate) {
		super(delegate);
	}

	@Nonnull
	@Override
	public LocalFileHeader mapLocal(@Nonnull ZipArchive archive, @Nonnull LocalFileHeader localFileHeader) {
		String name = localFileHeader.getFileNameAsString();
		if (name.endsWith(".class/")) {
			int newLength = name.length() - 1;
			LocalFileHeader copy = localFileHeader.copy();
			copy.setFileName(copy.getFileName().substring(0, newLength));
			copy.setFileNameLength(newLength);
			localFileHeader = copy;
		}
		return super.mapLocal(archive, localFileHeader);
	}

	@Nonnull
	@Override
	public CentralDirectoryFileHeader mapCentral(@Nonnull ZipArchive archive, @Nonnull CentralDirectoryFileHeader centralDirectoryFileHeader) {
		String name = centralDirectoryFileHeader.getFileNameAsString();
		if (name.endsWith(".class/")) {
			int newLength = name.length() - 1;
			CentralDirectoryFileHeader copy = centralDirectoryFileHeader.copy();
			copy.setFileName(copy.getFileName().substring(0, newLength));
			copy.setFileNameLength(newLength);
			centralDirectoryFileHeader = copy;
		}
		return super.mapCentral(archive, centralDirectoryFileHeader);
	}
}
