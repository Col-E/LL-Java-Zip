package software.coley.lljzip.format.transform;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.model.ZipPart;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Mapper outline for zip parts.
 *
 * @author Matt Coley
 */
public interface ZipPartMapper {
	/**
	 * @param archive
	 * 		Containing archive.
	 * @param part
	 * 		Part to map.
	 *
	 * @return Mapped part, or {@code null} on removal.
	 */
	@Nullable
	default ZipPart map(@Nonnull ZipArchive archive, @Nonnull ZipPart part) {
		return switch (part) {
			case LocalFileHeader localFileHeader -> mapLocal(archive, localFileHeader);
			case CentralDirectoryFileHeader centralDirectoryFileHeader ->
					mapCentral(archive, centralDirectoryFileHeader);
			case EndOfCentralDirectory endOfCentralDirectory -> mapEnd(archive, endOfCentralDirectory);
			default -> part; // Unknown part type, keep as-is.
		};
	}

	/**
	 * @param archive
	 * 		Containing archive.
	 * @param localFileHeader
	 * 		Original local file.
	 *
	 * @return Mapped local file, or {@code null} on removal.
	 */
	@Nullable
	LocalFileHeader mapLocal(@Nonnull ZipArchive archive, @Nonnull LocalFileHeader localFileHeader);

	/**
	 * @param archive
	 * 		Containing archive.
	 * @param centralDirectoryFileHeader
	 * 		Original central directory file.
	 *
	 * @return Mapped central directory file, or {@code null} on removal.
	 */
	@Nullable
	CentralDirectoryFileHeader mapCentral(@Nonnull ZipArchive archive, @Nonnull CentralDirectoryFileHeader centralDirectoryFileHeader);

	/**
	 * @param archive
	 * 		Containing archive.
	 * @param endOfCentralDirectory
	 * 		Original end.
	 *
	 * @return Mapped end, or {@code null} on removal.
	 */
	@Nullable
	EndOfCentralDirectory mapEnd(@Nonnull ZipArchive archive, @Nonnull EndOfCentralDirectory endOfCentralDirectory);
}
