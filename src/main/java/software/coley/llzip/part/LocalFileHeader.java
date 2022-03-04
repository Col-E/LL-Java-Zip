package software.coley.llzip.part;

import software.coley.llzip.util.Array;

/**
 * ZIP LocalFileHeader structure.
 *
 * @author Matt Coley
 */
public class LocalFileHeader implements ZipPart, ZipRead {
	private transient int offset = -1;
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
		// TODO: Encryption optional header goes here?
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

	public int getVersionNeededToExtract() {
		return versionNeededToExtract;
	}

	public void setVersionNeededToExtract(int versionNeededToExtract) {
		this.versionNeededToExtract = versionNeededToExtract;
	}

	public int getGeneralPurposeBitFlag() {
		return generalPurposeBitFlag;
	}

	public void setGeneralPurposeBitFlag(int generalPurposeBitFlag) {
		this.generalPurposeBitFlag = generalPurposeBitFlag;
	}

	public int getCompressionMethod() {
		return compressionMethod;
	}

	public void setCompressionMethod(int compressionMethod) {
		this.compressionMethod = compressionMethod;
	}

	public int getLastModFileTime() {
		return lastModFileTime;
	}

	public void setLastModFileTime(int lastModFileTime) {
		this.lastModFileTime = lastModFileTime;
	}

	public int getLastModFileDate() {
		return lastModFileDate;
	}

	public void setLastModFileDate(int lastModFileDate) {
		this.lastModFileDate = lastModFileDate;
	}

	public int getCrc32() {
		return crc32;
	}

	public void setCrc32(int crc32) {
		this.crc32 = crc32;
	}

	public int getCompressedSize() {
		return compressedSize;
	}

	public void setCompressedSize(int compressedSize) {
		this.compressedSize = compressedSize;
	}

	public int getUncompressedSize() {
		return uncompressedSize;
	}

	public void setUncompressedSize(int uncompressedSize) {
		this.uncompressedSize = uncompressedSize;
	}

	public int getFileNameLength() {
		return fileNameLength;
	}

	public void setFileNameLength(int fileNameLength) {
		this.fileNameLength = fileNameLength;
	}

	public int getExtraFieldLength() {
		return extraFieldLength;
	}

	public void setExtraFieldLength(int extraFieldLength) {
		this.extraFieldLength = extraFieldLength;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public byte[] getExtraField() {
		return extraField;
	}

	public void setExtraField(byte[] extraField) {
		this.extraField = extraField;
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}
}
