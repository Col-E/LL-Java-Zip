package software.coley.llzip.part;

import software.coley.llzip.ZipCompressions;
import software.coley.llzip.strategy.Decompressor;
import software.coley.llzip.util.Buffers;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * ZIP LocalFileHeader structure.
 *
 * @author Matt Coley
 */
public class LocalFileHeader implements ZipPart, ZipRead {
	private transient int offset = -1;
	private transient CentralDirectoryFileHeader linkedDirectoryFileHeader;
	// Zip spec elements
	private int versionNeededToExtract;
	private int generalPurposeBitFlag;
	private int compressionMethod;
	private int lastModFileTime;
	private int lastModFileDate;
	private int crc32;
	private int compressedSize;
	private int uncompressedSize;
	private int fileNameLength;
	private int extraFieldLength;
	private ByteBuffer fileName;
	private ByteBuffer extraField;
	private ByteBuffer fileData;
	
	private transient String fileNameCache;

	@Override
	public void read(ByteBuffer data, int offset) {
		this.offset = offset;
		versionNeededToExtract = Buffers.readWord(data, offset + 4);
		generalPurposeBitFlag = Buffers.readWord(data, offset + 6);
		compressionMethod = Buffers.readWord(data, offset + 8);
		lastModFileTime = Buffers.readWord(data, offset + 10);
		lastModFileDate = Buffers.readWord(data, offset + 12);
		crc32 = Buffers.readQuad(data, offset + 14);
		compressedSize = Buffers.readQuad(data, offset + 18);
		uncompressedSize = Buffers.readQuad(data, offset + 22);
		fileNameLength = Buffers.readWord(data, offset + 26);
		extraFieldLength = Buffers.readWord(data, offset + 28);
		fileName = Buffers.slice(data, offset + 30, fileNameLength);
		extraField = Buffers.slice(data, offset + 30 + fileNameLength, extraFieldLength);
		fileData = Buffers.slice(data, offset + 30 + fileNameLength + extraFieldLength, compressedSize);
	}

	@Override
	public int length() {
		return 30 + Buffers.length(fileName) + Buffers.length(extraField) + Buffers.length(fileData);
	}

	@Override
	public PartType type() {
		return PartType.LOCAL_FILE_HEADER;
	}

	@Override
	public int offset() {
		return offset;
	}

	/**
	 * @param decompressor
	 * 		Decompressor implementation.
	 *
	 * @return Decompressed bytes.
	 *
	 * @throws IOException
	 * 		When the decompressor fails.
	 */
	public ByteBuffer decompress(Decompressor decompressor) throws IOException {
		return decompressor.decompress(this, fileData);
	}

	/**
	 * @return The central directory file header this file is associated with.
	 */
	public CentralDirectoryFileHeader getLinkedDirectoryFileHeader() {
		return linkedDirectoryFileHeader;
	}

	/**
	 * @param directoryFileHeader
	 * 		The central directory file header this file is associated with.
	 */
	public void link(CentralDirectoryFileHeader directoryFileHeader) {
		this.linkedDirectoryFileHeader = directoryFileHeader;
	}

	/**
	 * @return Zip software version.
	 */
	public int getVersionNeededToExtract() {
		return versionNeededToExtract;
	}

	/**
	 * @param versionNeededToExtract
	 * 		Zip software version.
	 */
	public void setVersionNeededToExtract(int versionNeededToExtract) {
		this.versionNeededToExtract = versionNeededToExtract;
	}

	public int getGeneralPurposeBitFlag() {
		return generalPurposeBitFlag;
	}

	public void setGeneralPurposeBitFlag(int generalPurposeBitFlag) {
		this.generalPurposeBitFlag = generalPurposeBitFlag;
	}

	/**
	 * @return Method to use for {@link #decompress(Decompressor) decompressing data}.
	 *
	 * @see ZipCompressions Possible methods.
	 */
	public int getCompressionMethod() {
		return compressionMethod;
	}

	/**
	 * @param compressionMethod
	 * 		Method to use for {@link #decompress(Decompressor) decompressing data}.
	 *
	 * @see ZipCompressions Possible methods.
	 */
	public void setCompressionMethod(int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	/**
	 * @return Modification time of the file.
	 */
	public int getLastModFileTime() {
		return lastModFileTime;
	}

	/**
	 * @param lastModFileTime
	 * 		Modification time of the file.
	 */
	public void setLastModFileTime(int lastModFileTime) {
		this.lastModFileTime = lastModFileTime;
	}

	/**
	 * @return Modification date of the file.
	 */
	public int getLastModFileDate() {
		return lastModFileDate;
	}

	/**
	 * @param lastModFileDate
	 * 		Modification date of the file.
	 */
	public void setLastModFileDate(int lastModFileDate) {
		this.lastModFileDate = lastModFileDate;
	}

	/**
	 * @return File checksum.
	 */
	public int getCrc32() {
		return crc32;
	}

	/**
	 * @param crc32
	 * 		File checksum.
	 */
	public void setCrc32(int crc32) {
		this.crc32 = crc32;
	}

	/**
	 * Be aware that these attributes can be falsified.
	 * Different zip-parsing programs treat the files differently
	 * and may not adhere to what you expect from the zip specification.
	 * <br>
	 * When in doubt, trust {@link byte[]#length()} from {@link #getFileData()}.
	 *
	 * @return Compressed size of {@link #getFileData()}.
	 */
	public int getCompressedSize() {
		return compressedSize;
	}

	/**
	 * @param compressedSize
	 * 		Compressed size of {@link #getFileData()}.
	 */
	public void setCompressedSize(int compressedSize) {
		this.compressedSize = compressedSize;
	}

	/**
	 * Be aware that these attributes can be falsified.
	 * Different zip-parsing programs treat the files differently
	 * and may not adhere to what you expect from the zip specification.
	 *
	 * @return Uncompressed size after {@link #decompress(Decompressor)} is used on {@link #getFileData()}.
	 */
	public int getUncompressedSize() {
		return uncompressedSize;
	}

	/**
	 * @param uncompressedSize
	 * 		Uncompressed size after {@link #decompress(Decompressor)} is used on {@link #getFileData()}.
	 */
	public void setUncompressedSize(int uncompressedSize) {
		this.uncompressedSize = uncompressedSize;
	}

	/**
	 * @return Length of {@link #getFileName()}.
	 */
	public int getFileNameLength() {
		return fileNameLength;
	}

	/**
	 * @param fileNameLength
	 * 		Length of {@link #getFileName()}.
	 */
	public void setFileNameLength(int fileNameLength) {
		this.fileNameLength = fileNameLength;
	}

	/**
	 * @return Length of {@link #getExtraField()}
	 */
	public int getExtraFieldLength() {
		return extraFieldLength;
	}

	/**
	 * @param extraFieldLength
	 * 		Length of {@link #getExtraField()}
	 */
	public void setExtraFieldLength(int extraFieldLength) {
		this.extraFieldLength = extraFieldLength;
	}

	/**
	 * Should match {@link CentralDirectoryFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, the central directory file name should be trusted instead.
	 *
	 * @return File name.
	 */
	public ByteBuffer getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 * 		File name.
	 */
	public void setFileName(ByteBuffer fileName) {
		this.fileName = fileName;
		fileNameCache = null;
	}

	/**
	 * Should match {@link CentralDirectoryFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, the central directory file name should be trusted instead.
	 *
	 * @return File name.
	 */
	public String getFileNameAsString() {
		String fileNameCache = this.fileNameCache;
		if (fileNameCache == null) {
			return this.fileNameCache = Buffers.toString(fileName);
		}
		return fileNameCache;
	}

	/**
	 * @return May be used for extra compression information,
	 * depending on the {@link #getCompressionMethod() compression method} used.
	 */
	public ByteBuffer getExtraField() {
		return extraField;
	}

	/**
	 * @param extraField
	 * 		Extra field bytes.
	 */
	public void setExtraField(ByteBuffer extraField) {
		this.extraField = extraField;
	}

	/**
	 * @return Compressed file contents.
	 *
	 * @see #decompress(Decompressor) Decompresses this data.
	 */
	public ByteBuffer getFileData() {
		return fileData;
	}

	/**
	 * @param fileData
	 * 		Compressed file contents.
	 */
	public void setFileData(ByteBuffer fileData) {
		this.fileData = fileData;
	}
}
