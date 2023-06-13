package software.coley.llzip.format.model;

import software.coley.llzip.format.compression.Decompressor;
import software.coley.llzip.format.read.ZipReaderStrategy;
import software.coley.llzip.util.ByteData;

import java.io.IOException;
import java.util.Objects;

import static software.coley.llzip.format.compression.ZipCompressions.STORED;
import static software.coley.llzip.util.ByteDataUtil.*;

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
	protected ByteData fileData;

	// Caches
	private transient long fileDataLength;
	private transient ByteData data;

	@Override
	public void read(ByteData data, long offset) {
		super.read(data, offset);
		data = data.sliceOf(offset, data.length() - offset);
		this.data = data;
		versionNeededToExtract = readWord(data, 4);
		generalPurposeBitFlag = readWord(data, 6);
		compressionMethod = readWord(data, 8);
		lastModFileTime = readWord(data, 10);
		lastModFileDate = readWord(data, 12);
		crc32 = readQuad(data, 14);
		compressedSize = readUnsignedQuad(data, 18);
		uncompressedSize = readUnsignedQuad(data, 22);
		fileNameLength = readWord(data, 26);
		extraFieldLength = readWord(data, 28);
		fileName = data.sliceOf(30, fileNameLength);
		extraField = data.sliceOf(30 + fileNameLength, extraFieldLength);
		fileDataLength = compressedSize == STORED ? uncompressedSize : compressedSize;
		fileData = data.sliceOf(30 + fileNameLength + extraFieldLength, fileDataLength);
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
			fileName = data.sliceOf(30, fileNameLength);
			extraField = data.sliceOf(30 + fileNameLength, extraFieldLength);
			fileDataLength = compressedSize == STORED ? uncompressedSize : compressedSize;
			fileData = data.sliceOf(30 + fileNameLength + extraFieldLength, fileDataLength);
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
				fileNameLength +
				extraFieldLength +
				fileDataLength;
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

		return offset == that.offset;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(offset);
	}
}
