package software.coley.llzip.format.write;

import software.coley.llzip.format.ZipPatterns;
import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Manually recomputes the zip file format. Input data is written as-is.
 *
 * @author Ned Loynd
 */
public class CopyZipWriterStrategy implements ZipWriterStrategy {
	private static void writeShortLE(OutputStream os, int value) throws IOException {
		os.write(value & 0xFF);
		os.write((value >> 8) & 0xFF);
	}

	private static void writeIntLE(OutputStream os, int value) throws IOException {
		os.write(value & 0xFF);
		os.write((value >> 8) & 0xFF);
		os.write((value >> 16) & 0xFF);
		os.write((value >> 24) & 0xFF);
	}

	@Override
	public void write(ZipArchive archive, OutputStream os) throws IOException {
		// Write local file headers.
		for (final LocalFileHeader fileHeader : archive.getLocalFiles()) {
			// Header
			writeIntLE(os, ZipPatterns.LOCAL_FILE_HEADER_QUAD);
			// Minimum version
			writeShortLE(os, fileHeader.getVersionNeededToExtract());
			// General purpose bit flag
			writeShortLE(os, fileHeader.getGeneralPurposeBitFlag());
			// Compression method
			writeShortLE(os, fileHeader.getCompressionMethod());
			// Last modification time
			writeShortLE(os, fileHeader.getLastModFileTime());
			// Last modification date
			writeShortLE(os, fileHeader.getLastModFileDate());
			// CRC32
			writeIntLE(os, fileHeader.getCrc32());
			// Compressed size
			writeIntLE(os, (int) fileHeader.getCompressedSize());
			// Uncompressed size
			writeIntLE(os, (int) fileHeader.getUncompressedSize());
			// File name length
			writeShortLE(os, fileHeader.getFileNameLength());
			// Extra field length
			writeShortLE(os, fileHeader.getExtraFieldLength());
			// File name
			os.write(ByteDataUtil.toByteArray(fileHeader.getFileName()));
			// Extra field
			os.write(ByteDataUtil.toByteArray(fileHeader.getExtraField()));
			// Compressed data
			os.write(ByteDataUtil.toByteArray(fileHeader.getFileData()), 0, (int) fileHeader.getCompressedSize());
		}

		// Write central directory file headers.
		for (final CentralDirectoryFileHeader directory : archive.getCentralDirectories()) {
			// Header
			writeIntLE(os, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD);
			// Made by
			writeShortLE(os, directory.getVersionMadeBy());
			// Minimum version
			writeShortLE(os, directory.getVersionNeededToExtract());
			// General purpose bit flag
			writeShortLE(os, directory.getGeneralPurposeBitFlag());
			// Compression method
			writeShortLE(os, directory.getCompressionMethod());
			// Last modification time
			writeShortLE(os, directory.getLastModFileTime());
			// Last modification date
			writeShortLE(os, directory.getLastModFileDate());
			// CRC32
			writeIntLE(os, directory.getCrc32());
			// Compressed size
			writeIntLE(os, (int) directory.getCompressedSize());
			// Uncompressed size
			writeIntLE(os, (int) directory.getUncompressedSize());
			// File name length
			writeShortLE(os, directory.getFileNameLength());
			// Extra field length
			writeShortLE(os, directory.getExtraFieldLength());
			// File comment length
			writeShortLE(os, directory.getFileCommentLength());
			// Disk number where file starts
			writeShortLE(os, directory.getDiskNumberStart());
			// Internal file attributes
			writeShortLE(os, directory.getInternalFileAttributes());
			// External file attributes
			writeIntLE(os, directory.getExternalFileAttributes());
			// Relative offset of local file header
			writeIntLE(os, (int) directory.getRelativeOffsetOfLocalHeader());
			// File name
			os.write(ByteDataUtil.toByteArray(directory.getFileName()));
			// Extra field
			os.write(ByteDataUtil.toByteArray(directory.getExtraField()));
			// File comment
			os.write(ByteDataUtil.toByteArray(directory.getFileComment()));
		}

		// Write end of central directory record.
		final EndOfCentralDirectory end = archive.getEnd();
		// Header
		writeIntLE(os, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
		// Disk number
		writeShortLE(os, end.getDiskNumber());
		// Central directory start disk
		writeShortLE(os, end.getCentralDirectoryStartDisk());
		// TODO What is this?
		writeShortLE(os, end.getCentralDirectoryStartOffset());
		// Central directory entries
		writeShortLE(os, end.getNumEntries());
		// Central directory size
		writeIntLE(os, (int) end.getCentralDirectorySize());
		// Central directory offset
		writeIntLE(os, (int) end.getCentralDirectoryOffset());
		// Comment length
		writeShortLE(os, end.getZipCommentLength());
		// Comment
		os.write(ByteDataUtil.toByteArray(end.getZipComment()));
	}
}
