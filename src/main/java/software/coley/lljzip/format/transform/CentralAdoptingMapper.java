package software.coley.lljzip.format.transform;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;

import javax.annotation.Nonnull;

/**
 * Patches {@link LocalFileHeader} to adopt {@link CentralDirectoryFileHeader} values.
 *
 * @author Matt Coley
 */
public class CentralAdoptingMapper extends DelegatingZipPartMapper {
	/**
	 * @param delegate
	 * 		Part mapper to delegate to.
	 */
	public CentralAdoptingMapper(@Nonnull ZipPartMapper delegate) {
		super(delegate);
	}

	@Nonnull
	@Override
	public LocalFileHeader mapLocal(@Nonnull ZipArchive archive, @Nonnull LocalFileHeader localFileHeader) {
		if (localFileHeader.getLinkedDirectoryFileHeader() != null) {
			LocalFileHeader copy = localFileHeader.copy();
			copy.adoptLinkedCentralDirectoryValues();
			return copy;
		}
		return super.mapLocal(archive, localFileHeader);
	}
}
