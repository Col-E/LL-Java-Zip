package software.coley.lljzip.format.model;

import software.coley.lljzip.format.compression.Decompressor;
import software.coley.lljzip.format.read.ZipReader;
import software.coley.lljzip.util.data.MemorySegmentData;
import software.coley.lljzip.util.data.StringData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

import static software.coley.lljzip.format.compression.ZipCompressions.STORED;
import static software.coley.lljzip.util.MemorySegmentUtil.*;

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
	public static final int MIN_FIXED_SIZE = 30;
	protected transient CentralDirectoryFileHeader linkedDirectoryFileHeader;

	// LocalFileHeader spec (plus common elements between this and central file)
	protected MemorySegmentData fileData;

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LocalFileHeader copy() {
		LocalFileHeader copy = new LocalFileHeader();
		copy.data = data;
		copy.offset = offset;
		copy.linkedDirectoryFileHeader = linkedDirectoryFileHeader;
		copy.versionNeededToExtract = versionNeededToExtract;
		copy.generalPurposeBitFlag = generalPurposeBitFlag;
		copy.compressionMethod = compressionMethod;
		copy.lastModFileTime = lastModFileTime;
		copy.lastModFileDate = lastModFileDate;
		copy.crc32 = crc32;
		copy.compressedSize = compressedSize;
		copy.uncompressedSize = uncompressedSize;
		copy.fileNameLength = fileNameLength;
		copy.extraFieldLength = extraFieldLength;
		copy.fileName = fileName.copy();
		copy.extraField = extraField.copy();
		copy.fileData = fileData.copy();
		return copy;
	}

	@Override
	public void read(@Nonnull MemorySegment data, long offset) throws ZipParseException {
		super.read(data, offset);
		try {
			versionNeededToExtract = readWord(data, offset, 4);
			generalPurposeBitFlag = readWord(data, offset, 6);
			compressionMethod = readWord(data, offset, 8);
			lastModFileTime = readWord(data, offset, 10);
			lastModFileDate = readWord(data, offset, 12);
			crc32 = readQuad(data, offset, 14);
			compressedSize = readMaskedLongQuad(data, offset, 18);
			uncompressedSize = readMaskedLongQuad(data, offset, 22);
			fileNameLength = readWord(data, offset, 26);
			extraFieldLength = readWord(data, offset, 28);
		} catch (Throwable t) {
			throw new ZipParseException(ZipParseException.Type.OTHER);
		}
		try {
			fileName = StringData.of(readSlice(data, offset, MIN_FIXED_SIZE, fileNameLength));
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_FILE_NAME);
		} catch (Throwable t) {
			throw new ZipParseException(ZipParseException.Type.OTHER);
		}
		try {
			extraField = MemorySegmentData.of(data, offset + MIN_FIXED_SIZE + fileNameLength, extraFieldLength);
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_FILE_EXTRA);
		} catch (Throwable t) {
			throw new ZipParseException(ZipParseException.Type.OTHER);
		}
		long fileDataLength = (compressionMethod == STORED) ? uncompressedSize : compressedSize;
		try {
			fileData = MemorySegmentData.of(readLongSlice(data, offset, MIN_FIXED_SIZE + fileNameLength + extraFieldLength, fileDataLength));
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_FILE_DATA);
		} catch (Throwable t) {
			throw new ZipParseException(ZipParseException.Type.OTHER);
		}
	}

	/**
	 * Should match {@link CentralDirectoryFileHeader#getFileNameLength()} but is not a strict requirement.
	 * If they do not match, the central directory file name length should be trusted instead.
	 *
	 * @return File name length.
	 */
	@Override
	public int getFileNameLength() {
		return super.getFileNameLength();
	}

	/**
	 * Should match {@link CentralDirectoryFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, the central directory file name should be trusted instead.
	 *
	 * @return File name.
	 */
	@Override
	public StringData getFileName() {
		return super.getFileName();
	}

	/**
	 * Should match {@link CentralDirectoryFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, the central directory file name should be trusted instead.
	 *
	 * @return File name.
	 */
	@Override
	public String getFileNameAsString() {
		return super.getFileNameAsString();
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
	public void adoptLinkedCentralDirectoryValues() throws ZipParseException {
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
			//  fileName = ByteDataUtil.readSlice(data, offset, MIN_FIXED_SIZE, fileNameLength)
			//  extraField = ByteDataUtil.readSlice(data, offset, MIN_FIXED_SIZE + fileNameLength, extraFieldLength)
			long fileDataLength = (compressionMethod == STORED) ? uncompressedSize : compressedSize;
			if (data != null)
				try {
					fileData = MemorySegmentData.of(readLongSlice(data, offset, MIN_FIXED_SIZE + fileNameLength + extraFieldLength, fileDataLength));
				} catch (IndexOutOfBoundsException ex) {
					throw new ZipParseException(ex, ZipParseException.Type.IOOBE_FILE_DATA);
				}
		}
	}

	/**
	 * Sets the file data length to go up to the given offset.
	 *
	 * @param endOffset
	 * 		New file data length.
	 */
	public void setFileDataEndOffset(long endOffset) {
		long fileDataStartOffset = offset + MIN_FIXED_SIZE + fileNameLength + extraFieldLength;
		long length = endOffset - fileDataStartOffset;
		setFileDataLength(length);
	}

	/**
	 * @param newLength
	 * 		New file data length.
	 */
	public void setFileDataLength(long newLength) {
		fileData = MemorySegmentData.of(readLongSlice(data, offset, MIN_FIXED_SIZE + fileNameLength + extraFieldLength, newLength));
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE + fileNameLength + extraFieldLength + fileData.length();
	}

	@Nonnull
	@Override
	public PartType type() {
		return PartType.LOCAL_FILE_HEADER;
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
	public void setFileData(MemorySegmentData fileData) {
		this.fileData = fileData;
	}

	@Override
	public String toString() {
		return "LocalFileHeader{" +
				"fileData=" + fileData +
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
		if (!(o instanceof LocalFileHeader that)) return false;
		if (!super.equals(o)) return false;

		if (!Objects.equals(linkedDirectoryFileHeader, that.linkedDirectoryFileHeader)) return false;
		return Objects.equals(fileData, that.fileData);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (linkedDirectoryFileHeader != null ? linkedDirectoryFileHeader.hashCode() : 0);
		result = 31 * result + (fileData != null ? fileData.hashCode() : 0);
		return result;
	}
}
