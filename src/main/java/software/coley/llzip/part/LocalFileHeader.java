package software.coley.llzip.part;

import software.coley.llzip.ZipCompressions;
import software.coley.llzip.strategy.Decompressor;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;

import static software.coley.llzip.ZipCompressions.*;

/**
 * ZIP LocalFileHeader structure.
 *
 * @author Matt Coley
 */
public class LocalFileHeader implements ZipPart, ZipRead {
	protected static final int MIN_FIXED_SIZE = 30;
	private transient long offset = -1L;
	private transient CentralDirectoryFileHeader linkedDirectoryFileHeader;
	// Zip spec elements
	private int versionNeededToExtract;
	private int generalPurposeBitFlag;
	private int compressionMethod;
	private int lastModFileTime;
	private int lastModFileDate;
	private int crc32;
	private long compressedSize;
	private long uncompressedSize;
	private int fileNameLength;
	private int extraFieldLength;
	private ByteData fileName;
	private ByteData extraField;
	private ByteData fileData;

	private transient String fileNameCache;

	@Override
	public void read(ByteData data, long offset) {
		this.offset = offset;
		versionNeededToExtract = ByteDataUtil.readWord(data, offset + 4);
		generalPurposeBitFlag = ByteDataUtil.readWord(data, offset + 6);
		compressionMethod = ByteDataUtil.readWord(data, offset + 8);
		lastModFileTime = ByteDataUtil.readWord(data, offset + 10);
		lastModFileDate = ByteDataUtil.readWord(data, offset + 12);
		crc32 = ByteDataUtil.readQuad(data, offset + 14);
		setCompressedSize(ByteDataUtil.readQuad(data, offset + 18));
		setUncompressedSize(ByteDataUtil.readQuad(data, offset + 22));
		setFileNameLength(ByteDataUtil.readWord(data, offset + 26));
		setExtraFieldLength(ByteDataUtil.readWord(data, offset + 28));
		fileName = data.sliceOf(offset + 30, fileNameLength);
		extraField = data.sliceOf(offset + 30 + fileNameLength, extraFieldLength);
		long fileDataLength;
		if (compressionMethod == STORED) {
			fileDataLength = uncompressedSize;
		} else {
			fileDataLength = compressedSize;
		}
		fileData = data.sliceOf(offset + 30 + fileNameLength + extraFieldLength, fileDataLength);
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE + fileName.length() + extraField.length() + fileData.length();
	}

	@Override
	public PartType type() {
		return PartType.LOCAL_FILE_HEADER;
	}

	@Override
	public long offset() {
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
	public ByteData decompress(Decompressor decompressor) throws IOException {
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
	public long getCompressedSize() {
		return compressedSize;
	}

	/**
	 * @param compressedSize
	 * 		Compressed size of {@link #getFileData()}.
	 */
	public void setCompressedSize(long compressedSize) {
		this.compressedSize = compressedSize & 0xffffffffL;
	}

	/**
	 * Be aware that these attributes can be falsified.
	 * Different zip-parsing programs treat the files differently
	 * and may not adhere to what you expect from the zip specification.
	 *
	 * @return Uncompressed size after {@link #decompress(Decompressor)} is used on {@link #getFileData()}.
	 */
	public long getUncompressedSize() {
		return uncompressedSize;
	}

	/**
	 * @param uncompressedSize
	 * 		Uncompressed size after {@link #decompress(Decompressor)} is used on {@link #getFileData()}.
	 */
	public void setUncompressedSize(long uncompressedSize) {
		this.uncompressedSize = uncompressedSize & 0xffffffffL;
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
		this.fileNameLength = fileNameLength & 0xffff;
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
		this.extraFieldLength = extraFieldLength & 0xffff;
	}

	/**
	 * Should match {@link CentralDirectoryFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, the central directory file name should be trusted instead.
	 *
	 * @return File name.
	 */
	public ByteData getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 * 		File name.
	 */
	public void setFileName(ByteData fileName) {
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
			return this.fileNameCache = ByteDataUtil.toString(fileName);
		}
		return fileNameCache;
	}

	/**
	 * @return May be used for extra compression information,
	 * depending on the {@link #getCompressionMethod() compression method} used.
	 */
	public ByteData getExtraField() {
		return extraField;
	}

	/**
	 * @param extraField
	 * 		Extra field bytes.
	 */
	public void setExtraField(ByteData extraField) {
		this.extraField = extraField;
	}

	/**
	 * @return Compressed file contents.
	 *
	 * @see #decompress(Decompressor) Decompresses this data.
	 */
	public ByteData getFileData() {
		return fileData;
	}

	/**
	 * @param fileData
	 * 		Compressed file contents.
	 */
	public void setFileData(ByteData fileData) {
		this.fileData = fileData;
	}
}
