package software.coley.lljzip.format.model;

import software.coley.lljzip.format.compression.Decompressor;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.lazy.LazyMemorySegment;
import software.coley.lljzip.util.lazy.LazyInt;
import software.coley.lljzip.util.lazy.LazyLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.foreign.MemorySegment;

/**
 * Common base for shared elements of {@link CentralDirectoryFileHeader} and {@link LocalFileHeader}.
 *
 * @author Matt Coley
 */
public abstract class AbstractZipFileHeader implements ZipPart, ZipRead {
	// Zip spec elements, all lazily read, common between central/local file headers
	protected LazyInt versionNeededToExtract;
	protected LazyInt generalPurposeBitFlag;
	protected LazyInt compressionMethod;
	protected LazyInt lastModFileTime;
	protected LazyInt lastModFileDate;
	protected LazyInt crc32;
	protected LazyLong compressedSize;
	protected LazyLong uncompressedSize;
	protected LazyInt fileNameLength;
	protected LazyInt extraFieldLength;
	protected LazyMemorySegment fileName;
	protected LazyMemorySegment extraField;

	// Offset into the data this part is read from
	protected transient long offset = -1L;

	// Data source that contents were read from.
	protected transient MemorySegment data;

	// String cache values
	private transient String fileNameCache;
	private transient String extraFieldCache;

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
	public void read(@Nonnull MemorySegment data, long offset) {
		this.data = data;
		this.offset = offset;
	}

	/**
	 * @return Version of zip software required to read the archive features.
	 */
	public int getVersionNeededToExtract() {
		return versionNeededToExtract.get();
	}

	/**
	 * @param versionNeededToExtract
	 * 		Version of zip software required to read the archive features.
	 */
	public void setVersionNeededToExtract(int versionNeededToExtract) {
		this.versionNeededToExtract.set(versionNeededToExtract);
	}

	/**
	 * @return Used primarily to expand on details of file compression.
	 */
	public int getGeneralPurposeBitFlag() {
		return generalPurposeBitFlag.get();
	}

	/**
	 * @param generalPurposeBitFlag
	 * 		Used primarily to expand on details of file compression.
	 */
	public void setGeneralPurposeBitFlag(int generalPurposeBitFlag) {
		this.generalPurposeBitFlag.set(generalPurposeBitFlag);
	}

	/**
	 * @return Method to use for {@link LocalFileHeader#decompress(Decompressor) decompressing data}.
	 *
	 * @see ZipCompressions Possible methods.
	 */
	public int getCompressionMethod() {
		return compressionMethod.get();
	}

	/**
	 * @param compressionMethod
	 * 		Method to use for {@link LocalFileHeader#decompress(Decompressor) decompressing data}.
	 *
	 * @see ZipCompressions Possible methods.
	 */
	public void setCompressionMethod(int compressionMethod) {
		this.compressionMethod.set(compressionMethod);
	}

	/**
	 * @return Modification time of the file.
	 */
	public int getLastModFileTime() {
		return lastModFileTime.get();
	}

	/**
	 * @param lastModFileTime
	 * 		Modification time of the file.
	 */
	public void setLastModFileTime(int lastModFileTime) {
		this.lastModFileTime.set(lastModFileTime);
	}

	/**
	 * @return Modification date of the file.
	 */
	public int getLastModFileDate() {
		return lastModFileDate.get();
	}

	/**
	 * @param lastModFileDate
	 * 		Modification date of the file.
	 */
	public void setLastModFileDate(int lastModFileDate) {
		this.lastModFileDate.set(lastModFileDate);
	}

	/**
	 * @return File checksum.
	 */
	public int getCrc32() {
		return crc32.get();
	}

	/**
	 * @param crc32
	 * 		File checksum.
	 */
	public void setCrc32(int crc32) {
		this.crc32.set(crc32);
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
		return compressedSize.get();
	}

	/**
	 * @param compressedSize
	 * 		Compressed size of {@link LocalFileHeader#getFileData()}.
	 */
	public void setCompressedSize(long compressedSize) {
		this.compressedSize.set(compressedSize & 0xFFFFFFFFL);
	}

	/**
	 * Be aware that these attributes can be falsified.
	 * Different zip-parsing programs treat the files differently
	 * and may not adhere to what you expect from the zip specification.
	 *
	 * @return Uncompressed size after {@link LocalFileHeader#decompress(Decompressor)} is used on {@link LocalFileHeader#getFileData()}.
	 */
	public long getUncompressedSize() {
		return uncompressedSize.get();
	}

	/**
	 * @param uncompressedSize
	 * 		Uncompressed size after {@link LocalFileHeader#decompress(Decompressor)} is used on {@link LocalFileHeader#getFileData()}.
	 */
	public void setUncompressedSize(long uncompressedSize) {
		this.uncompressedSize.set(uncompressedSize & 0xFFFFFFFFL);
	}

	/**
	 * @return Length of {@link #getFileName()}.
	 */
	public int getFileNameLength() {
		return fileNameLength.get();
	}

	/**
	 * @param fileNameLength
	 * 		Length of {@link #getFileName()}.
	 */
	public void setFileNameLength(int fileNameLength) {
		this.fileNameLength.set(fileNameLength & 0xFFFF);
	}

	/**
	 * @return Length of {@link #getExtraField()}
	 */
	public int getExtraFieldLength() {
		return extraFieldLength.get();
	}

	/**
	 * @param extraFieldLength
	 * 		Length of {@link #getExtraField()}
	 */
	public void setExtraFieldLength(int extraFieldLength) {
		this.extraFieldLength.set(extraFieldLength & 0xFFFF);
	}

	/**
	 * Should match {@link LocalFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, trust this value instead.
	 *
	 * @return File name.
	 */
	public MemorySegment getFileName() {
		return fileName.get();
	}

	/**
	 * @param fileName
	 * 		File name.
	 */
	public void setFileName(MemorySegment fileName) {
		this.fileName.set(fileName);
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
		if (fileNameCache == null && fileName != null) {
			return this.fileNameCache = MemorySegmentUtil.toString(fileName.get());
		}
		return fileNameCache;
	}

	/**
	 * @return May be used for extra compression information,
	 * depending on the {@link #getCompressionMethod() compression method} used.
	 */
	public MemorySegment getExtraField() {
		return extraField.get();
	}

	/**
	 * @param extraField
	 * 		Extra field bytes.
	 */
	public void setExtraField(MemorySegment extraField) {
		this.extraField.set(extraField);
	}

	/**
	 * @return Extra field.
	 */
	public String getExtraFieldAsString() {
		String fileCommentCache = this.extraFieldCache;
		if (fileCommentCache == null && extraField != null) {
			return this.extraFieldCache = MemorySegmentUtil.toString(extraField.get());
		}
		return fileCommentCache;
	}
}
