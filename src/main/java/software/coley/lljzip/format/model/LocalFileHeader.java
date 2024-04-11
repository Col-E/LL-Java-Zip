package software.coley.lljzip.format.model;

import software.coley.lljzip.format.compression.Decompressor;
import software.coley.lljzip.format.read.ZipReader;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.lazy.LazyByteData;
import software.coley.lljzip.util.lazy.LazyInt;
import software.coley.lljzip.util.lazy.LazyLong;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
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
	protected transient CentralDirectoryFileHeader linkedDirectoryFileHeader;

	// LocalFileHeader spec (plus common elements between this and central file)
	protected LazyByteData fileData;

	// Caches
	protected transient LazyLong fileDataLength;

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LocalFileHeader copy() {
		LocalFileHeader copy = new LocalFileHeader();
		copy.data = data;
		copy.offset = offset;
		copy.linkedDirectoryFileHeader = linkedDirectoryFileHeader;
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
	public void read(@Nonnull MemorySegment data, long offset) {
		super.read(data, offset);
		versionNeededToExtract = MemorySegmentUtil.readLazyWord(data, offset, 4).withId("versionNeededToExtract");
		generalPurposeBitFlag = MemorySegmentUtil.readLazyWord(data, offset, 6).withId("generalPurposeBitFlag");
		compressionMethod = MemorySegmentUtil.readLazyWord(data, offset, 8).withId("compressionMethod");
		lastModFileTime = MemorySegmentUtil.readLazyWord(data, offset, 10).withId("lastModFileTime");
		lastModFileDate = MemorySegmentUtil.readLazyWord(data, offset, 12).withId("lastModFileDate");
		crc32 = MemorySegmentUtil.readLazyQuad(data, offset, 14).withId("crc32");
		compressedSize = MemorySegmentUtil.readLazyMaskedLongQuad(data, offset, 18).withId("compressedSize");
		uncompressedSize = MemorySegmentUtil.readLazyMaskedLongQuad(data, offset, 22).withId("uncompressedSize");
		fileNameLength = MemorySegmentUtil.readLazyWord(data, offset, 26).withId("fileNameLength");
		extraFieldLength = MemorySegmentUtil.readLazyWord(data, offset, 28).withId("extraFieldLength");
		fileName = MemorySegmentUtil.readLazySlice(data, offset, new LazyInt(() -> MIN_FIXED_SIZE), fileNameLength).withId("fileName");
		extraField = MemorySegmentUtil.readLazySlice(data, offset, fileNameLength.add(MIN_FIXED_SIZE), extraFieldLength).withId("extraField");
		fileDataLength = new LazyLong(() -> {
			long fileDataLength;
			if (compressionMethod.get() == STORED) {
				fileDataLength = uncompressedSize.get();
			} else {
				fileDataLength = compressedSize.get();
			}
			return fileDataLength;
		}).withId("fileDataLength");
		fileData = MemorySegmentUtil.readLazyLongSlice(data, offset,
				fileNameLength.add(extraFieldLength).add(MIN_FIXED_SIZE), fileDataLength).withId("fileData");
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
		if (linkedDirectoryFileHeader != null) {
			versionNeededToExtract = linkedDirectoryFileHeader.versionNeededToExtract;
			generalPurposeBitFlag = linkedDirectoryFileHeader.generalPurposeBitFlag;
			compressionMethod = linkedDirectoryFileHeader.compressionMethod;
			lastModFileTime = linkedDirectoryFileHeader.lastModFileTime;
			lastModFileDate = linkedDirectoryFileHeader.lastModFileDate;
			crc32 = linkedDirectoryFileHeader.crc32;
			compressedSize = linkedDirectoryFileHeader.compressedSize;
			uncompressedSize = linkedDirectoryFileHeader.uncompressedSize;
			fileNameLength = linkedDirectoryFileHeader.fileNameLength;
			fileName = linkedDirectoryFileHeader.fileName;
			extraField = linkedDirectoryFileHeader.extraField;
			// We're using the same slices/data locations from the central directory.
			// If we wanted to use local data but with updated offsets from the central directory it would look like this:
			//  fileName = ByteDataUtil.readLazySlice(data, offset, new LazyInt(() -> MIN_FIXED_SIZE), fileNameLength).withId("fileName");
			//  extraField = ByteDataUtil.readLazySlice(data, offset, fileNameLength.add(MIN_FIXED_SIZE), extraFieldLength).withId("extraField");
			fileDataLength = new LazyLong(() -> {
				long fileDataLength;
				if (compressionMethod.get() == STORED) {
					fileDataLength = uncompressedSize.get();
				} else {
					fileDataLength = compressedSize.get();
				}
				return fileDataLength;
			}).withId("fileDataLength");
			if (data != null)
				fileData = MemorySegmentUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(MIN_FIXED_SIZE), fileDataLength).withId("fileData");
		}
	}

	/**
	 * Sets the file data length to go up to the given offset.
	 *
	 * @param endOffset New file data length.
	 */
	public void setFileDataEndOffset(long endOffset) {
		long fileDataStartOffset = offset + fileNameLength.add(extraFieldLength).add(MIN_FIXED_SIZE).get();
		long length = endOffset - fileDataStartOffset;
		setFileDataLength(length);
	}

	/**
	 * @param newLength New file data length.
	 */
	public void setFileDataLength(long newLength) {
		fileDataLength.set(newLength);
		fileData = MemorySegmentUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(MIN_FIXED_SIZE), newLength).withId("fileData");
	}

	/**
	 * @param newLength New file data length.
	 */
	public void setFileDataLength(@Nonnull LazyLong newLength) {
		fileDataLength = newLength;
		fileData = MemorySegmentUtil.readLazyLongSlice(data, offset, fileNameLength.add(extraFieldLength).add(MIN_FIXED_SIZE), newLength).withId("fileData");
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
	public MemorySegment decompress(Decompressor decompressor) throws IOException {
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
	public MemorySegment getFileData() {
		return fileData.get();
	}

	/**
	 * @param fileData
	 * 		Compressed file contents.
	 */
	public void setFileData(MemorySegment fileData) {
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
