package software.coley.lljzip.format.transform;

import software.coley.lljzip.format.model.*;

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
		if (part instanceof LocalFileHeader) {
			return mapLocal(archive, (LocalFileHeader) part);
		} else if (part instanceof CentralDirectoryFileHeader) {
			return mapCentral(archive, (CentralDirectoryFileHeader) part);
		} else if (part instanceof EndOfCentralDirectory) {
			return mapEnd(archive, (EndOfCentralDirectory) part);
		}
		// Unknown part type, keep as-is.
		return part;
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
