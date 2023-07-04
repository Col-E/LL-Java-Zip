package software.coley.llzip.format.model;

import software.coley.llzip.format.ZipPatterns;
import software.coley.llzip.format.compression.Decompressor;
import software.coley.llzip.format.read.ZipReaderStrategy;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.lazy.LazyByteData;
import software.coley.llzip.util.lazy.LazyInt;
import software.coley.llzip.util.lazy.LazyLong;

import java.io.IOException;
import java.util.Objects;

import static software.coley.llzip.format.compression.ZipCompressions.STORED;

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
	protected static final long MIN_FIXED_SIZE = 30;
	private transient CentralDirectoryFileHeader linkedDirectoryFileHeader;

	// LocalFileHeader spec (plus common elements between this and central file)
	protected LazyByteData fileData;

	// Caches
	private transient LazyLong fileDataLength;
	private transient ByteData data;
	private transient LazyLong crcOffset;

	@Override
	public void read(ByteData data, long offset) {
		super.read(data, offset);
		this.data = data;
		versionNeededToExtract = readWord(data, 4);
		generalPurposeBitFlag = readWord(data, 6);
		compressionMethod = readWord(data, 8);
		lastModFileTime = readWord(data, 10);
		lastModFileDate = readWord(data, 12);
		fileNameLength = readWord(data, 26);
		extraFieldLength = readWord(data, 28);
		fileName = readSlice(data, new LazyInt(() -> 30), fileNameLength);
		extraField = readSlice(data, fileNameLength.add(30), extraFieldLength);
		fileDataLength = new LazyLong(() -> {
			long fileDataLength;
			if (compressionMethod.get() == STORED) {
				fileDataLength = uncompressedSize.get();
			} else {
				fileDataLength = compressedSize.get();
			}
			return fileDataLength;
		});
		fileData = readLongSlice(data, fileNameLength.add(extraFieldLength).add(30), fileDataLength);
		crcOffset = new LazyLong(() -> {
			long crcOffset;
			if ((generalPurposeBitFlag.get() & 8) == 8) {
				crcOffset = 30 + fileNameLength.get() + extraFieldLength.get() + fileDataLength.get();
				if (data.getInt(crcOffset) == ZipPatterns.DATA_DESCRIPTOR_QUAD) {
					crcOffset += 4;
				}
			} else {
				crcOffset = 14;
			}

			return crcOffset;
		});

		crc32 = readQuad(data, crcOffset);
		compressedSize = readMaskedLongQuad(data, crcOffset.add(4));
		uncompressedSize = readMaskedLongQuad(data, crcOffset.add(8));
	}

	/**
	 * Checks if the contents do not match those described in {@link CentralDirectoryFileHeader}.
	 * If this is the case you will probably want to change your ZIP reading configuration.
	 * <p>
	 * You can override the {@link ZipReaderStrategy#postProcessLocalFileHeader(LocalFileHeader)}
	 * to call {@link #adoptLinkedCentralDirectoryValues()}. The implementations of {@link ZipReaderStrategy}
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
	 * When called before being {@link #freeze() frozen} values can be adopted from the linked
	 * {@link #getLinkedDirectoryFileHeader() CentralDirectoryFileHeader}.
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
			fileName = readSlice(data, new LazyInt(() -> 30), fileNameLength);
			extraField = readSlice(data, fileNameLength.add(30), extraFieldLength);
			fileDataLength = new LazyLong(() -> {
				long fileDataLength;
				if (compressionMethod.get() == STORED) {
					fileDataLength = uncompressedSize.get();
				} else {
					fileDataLength = compressedSize.get();
				}
				return fileDataLength;
			});
			fileData = readLongSlice(data, fileNameLength.add(extraFieldLength).add(30), fileDataLength);
		}
	}

	/**
	 * Clears the reference to the source {@link ByteData}, preventing further modification.
	 * <p>
	 * Prevents usage of {@link #adoptLinkedCentralDirectoryValues()}.
	 */
	public void freeze() {
		data = null;
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE +
				fileNameLength.get() +
				extraFieldLength.get() +
				fileDataLength.get();
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
