package software.coley.lljzip.format.compression;

import software.coley.lljzip.format.model.LocalFileHeader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.MemorySegment;
import java.util.zip.DeflaterInputStream;

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
		return switch (method) {
			case STORED -> "STORED";
			case SHRUNK -> "SHRUNK";
			case REDUCED_F1 -> "REDUCED_F1";
			case REDUCED_F2 -> "REDUCED_F2";
			case REDUCED_F3 -> "REDUCED_F3";
			case REDUCED_F4 -> "REDUCED_F4";
			case IMPLODED -> "IMPLODED";
			case RESERVED_TOKENIZING -> "RESERVED_TOKENIZING";
			case DEFLATED -> "DEFLATED";
			case DEFLATED_64 -> "DEFLATED_64";
			case PKWARE_IMPLODING -> "PKWARE_IMPLODING";
			case PKWARE_RESERVED_11 -> "PKWARE_RESERVED_11";
			case BZIP2 -> "BZIP2";
			case PKWARE_RESERVED_13 -> "PKWARE_RESERVED_13";
			case LZMA -> "LZMA";
			case PKWARE_RESERVED_15 -> "PKWARE_RESERVED_15";
			case CMPSC -> "CMPSC";
			case PKWARE_RESERVED_17 -> "PKWARE_RESERVED_17";
			case IBM_TERSE -> "IBM_TERSE";
			case IBM_LZ77 -> "IBM_LZ77";
			case DEPRECATED_ZSTD -> "DEPRECATED_ZSTD";
			case ZSTANDARD -> "ZSTANDARD";
			case MP3 -> "MP3";
			case XZ -> "XZ";
			case JPEG -> "JPEG";
			case WAVPACK -> "WAVPACK";
			case PPMD -> "PPMD";
			case AE_x -> "AE_x";
			default -> "Unknown[" + method + "]";
		};
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
	static MemorySegment decompress(LocalFileHeader header) throws IOException {
		int method = header.getCompressionMethod();
		return switch (method) {
			case STORED -> header.getFileData();
			case DEFLATED -> header.decompress(UnsafeDeflateDecompressor.INSTANCE);
			default -> {
				// TODO: Support other decompressing techniques
				String methodName = getName(method);
				throw new IOException("Unsupported compression method: " + methodName);
			}
		};
	}

	/**
	 * @param header
	 * 		Header with {@link LocalFileHeader#getFileData()} to decompress.
	 *
	 * @return Stream with decompressed data.
	 *
	 * @throws IOException
	 * 		When the decompression failed.
	 */
	static InputStream decompressStream(LocalFileHeader header) throws IOException {
		int method = header.getCompressionMethod();
		InputStream in = new MemorySegmentInputStream(header.getFileData());
		return switch (method) {
			case STORED -> in;
			case DEFLATED -> new DeflaterInputStream(in);
			default -> {
				// TODO: Support other decompressing techniques
				String methodName = getName(method);
				throw new IOException("Unsupported compression method: " + methodName);
			}
		};
	}
}
