package software.coley.llzip.part;

import software.coley.llzip.ZipCompressions;
import software.coley.llzip.strategy.Decompressor;
import software.coley.llzip.util.Array;

import java.io.IOException;

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
	private String fileName;
	private byte[] extraField;
	private byte[] fileData;

	@Override
	public void read(byte[] data, int offset) {
		this.offset = offset;
		versionNeededToExtract = Array.readWord(data, offset + 4);
		generalPurposeBitFlag = Array.readWord(data, offset + 6);
		compressionMethod = Array.readWord(data, offset + 8);
		lastModFileTime = Array.readWord(data, offset + 10);
		lastModFileDate = Array.readWord(data, offset + 12);
		crc32 = Array.readQuad(data, offset + 14);
		compressedSize = Array.readQuad(data, offset + 18);
		uncompressedSize = Array.readQuad(data, offset + 22);
		fileNameLength = Array.readWord(data, offset + 26);
		extraFieldLength = Array.readWord(data, offset + 28);
		fileName = Array.readString(data, offset + 30, fileNameLength);
		extraField = Array.readArray(data, offset + 30 + fileNameLength, extraFieldLength);
		fileData = Array.readArray(data, offset + 30 + fileNameLength + extraFieldLength, compressedSize);
	}

	@Override
	public int length() {
		return 30 + fileName.length() + extraField.length + fileData.length;
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
	public byte[] decompress(Decompressor decompressor) throws IOException {
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
	public String getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 * 		File name.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return May be used for extra compression information,
	 * depending on the {@link #getCompressionMethod() compression method} used.
	 */
	public byte[] getExtraField() {
		return extraField;
	}

	/**
	 * @param extraField
	 * 		Extra field bytes.
	 */
	public void setExtraField(byte[] extraField) {
		this.extraField = extraField;
	}

	/**
	 * @return Compressed file contents.
	 *
	 * @see #decompress(Decompressor) Decompresses this data.
	 */
	public byte[] getFileData() {
		return fileData;
	}

	/**
	 * @param fileData
	 * 		Compressed file contents.
	 */
	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
}
