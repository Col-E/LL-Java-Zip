package software.coley.lljzip.format.model;

import software.coley.lljzip.format.compression.Decompressor;
import software.coley.lljzip.format.read.ZipReader;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;
import software.coley.lljzip.util.lazy.LazyByteData;
import software.coley.lljzip.util.lazy.LazyInt;
import software.coley.lljzip.util.lazy.LazyLong;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;

import static software.coley.lljzip.format.compression.ZipCompressions.STORED;

/**
 * ZIP LocalFileHeader structure.
 * <pre>
 * {@code
 *     SIGNATURE Signature ;
 *     WORD VersionNeededToExtract ;
 *     WORD GeneralPurposeBitFlag ;
 *     COMPRESSION_METHOD CompressionMethod ;
 *     DOSTIME LastModFileTime ;
 *     DOSDATE LastModFileDate ;
 *     DWORD Crc32 ;
 *     DWORD CompressedSize ;
 *     DWORD UncompressedSize ;
 *     WORD  FileNameLength ;
 *     WORD  ExtraFieldLength ;
 *     char  FileName[FileNameLength] ;
 *     blob  ExtraField[ExtraFieldLength] ;
 *     blob  FileData[CompressedSize] ;
 * }
 * </pre>
 *
 * @author Matt Coley
 */
public class LocalFileHeader extends AbstractZipFileHeader {
	protected static final int MIN_FIXED_SIZE = 30;
	private transient CentralDirectoryFileHeader linkedDirectoryFileHeader;

	// LocalFileHeader spec (plus common elements between this and central file)
	protected LazyByteData fileData;

	// Caches
	private transient LazyLong fileDataLength;

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LocalFileHeader copy() {
		LocalFileHeader copy = new LocalFileHeader();
		copy.data = data;
		copy.offset = offset;
		copy.versionNeededToExtract = versionNeededToExtract.copy();
		copy.generalPurposeBitFlag = generalPurposeBitFlag.copy();
		copy.compressionMethod = compressionMethod.copy();
		copy.lastModFileTime = lastModFileTime.copy();
		copy.lastModFileDate = lastModFileDate.copy();
		copy.crc32 = crc32.copy();
		copy.compressedSize = compressedSize.copy();
		copy.uncompressedSize = uncompressedSize.copy();
		copy.fileNameLength = fileNameLength.copy();
		copy.extraFieldLength = extraFieldLength.copy();
		copy.fileName = fileName.copy();
		copy.extraField = extraField.copy();
		copy.fileDataLength = fileDataLength.copy();
		copy.fileData = fileData.copy();
		return copy;
	}

	@Override
	public void read(@Nonnull ByteData data, long offset) {
		super.read(data, offset);
		versionNeededToExtract = ByteDataUtil.readLazyWord(data, offset, 4);
		generalPurposeBitFlag = ByteDataUtil.readLazyWord(data, offset, 6);
		compressionMethod = ByteDataUtil.readLazyWord(data, offset, 8);
		lastModFileTime = ByteDataUtil.readLazyWord(data, offset, 10);
		lastModFileDate = ByteDataUtil.readLazyWord(data, offset, 12);
		crc32 = ByteDataUtil.readLazyQuad(data, offset, 14);
		compressedSize = ByteDataUtil.readLazyMaskedLongQuad(data, offset, 18);
		uncompressedSize = ByteDataUtil.readLazyMaskedLongQuad(data, offset, 22);
		fileNameLength = ByteDataUtil.readLazyWord(data, offset, 26);
		extraFieldLength = ByteDataUtil.readLazyWord(data, offset, 28);
		fileName = ByteDataUtil.readLazySlice(data, offset, new LazyInt(() -> MIN_FIXED_SIZE), fileNameLength);
		extraField = ByteDataUtil.readLazySlice(data, offset, fileNameLength.add(MIN_FIXED_SIZE), extraFieldLength);
		fileDataLength = new LazyLong(() -> {
			long fileDataLength;
			if (compressionMethod.get() == STORED) {
				fileDataLength = uncompressedSize.get();
			} else {
				fileDataLength = compressedSize.get();
			}
			return fileDataLength;
		});
		fileData = ByteDataUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(MIN_FIXED_SIZE), fileDataLength);
	}

	/**
	 * Checks if the contents do not match those described in {@link CentralDirectoryFileHeader}.
	 * If this is the case you will probably want to change your ZIP reading configuration.
	 * <p>
	 * You can override the {@link ZipReader#postProcessLocalFileHeader(LocalFileHeader)}
	 * to call {@link #adoptLinkedCentralDirectoryValues()}. The implementations of {@link ZipReader}
	 * are non-final, so you can extend them to add the override.
	 *
	 * @return {@code true} when the contents of this file header do not match those outlined by the associated
	 * {@link CentralDirectoryFileHeader}.
	 */
	public boolean hasDifferentValuesThanCentralDirectoryHeader() {
		if (linkedDirectoryFileHeader == null) return false;
		if (getVersionNeededToExtract() != linkedDirectoryFileHeader.getVersionNeededToExtract()) return true;
		if (getGeneralPurposeBitFlag() != linkedDirectoryFileHeader.getGeneralPurposeBitFlag()) return true;
		if (getCompressionMethod() != linkedDirectoryFileHeader.getCompressionMethod()) return true;
		if (getLastModFileTime() != linkedDirectoryFileHeader.getLastModFileTime()) return true;
		if (getLastModFileDate() != linkedDirectoryFileHeader.getLastModFileDate()) return true;
		if (getCrc32() != linkedDirectoryFileHeader.getCrc32()) return true;
		if (getCompressedSize() != linkedDirectoryFileHeader.getCompressedSize()) return true;
		if (getUncompressedSize() != linkedDirectoryFileHeader.getUncompressedSize()) return true;
		if (getFileNameLength() != linkedDirectoryFileHeader.getFileNameLength()) return true;
		return !Objects.equals(getFileNameAsString(), linkedDirectoryFileHeader.getFileNameAsString());
	}

	/**
	 * Allows values to be adopted from the linked {@link #getLinkedDirectoryFileHeader() CentralDirectoryFileHeader}.
	 * <p>
	 * In some cases the {@link LocalFileHeader} file size may be 0, but the authoritative CEN states a non-0 value,
	 * which you may want to adopt.
	 */
	public void adoptLinkedCentralDirectoryValues() {
		if (data != null && linkedDirectoryFileHeader != null) {
			setVersionNeededToExtract(linkedDirectoryFileHeader.getVersionNeededToExtract());
			setGeneralPurposeBitFlag(linkedDirectoryFileHeader.getGeneralPurposeBitFlag());
			setCompressionMethod(linkedDirectoryFileHeader.getCompressionMethod());
			setLastModFileTime(linkedDirectoryFileHeader.getLastModFileTime());
			setLastModFileDate(linkedDirectoryFileHeader.getLastModFileDate());
			setCrc32(linkedDirectoryFileHeader.getCrc32());
			setCompressedSize(linkedDirectoryFileHeader.getCompressedSize());
			setUncompressedSize(linkedDirectoryFileHeader.getUncompressedSize());
			setFileNameLength(linkedDirectoryFileHeader.getFileNameLength());
			fileName = ByteDataUtil.readLazySlice(data, offset, new LazyInt(() -> MIN_FIXED_SIZE), fileNameLength);
			extraField = ByteDataUtil.readLazySlice(data, offset, fileNameLength.add(MIN_FIXED_SIZE), extraFieldLength);
			fileDataLength = new LazyLong(() -> {
				long fileDataLength;
				if (compressionMethod.get() == STORED) {
					fileDataLength = uncompressedSize.get();
				} else {
					fileDataLength = compressedSize.get();
				}
				return fileDataLength;
			});
			fileData = ByteDataUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(30), fileDataLength);
		}
	}

	/**
	 * Sets the file data length to go up to the given offset.
	 *
	 * @param endOffset New file data length.
	 */
	public void setFileDataEndOffset(long endOffset) {
		int fileDataStartOffset = fileNameLength.add(extraFieldLength).add(30).get();
		long length = endOffset - fileDataStartOffset;
		setFileDataLength(length);
	}

	/**
	 * @param newLength New file data length.
	 */
	public void setFileDataLength(long newLength) {
		fileDataLength.set(newLength);
		fileData = ByteDataUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(30), newLength);
	}

	/**
	 * @param newLength New file data length.
	 */
	public void setFileDataLength(@Nonnull LazyLong newLength) {
		fileDataLength = newLength;
		fileData = ByteDataUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(30), newLength);
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE +
				fileNameLength.get() +
				extraFieldLength.get() +
				fileDataLength.get();
	}

	@Nonnull
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
		return decompressor.decompress(this, fileData.get());
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
	 * @return Compressed file contents.
	 *
	 * @see #decompress(Decompressor) Decompresses this data.
	 */
	public ByteData getFileData() {
		return fileData.get();
	}

	/**
	 * @param fileData
	 * 		Compressed file contents.
	 */
	public void setFileData(ByteData fileData) {
		this.fileData.set(fileData);
	}

	@Override
	public String toString() {
		return "LocalFileHeader{" +
				"fileData=" + fileData +
				", fileDataLength=" + fileDataLength +
				", data=" + data +
				", versionNeededToExtract=" + versionNeededToExtract +
				", generalPurposeBitFlag=" + generalPurposeBitFlag +
				", compressionMethod=" + compressionMethod +
				", lastModFileTime=" + lastModFileTime +
				", lastModFileDate=" + lastModFileDate +
				", crc32=" + crc32 +
				", compressedSize=" + compressedSize +
				", uncompressedSize=" + uncompressedSize +
				", fileNameLength=" + fileNameLength +
				", extraFieldLength=" + extraFieldLength +
				", fileName='" + getFileNameAsString() + '\'' +
				", extraField='" + getExtraFieldAsString() + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LocalFileHeader that = (LocalFileHeader) o;

		if (!Objects.equals(versionNeededToExtract, that.versionNeededToExtract)) return false;
		if (!Objects.equals(generalPurposeBitFlag, that.generalPurposeBitFlag)) return false;
		if (!Objects.equals(compressionMethod, that.compressionMethod)) return false;
		if (!Objects.equals(lastModFileTime, that.lastModFileTime)) return false;
		if (!Objects.equals(lastModFileDate, that.lastModFileDate)) return false;
		if (!Objects.equals(crc32, that.crc32)) return false;
		if (!Objects.equals(compressedSize, that.compressedSize)) return false;
		if (!Objects.equals(uncompressedSize, that.uncompressedSize)) return false;
		if (!Objects.equals(fileNameLength, that.fileNameLength)) return false;
		if (!Objects.equals(extraFieldLength, that.extraFieldLength)) return false;
		if (!Objects.equals(fileName, that.fileName)) return false;
		if (!Objects.equals(extraField, that.extraField)) return false;
		if (!Objects.equals(fileDataLength, that.fileDataLength)) return false;
		return Objects.equals(fileData, that.fileData);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = 31 * result + (versionNeededToExtract != null ? versionNeededToExtract.hashCode() : 0);
		result = 31 * result + (generalPurposeBitFlag != null ? generalPurposeBitFlag.hashCode() : 0);
		result = 31 * result + (compressionMethod != null ? compressionMethod.hashCode() : 0);
		result = 31 * result + (lastModFileTime != null ? lastModFileTime.hashCode() : 0);
		result = 31 * result + (lastModFileDate != null ? lastModFileDate.hashCode() : 0);
		result = 31 * result + (crc32 != null ? crc32.hashCode() : 0);
		result = 31 * result + (compressedSize != null ? compressedSize.hashCode() : 0);
		result = 31 * result + (uncompressedSize != null ? uncompressedSize.hashCode() : 0);
		result = 31 * result + (fileNameLength != null ? fileNameLength.hashCode() : 0);
		result = 31 * result + (extraFieldLength != null ? extraFieldLength.hashCode() : 0);
		result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
		result = 31 * result + (extraField != null ? extraField.hashCode() : 0);
		result = 31 * result + (fileDataLength != null ? fileDataLength.hashCode() : 0);
		result = 31 * result + (fileData != null ? fileData.hashCode() : 0);
		return result;
	}
}
