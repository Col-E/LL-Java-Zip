package software.coley.lljzip.format.compression;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.util.ByteData;

import java.io.IOException;

/**
 * Constants for {@link LocalFileHeader#getCompressionMethod()}.
 *
 * @author Matt Coley
 */
public interface ZipCompressions {
	/**
	 * The file is stored <i>(no compression)</i>.
	 */
	int STORED = 0;
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

	/**
	 * @param method
	 * 		Compression method value.
	 *
	 * @return Name of method.
	 */
	static String getName(int method) {
		switch (method) {
			case STORED:
				return "STORED";
			case SHRUNK:
				return "SHRUNK";
			case REDUCED_F1:
				return "REDUCED_F1";
			case REDUCED_F2:
				return "REDUCED_F2";
			case REDUCED_F3:
				return "REDUCED_F3";
			case REDUCED_F4:
				return "REDUCED_F4";
			case IMPLODED:
				return "IMPLODED";
			case RESERVED_TOKENIZING:
				return "RESERVED_TOKENIZING";
			case DEFLATED:
				return "DEFLATED";
			case DEFLATED_64:
				return "DEFLATED_64";
			case PKWARE_IMPLODING:
				return "PKWARE_IMPLODING";
			case PKWARE_RESERVED_11:
				return "PKWARE_RESERVED_11";
			case BZIP2:
				return "BZIP2";
			case PKWARE_RESERVED_13:
				return "PKWARE_RESERVED_13";
			case LZMA:
				return "LZMA";
			case PKWARE_RESERVED_15:
				return "PKWARE_RESERVED_15";
			case CMPSC:
				return "CMPSC";
			case PKWARE_RESERVED_17:
				return "PKWARE_RESERVED_17";
			case IBM_TERSE:
				return "IBM_TERSE";
			case IBM_LZ77:
				return "IBM_LZ77";
			case DEPRECATED_ZSTD:
				return "DEPRECATED_ZSTD";
			case ZSTANDARD:
				return "ZSTANDARD";
			case MP3:
				return "MP3";
			case XZ:
				return "XZ";
			case JPEG:
				return "JPEG";
			case WAVPACK:
				return "WAVPACK";
			case PPMD:
				return "PPMD";
			case AE_x:
				return "AE_x";
			default:
				return "Unknown[" + method + "]";
		}
	}

	/**
	 * @param header
	 * 		Header with {@link LocalFileHeader#getFileData()} to decompress.
	 *
	 * @return Decompressed {@code byte[]}.
	 *
	 * @throws IOException
	 * 		When the decompression failed.
	 */
	static ByteData decompress(LocalFileHeader header) throws IOException {
		int method = header.getCompressionMethod();
		switch (method) {
			case STORED:
				return header.getFileData();
			case DEFLATED:
				return header.decompress(new UnsafeDeflateDecompressor());
			default:
				// TODO: Support other decompressing techniques
				String methodName = getName(method);
				throw new IOException("Unsupported compression method: " + methodName);
		}
	}
}
