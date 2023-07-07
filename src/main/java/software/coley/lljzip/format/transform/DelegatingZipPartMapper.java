package software.coley.lljzip.format.transform;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Zip part mapper that delegates to another mapper. Useful as a base to implement other mappers.
 *
 * @author Matt Coley
 */
public class DelegatingZipPartMapper implements ZipPartMapper {
	private final ZipPartMapper delegate;

	/**
	 * @param delegate
	 * 		Part mapper to delegate to.
	 */
	public DelegatingZipPartMapper(@Nonnull ZipPartMapper delegate) {
		this.delegate = delegate;
	}

	@Nullable
	@Override
	public LocalFileHeader mapLocal(@Nonnull ZipArchive archive, @Nonnull LocalFileHeader localFileHeader) {
		return delegate.mapLocal(archive, localFileHeader);
	}

	@Nullable
	@Override
	public CentralDirectoryFileHeader mapCentral(@Nonnull ZipArchive archive, @Nonnull CentralDirectoryFileHeader centralDirectoryFileHeader) {
		return delegate.mapCentral(archive, centralDirectoryFileHeader);
	}

	@Nullable
	@Override
	public EndOfCentralDirectory mapEnd(@Nonnull ZipArchive archive, @Nonnull EndOfCentralDirectory endOfCentralDirectory) {
		return delegate.mapEnd(archive, endOfCentralDirectory);
	}
}
