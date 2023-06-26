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
 * Manually copies the input zip file.
 * Data is written as-is, and no validation is performed.
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
			writeIntLE(os, ZipPatterns.LOCAL_FILE_HEADER_QUAD);
			writeShortLE(os, fileHeader.getVersionNeededToExtract());
			writeShortLE(os, fileHeader.getGeneralPurposeBitFlag());
			writeShortLE(os, fileHeader.getCompressionMethod());
			writeShortLE(os, fileHeader.getLastModFileTime());
			writeShortLE(os, fileHeader.getLastModFileDate());
			writeIntLE(os, fileHeader.getCrc32());
			writeIntLE(os, (int) fileHeader.getCompressedSize());
			writeIntLE(os, (int) fileHeader.getUncompressedSize());
			writeShortLE(os, fileHeader.getFileNameLength());
			writeShortLE(os, fileHeader.getExtraFieldLength());
			os.write(ByteDataUtil.toByteArray(fileHeader.getFileName()));
			os.write(ByteDataUtil.toByteArray(fileHeader.getExtraField()));
			os.write(ByteDataUtil.toByteArray(fileHeader.getFileData()));
		}

		// Write central directory file headers.
		for (final CentralDirectoryFileHeader directory : archive.getCentralDirectories()) {
			writeIntLE(os, ZipPatterns.CENTRAL_DIRECTORY_FILE_HEADER_QUAD);
			writeShortLE(os, directory.getVersionMadeBy());
			writeShortLE(os, directory.getVersionNeededToExtract());
			writeShortLE(os, directory.getGeneralPurposeBitFlag());
			writeShortLE(os, directory.getCompressionMethod());
			writeShortLE(os, directory.getLastModFileTime());
			writeShortLE(os, directory.getLastModFileDate());
			writeIntLE(os, directory.getCrc32());
			writeIntLE(os, (int) directory.getCompressedSize());
			writeIntLE(os, (int) directory.getUncompressedSize());
			writeShortLE(os, directory.getFileNameLength());
			writeShortLE(os, directory.getExtraFieldLength());
			writeShortLE(os, directory.getFileCommentLength());
			writeShortLE(os, directory.getDiskNumberStart());
			writeShortLE(os, directory.getInternalFileAttributes());
			writeIntLE(os, directory.getExternalFileAttributes());
			writeIntLE(os, (int) directory.getRelativeOffsetOfLocalHeader());
			os.write(ByteDataUtil.toByteArray(directory.getFileName()));
			os.write(ByteDataUtil.toByteArray(directory.getExtraField()));
			os.write(ByteDataUtil.toByteArray(directory.getFileComment()));
		}

		// Write end of central directory record.
		final EndOfCentralDirectory end = archive.getEnd();
		if (end != null) {
			writeIntLE(os, ZipPatterns.END_OF_CENTRAL_DIRECTORY_QUAD);
			writeShortLE(os, end.getDiskNumber());
			writeShortLE(os, end.getCentralDirectoryStartDisk());
			writeShortLE(os, end.getCentralDirectoryStartOffset());
			writeShortLE(os, end.getNumEntries());
			writeIntLE(os, (int) end.getCentralDirectorySize());
			writeIntLE(os, (int) end.getCentralDirectoryOffset());
			writeShortLE(os, end.getZipCommentLength());
			os.write(ByteDataUtil.toByteArray(end.getZipComment()));
		}
	}
}
