package software.coley.llzip;

import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.EndOfCentralDirectory;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.util.ByteDataUtil;

/**
 * Patterns for usage in {@link ByteDataUtil} methods.
 *
 * @author Matt Coley
 */
public interface ZipPatterns {
	/**
	 * Any PK header match.
	 */
	int[] PK = {0x50, 0x4B};
	/**
	 * Header for {@link LocalFileHeader}.
	 */
	int[] LOCAL_FILE_HEADER = {0x50, 0x4B, 0x03, 0x04};
	/**
	 * Header for {@link CentralDirectoryFileHeader}.
	 */
	int[] CENTRAL_DIRECTORY_FILE_HEADER = {0x50, 0x4B, 0x01, 0x02};
	/**
	 * Header for {@link EndOfCentralDirectory}.
	 */
	int[] END_OF_CENTRAL_DIRECTORY = {0x50, 0x4B, 0x05, 0x06};
}
