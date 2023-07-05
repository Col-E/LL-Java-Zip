package software.coley.lljzip.format.read;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;

import javax.annotation.Nonnull;

/**
 * Outlines part allocation.
 *
 * @author Matt Coley
 */
public interface ZipPartAllocator {
	/**
	 * @return New local file header.
	 */
	@Nonnull
	LocalFileHeader newLocalFileHeader();

	/**
	 * @return New central directory file header.
	 */
	@Nonnull
	CentralDirectoryFileHeader newCentralDirectoryFileHeader();

	/**
	 * @return New end of central directory header.
	 */
	@Nonnull
	EndOfCentralDirectory newEndOfCentralDirectory();
}
