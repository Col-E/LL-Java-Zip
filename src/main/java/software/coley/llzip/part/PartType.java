package software.coley.llzip.part;

/**
 * Part sections.
 *
 * @author Matt Coley
 */
public enum PartType {
	/**
	 * @see LocalFileHeader
	 */
	LOCAL_FILE_HEADER,
	/**
	 * @see CentralDirectoryFileHeader
	 */
	CENTRAL_DIRECTORY_FILE_HEADER,
	/**
	 * @see EndOfCentralDirectory
	 */
	END_OF_CENTRAL_DIRECTORY
}
