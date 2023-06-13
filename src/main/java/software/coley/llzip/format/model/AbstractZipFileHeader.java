package software.coley.llzip.format.model;

import software.coley.llzip.format.compression.Decompressor;
import software.coley.llzip.format.compression.ZipCompressions;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

/**
 * Common base for shared elements of {@link CentralDirectoryFileHeader} and {@link LocalFileHeader}.
 *
 * @author Matt Coley
 */
public abstract class AbstractZipFileHeader implements ZipPart, ZipRead {
	// Zip spec elements, all lazily read, common between central/local file headers
	protected int versionNeededToExtract;
	protected int generalPurposeBitFlag;
	protected int compressionMethod;
	protected int lastModFileTime;
	protected int lastModFileDate;
	protected int crc32;
	protected long compressedSize;
	protected long uncompressedSize;
	protected int fileNameLength;
	protected int extraFieldLength;
	protected ByteData fileName;
	protected ByteData extraField;

	// Offset into the data this part is read from
	protected transient long offset = -1L;

	// String cache values
	private transient String fileNameCache;
	private transient String extraFieldCache;

	@Override
	public long offset() {
		return offset;
	}

	@Override
	public void read(ByteData data, long offset) {
		this.offset = offset;
	}

	/**
	 * @return Version of zip software required to read the archive features.
	 */
	public int getVersionNeededToExtract() {
		return versionNeededToExtract;
	}

	/**
	 * @param versionNeededToExtract
	 * 		Version of zip software required to read the archive features.
	 */
	public void setVersionNeededToExtract(int versionNeededToExtract) {
		this.versionNeededToExtract = versionNeededToExtract;
	}

	/**
	 * @return Used primarily to expand on details of file compression.
	 */
	public int getGeneralPurposeBitFlag() {
		return generalPurposeBitFlag;
	}

	/**
	 * @param generalPurposeBitFlag
	 * 		Used primarily to expand on details of file compression.
	 */
	public void setGeneralPurposeBitFlag(int generalPurposeBitFlag) {
		this.generalPurposeBitFlag = generalPurposeBitFlag;
	}

	/**
	 * @return Method to use for {@link LocalFileHeader#decompress(Decompressor) decompressing data}.
	 *
	 * @see ZipCompressions Possible methods.
	 */
	public int getCompressionMethod() {
		return compressionMethod;
	}

	/**
	 * @param compressionMethod
	 * 		Method to use for {@link LocalFileHeader#decompress(Decompressor) decompressing data}.
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
	 * <p>
	 * When in doubt, trust {@code data.length()} from {@link LocalFileHeader#getFileData()}.
	 *
	 * @return Compressed size of {@link LocalFileHeader#getFileData()}.
	 */
	public long getCompressedSize() {
		return compressedSize;
	}

	/**
	 * @param compressedSize
	 * 		Compressed size of {@link LocalFileHeader#getFileData()}.
	 */
	public void setCompressedSize(long compressedSize) {
		this.compressedSize = compressedSize;
	}

	/**
	 * Be aware that these attributes can be falsified.
	 * Different zip-parsing programs treat the files differently
	 * and may not adhere to what you expect from the zip specification.
	 *
	 * @return Uncompressed size after {@link LocalFileHeader#decompress(Decompressor)} is used on {@link LocalFileHeader#getFileData()}.
	 */
	public long getUncompressedSize() {
		return uncompressedSize;
	}

	/**
	 * @param uncompressedSize
	 * 		Uncompressed size after {@link LocalFileHeader#decompress(Decompressor)} is used on {@link LocalFileHeader#getFileData()}.
	 */
	public void setUncompressedSize(long uncompressedSize) {
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
	 * Should match {@link LocalFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, trust this value instead.
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
		if (this.fileName != fileName)
			fileNameCache = null;
		this.fileName = fileName;
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
	 * @return Extra field.
	 */
	public String getExtraFieldAsString() {
		String fileCommentCache = this.extraFieldCache;
		if (fileCommentCache == null) {
			return this.extraFieldCache = ByteDataUtil.toString(extraField);
		}
		return fileCommentCache;
	}
}
