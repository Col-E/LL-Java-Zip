package software.coley.llzip;

import software.coley.llzip.part.LocalFileHeader;

/**
 * Constants for {@link LocalFileHeader#getCompressionMethod()}.
 *
 * @author Matt Coley
 */
public interface ZipCompressions {
	/**
	 * The file is stored <i>(no compression)</i>.
	 */
	int STRORED = 0;
	/**
	 * The file is Shrunk.
	 */
	int SHRUNK = 1;
	/**
	 * The file is Reduced with compression factor 1.
	 */
	int REDUCED_F1 = 2; //
	/**
	 * The file is Reduced with compression factor 2.
	 */
	int REDUCED_F2 = 3; //
	/**
	 * The file is Reduced with compression factor 3
	 */
	int REDUCED_F3 = 4; //
	/**
	 * The file is Reduced with compression factor 4.
	 */
	int REDUCED_F4 = 5; //
	/**
	 * The file is Imploded.
	 */
	int IMPLODED = 6; //
	/**
	 * Reserved for Tokenizing compression algorithm.
	 */
	int RESERVED_TOKENIZING = 7; //
	/**
	 * The file is Deflated.
	 */
	int DEFLATED = 8; //
	/**
	 * Enhanced Deflating using Deflate64(tm).
	 */
	int DEFLATED_64 = 9; //
	/**
	 * PKWARE Data Compression Library Imploding <i>(old IBM TERSE)</i>.
	 */
	int PKWARE_IMPLODING = 10; //
	/**
	 * Reserved by PKWARE.
	 */
	int PKWARE_RESERVED_11 = 11; //
	/**
	 * File is compressed using BZIP2 algorithm
	 */
	int BZIP2 = 12; //
	/**
	 * Reserved by PKWARE
	 */
	int PKWARE_RESERVED_13 = 13; //
	/**
	 * Lempel–Ziv–Markov chain algorithm.
	 */
	int LZMA = 14; //
	/**
	 * Reserved by PKWARE.
	 */
	int PKWARE_RESERVED_15 = 15;
	/**
	 * IBM z/OS CMPSC Compression.
	 */
	int CMPSC = 16;
	/**
	 * Reserved by PKWARE.
	 */
	int PKWARE_RESERVED_17 = 17; //
	/**
	 * File is compressed using IBM TERSE <i>(new)</i>.
	 */
	int IBM_TERSE = 18;
	/**
	 * IBM LZ77 z Architecture.
	 */
	int IBM_LZ77 = 19;
	/**
	 * Deprecated <i>(use method 93 for zstd)</i>.
	 */
	int DEPRECATED_ZSTD = 20; //
	/**
	 * Zstandard <i>(zstd)</i> Compression.
	 */
	int ZSTANDARD = 93;
	/**
	 * MP3 Compression.
	 */
	int MP3 = 94;
	/**
	 * XZ Compression
	 */
	int XZ = 95;
	/**
	 * JPEG variant.
	 */
	int JPEG = 96;
	/**
	 * WavPack compressed data.
	 */
	int WAVPACK = 97;
	/**
	 * PPMd version I, Rev 1.
	 */
	int PPMD = 98;
	/**
	 * AE-x encryption marker.
	 */
	int AE_x = 99;
}
