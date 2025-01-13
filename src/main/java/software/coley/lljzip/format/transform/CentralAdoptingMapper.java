package software.coley.lljzip.format.transform;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.model.ZipParseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

	@Nullable
	@Override
	public LocalFileHeader mapLocal(@Nonnull ZipArchive archive, @Nonnull LocalFileHeader localFileHeader) {
		if (localFileHeader.getLinkedDirectoryFileHeader() != null) {
			LocalFileHeader copy = localFileHeader.copy();
			try {
				copy.adoptLinkedCentralDirectoryValues();
			} catch (ZipParseException ex) {
				throw new IllegalStateException(ex);
			}
			return copy;
		}
		return super.mapLocal(archive, localFileHeader);
	}
}
