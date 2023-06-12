package software.coley.llzip.format;

import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.LocalFileHeader;
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
	 * Any PK header match, as a {@code u2/word/short}
	 */
	int PK_WORD = 0x4B_50;
	/**
	 * Header for {@link LocalFileHeader}.
	 */
	int[] LOCAL_FILE_HEADER = {0x50, 0x4B, 0x03, 0x04};
	/**
	 * Header for {@link LocalFileHeader}, as a {@code s4/quad/int}
	 */
	int LOCAL_FILE_HEADER_QUAD = 0x04_03_4B_50;
	/**
	 * Header for {@link CentralDirectoryFileHeader}.
	 */
	int[] CENTRAL_DIRECTORY_FILE_HEADER = {0x50, 0x4B, 0x01, 0x02};
	/**
	 * Header for {@link CentralDirectoryFileHeader}, as a {@code s4/quad/int}
	 */
	int CENTRAL_DIRECTORY_FILE_HEADER_QUAD = 0x02_01_4B_50;
	/**
	 * Header for {@link EndOfCentralDirectory}.
	 */
	int[] END_OF_CENTRAL_DIRECTORY = {0x50, 0x4B, 0x05, 0x06};
	/**
	 * Header for {@link EndOfCentralDirectory}, as a {@code s4/quad/int}
	 */
	int END_OF_CENTRAL_DIRECTORY_QUAD = 0x06_05_4B_50;
}
