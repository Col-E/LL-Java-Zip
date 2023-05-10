package software.coley.llzip.format.model;

import software.coley.llzip.format.compression.ZipCompressions;
import software.coley.llzip.format.compression.Decompressor;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.util.Objects;

import static software.coley.llzip.format.compression.ZipCompressions.STORED;

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

	private ByteData data;

	@Override
	public void read(ByteData data, long offset) {
		this.data = data;
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

	/**
	 * When called before being {@link #freeze() frozen} values can be adopted from the linked
	 * {@link #getLinkedDirectoryFileHeader() CentralDirectoryFileHeader}.
	 * <br>
	 * In some cases the {@link LocalFileHeader} file size may be 0, but the authoritative CEN states a non-0 value,
	 * which you may want to adopt.
	 */
	public void adoptLinkedCentralDirectoryValues() {
		if (data != null && linkedDirectoryFileHeader != null) {
			versionNeededToExtract = linkedDirectoryFileHeader.getVersionNeededToExtract();
			generalPurposeBitFlag = linkedDirectoryFileHeader.getGeneralPurposeBitFlag();
			setCompressionMethod(linkedDirectoryFileHeader.getCompressionMethod());
			lastModFileTime = linkedDirectoryFileHeader.getLastModFileTime();
			lastModFileDate = linkedDirectoryFileHeader.getLastModFileDate();
			setCrc32(linkedDirectoryFileHeader.getCrc32());
			setCompressedSize(linkedDirectoryFileHeader.getCompressedSize());
			setUncompressedSize(linkedDirectoryFileHeader.getUncompressedSize());
			setFileNameLength(linkedDirectoryFileHeader.getFileNameLength());
			setFileName(data.sliceOf(offset + 30, fileNameLength));
			extraField = data.sliceOf(offset + 30 + fileNameLength, extraFieldLength);
			long fileDataLength;
			if (compressionMethod == STORED) {
				fileDataLength = uncompressedSize;
			} else {
				fileDataLength = compressedSize;
			}
			fileData = data.sliceOf(offset + 30 + fileNameLength + extraFieldLength, fileDataLength);
		}
	}

	/**
	 * Clears the reference to the source {@link ByteData}, preventing further modification.
	 * <br>
	 * Prevents usage of {@link #adoptLinkedCentralDirectoryValues()}.
	 */
	public void freeze() {
		data = null;
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
	 * When in doubt, trust {@code data.length()} from {@link #getFileData()}.
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
		this.compressedSize = compressedSize & 0xFFFFFFFFL;
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
		this.uncompressedSize = uncompressedSize & 0xFFFFFFFFL;
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
		this.fileNameLength = fileNameLength & 0xFFFF;
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
		this.extraFieldLength = extraFieldLength & 0xFFFF;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LocalFileHeader that = (LocalFileHeader) o;

		if (versionNeededToExtract != that.versionNeededToExtract) return false;
		if (generalPurposeBitFlag != that.generalPurposeBitFlag) return false;
		if (compressionMethod != that.compressionMethod) return false;
		if (lastModFileTime != that.lastModFileTime) return false;
		if (lastModFileDate != that.lastModFileDate) return false;
		if (crc32 != that.crc32) return false;
		if (compressedSize != that.compressedSize) return false;
		if (uncompressedSize != that.uncompressedSize) return false;
		if (fileNameLength != that.fileNameLength) return false;
		if (extraFieldLength != that.extraFieldLength) return false;
		if (!Objects.equals(fileName, that.fileName)) return false;
		if (!Objects.equals(extraField, that.extraField)) return false;
		if (!Objects.equals(fileData, that.fileData)) return false;
		return Objects.equals(data, that.data);
	}

	@Override
	public int hashCode() {
		int result = versionNeededToExtract;
		result = 31 * result + generalPurposeBitFlag;
		result = 31 * result + compressionMethod;
		result = 31 * result + lastModFileTime;
		result = 31 * result + lastModFileDate;
		result = 31 * result + crc32;
		result = 31 * result + (int) (compressedSize ^ (compressedSize >>> 32));
		result = 31 * result + (int) (uncompressedSize ^ (uncompressedSize >>> 32));
		result = 31 * result + fileNameLength;
		result = 31 * result + extraFieldLength;
		result = 31 * result + fileName.hashCode();
		result = 31 * result + extraField.hashCode();
		result = 31 * result + fileData.hashCode();
		return result;
	}
}
