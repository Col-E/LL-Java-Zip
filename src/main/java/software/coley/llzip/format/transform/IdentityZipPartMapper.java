package software.coley.llzip.format.transform;

import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipArchive;

import javax.annotation.Nonnull;

/**
 * Zip part mapper that returns the input part. Useful as a base to implement other mappers.
 *
 * @author Matt Coley
 */
public class IdentityZipPartMapper implements ZipPartMapper {
	@Nonnull
	@Override
	public LocalFileHeader mapLocal(@Nonnull ZipArchive archive, @Nonnull LocalFileHeader localFileHeader) {
		return localFileHeader;
	}

	@Nonnull
	@Override
	public CentralDirectoryFileHeader mapCentral(@Nonnull ZipArchive archive, @Nonnull CentralDirectoryFileHeader centralDirectoryFileHeader) {
		return centralDirectoryFileHeader;
	}

	@Nonnull
	@Override
	public EndOfCentralDirectory mapEnd(@Nonnull ZipArchive archive, @Nonnull EndOfCentralDirectory endOfCentralDirectory) {
		return endOfCentralDirectory;
	}
}
