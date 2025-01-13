package software.coley.lljzip.format.model;

import software.coley.lljzip.format.compression.Decompressor;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.data.MemorySegmentData;
import software.coley.lljzip.util.data.StringData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * Common base for shared elements of {@link CentralDirectoryFileHeader} and {@link LocalFileHeader}.
 *
 * @author Matt Coley
 */
public abstract class AbstractZipFileHeader implements ZipPart, ZipRead {
	// Zip spec elements, common between central/local file headers
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
	protected StringData fileName;
	protected MemorySegmentData extraField;

	// Offset into the data this part is read from
	protected transient long offset = -1L;

	// Data source that contents were read from.
	protected transient MemorySegment data;


	/**
	 * @return The associated backing data that this file header was read from.
	 */
	@Nullable
	public MemorySegment getBackingData() {
		return data;
	}

	@Override
	public long offset() {
		return offset;
	}

	@Override
	public void read(@Nonnull MemorySegment data, long offset) throws ZipParseException {
		this.data = data;
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
		this.compressedSize = compressedSize & 0xFFFFFFFFL;
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
	 * @return File name.
	 */
	public StringData getFileName() {
		return fileName;
	}

	/**
	 * @param fileName
	 * 		File name.
	 */
	public void setFileName(StringData fileName) {
		this.fileName = fileName;
	}

	/**
	 * @return File name.
	 */
	public String getFileNameAsString() {
		return fileName.get();
	}

	/**
	 * @return May be used for extra compression information,
	 * depending on the {@link #getCompressionMethod() compression method} used.
	 */
	public MemorySegmentData getExtraField() {
		return extraField;
	}

	/**
	 * @param extraField
	 * 		Extra field bytes.
	 */
	public void setExtraField(MemorySegmentData extraField) {
		this.extraField = extraField;
	}

	/**
	 * @return Extra field.
	 */
	public String getExtraFieldAsString() {
		return MemorySegmentUtil.toString(extraField.get());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof AbstractZipFileHeader that)) return false;

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
		if (offset != that.offset) return false;
		if (!Objects.equals(fileName, that.fileName)) return false;
		if (!Objects.equals(extraField, that.extraField)) return false;
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
		result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
		result = 31 * result + (extraField != null ? extraField.hashCode() : 0);
		result = 31 * result + (int) (offset ^ (offset >>> 32));
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}
}
