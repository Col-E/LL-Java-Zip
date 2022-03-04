package software.coley.llzip.part;

import software.coley.llzip.util.Array;

/**
 * ZIP CentralDirectoryFileHeader structure.
 *
 * @author Matt Coley
 */
public class CentralDirectoryFileHeader implements ZipPart, ZipRead {
	private transient int offset = -1;
	// Zip spec elements
	private LocalFileHeader linked;
	private int versionMadeBy;
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
	private int fileCommentLength;
	private int diskNumberStart;
	private int internalFileAttributes;
	private int externalFileAttributes;
	private int relativeOffsetOfLocalHeader;
	private String fileName;
	private byte[] extraField;
	private String fileComment;

	@Override
	public void read(byte[] data, int offset) {
		this.offset = offset;
		versionMadeBy = Array.readWord(data, offset + 4);
		versionNeededToExtract = Array.readWord(data, offset + 6);
		generalPurposeBitFlag = Array.readWord(data, offset + 8);
		compressionMethod = Array.readWord(data, offset + 10);
		lastModFileTime = Array.readWord(data, offset + 12);
		lastModFileDate = Array.readWord(data, offset + 14);
		crc32 = Array.readQuad(data, offset + 16);
		compressedSize = Array.readQuad(data, offset + 20);
		uncompressedSize = Array.readQuad(data, offset + 24);
		fileNameLength = Array.readWord(data, offset + 28);
		extraFieldLength = Array.readWord(data, offset + 30);
		fileCommentLength = Array.readWord(data, offset + 32);
		diskNumberStart = Array.readWord(data, offset + 34);
		internalFileAttributes = Array.readWord(data, offset + 36);
		externalFileAttributes = Array.readQuad(data, offset + 38);
		relativeOffsetOfLocalHeader = Array.readQuad(data, offset + 42);
		fileName = Array.readString(data, offset + 46, fileNameLength);
		extraField = Array.readArray(data, offset + 46 + fileNameLength, extraFieldLength);
		fileComment = Array.readString(data, offset + 46 + fileNameLength + extraFieldLength, fileCommentLength);
	}

	@Override
	public int length() {
		return 46 + fileName.length() + extraField.length + fileComment.length();
	}

	@Override
	public PartType type() {
		return PartType.CENTRAL_DIRECTORY_FILE_HEADER;
	}

	@Override
	public int offset() {
		return offset;
	}

	public LocalFileHeader getLinked() {
		return linked;
	}

	public void link(LocalFileHeader linked) {
		this.linked = linked;
	}

	public int getVersionMadeBy() {
		return versionMadeBy;
	}

	public void setVersionMadeBy(int versionMadeBy) {
		this.versionMadeBy = versionMadeBy;
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

	public int getFileCommentLength() {
		return fileCommentLength;
	}

	public void setFileCommentLength(int fileCommentLength) {
		this.fileCommentLength = fileCommentLength;
	}

	public int getDiskNumberStart() {
		return diskNumberStart;
	}

	public void setDiskNumberStart(int diskNumberStart) {
		this.diskNumberStart = diskNumberStart;
	}

	public int getInternalFileAttributes() {
		return internalFileAttributes;
	}

	public void setInternalFileAttributes(int internalFileAttributes) {
		this.internalFileAttributes = internalFileAttributes;
	}

	public int getExternalFileAttributes() {
		return externalFileAttributes;
	}

	public void setExternalFileAttributes(int externalFileAttributes) {
		this.externalFileAttributes = externalFileAttributes;
	}

	public int getRelativeOffsetOfLocalHeader() {
		return relativeOffsetOfLocalHeader;
	}

	public void setRelativeOffsetOfLocalHeader(int relativeOffsetOfLocalHeader) {
		this.relativeOffsetOfLocalHeader = relativeOffsetOfLocalHeader;
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

	public String getFileComment() {
		return fileComment;
	}

	public void setFileComment(String fileComment) {
		this.fileComment = fileComment;
	}
}
