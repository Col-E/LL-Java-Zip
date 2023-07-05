package software.coley.lljzip.format.model;

import javax.annotation.Nonnull;

/**
 * Outline of a zip section.
 *
 * @author Matt Coley
 * @see CentralDirectoryFileHeader
 * @see EndOfCentralDirectory
 * @see LocalFileHeader
 */
public interface ZipPart {
	/**
	 * @return Length of the part.
	 */
	long length();

	/**
	 * @return Implementation type.
	 */
	@Nonnull
	PartType type();

	/**
	 * Optional offset information.
	 *
	 * @return Offset of part in the ZIP file.
	 * May be {@code -1} to indicate unknown offset.
	 */
	long offset();

	/**
	 * @return {@code true} when {@link #offset()} information is present.
	 */
	default boolean hasOffset() {
		return offset() >= 0L;
	}
}
