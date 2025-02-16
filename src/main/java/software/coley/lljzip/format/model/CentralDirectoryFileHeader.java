package software.coley.lljzip.format.model;

import software.coley.lljzip.util.data.MemorySegmentData;
import software.coley.lljzip.util.data.StringData;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

import static software.coley.lljzip.util.MemorySegmentUtil.*;

/**
 * ZIP CentralDirectoryFileHeader structure.
 * <pre>
 * {@code
 *     SIGNATURE Signature ;
 *     VERSION_MADE_BY VersionMadeBy ;
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
 *     WORD  FileCommentLength ;
 *     WORD  DiskNumberStart ;
 *     WORD  InternalFileAttributes ;
 *     DWORD ExternalFileAttributes ;
 *     DWORD RelativeOffsetOfLocalHeader ;
 * }
 * </pre>
 *
 * @author Matt Coley
 */
public class CentralDirectoryFileHeader extends AbstractZipFileHeader {
	protected static final long MIN_FIXED_SIZE = 46;

	private transient LocalFileHeader linkedFileHeader;

	// CentralDirectoryFileHeader spec (plus common elements between this and local file)
	private int versionMadeBy;
	private int fileCommentLength;
	private StringData fileComment;
	private int diskNumberStart;
	private int internalFileAttributes;
	private int externalFileAttributes;
	private long relativeOffsetOfLocalHeader;

	/**
	 * @return Copy.
	 */
	@Nonnull
	public CentralDirectoryFileHeader copy() {
		CentralDirectoryFileHeader copy = new CentralDirectoryFileHeader();
		copy.data = data;
		copy.offset = offset;
		copy.linkedFileHeader = linkedFileHeader;
		copy.versionMadeBy = versionMadeBy;
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
		copy.fileCommentLength = fileCommentLength;
		copy.diskNumberStart = diskNumberStart;
		copy.internalFileAttributes = internalFileAttributes;
		copy.externalFileAttributes = externalFileAttributes;
		copy.relativeOffsetOfLocalHeader = relativeOffsetOfLocalHeader;
		copy.fileName = fileName.copy();
		copy.extraField = extraField.copy();
		copy.fileComment = fileComment.copy();
		return copy;
	}

	@Override
	public void read(@Nonnull MemorySegment data, long offset) throws ZipParseException {
		super.read(data, offset);
		try {
			versionMadeBy = readWord(data, offset, 4);
			versionNeededToExtract = readWord(data, offset, 6);
			generalPurposeBitFlag = readWord(data, offset, 8);
			compressionMethod = readWord(data, offset, 10);
			lastModFileTime = readWord(data, offset, 12);
			lastModFileDate = readWord(data, offset, 14);
			crc32 = readQuad(data, offset, 16);
			compressedSize = readMaskedLongQuad(data, offset, 20);
			uncompressedSize = readMaskedLongQuad(data, offset, 24);
			fileNameLength = readWord(data, offset, 28);
			extraFieldLength = readWord(data, offset, 30);
			fileCommentLength = readWord(data, offset, 32);
			diskNumberStart = readWord(data, offset, 34);
			internalFileAttributes = readWord(data, offset, 36);
			externalFileAttributes = readQuad(data, offset, 38);
			relativeOffsetOfLocalHeader = readMaskedLongQuad(data, offset, 42);
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_OTHER);
		} catch (Throwable t) {
			throw new ZipParseException(t, ZipParseException.Type.OTHER);
		}
		try {
			fileName = StringData.of(data, offset + 46, fileNameLength);
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_FILE_NAME);
		} catch (Throwable t) {
			throw new ZipParseException(t, ZipParseException.Type.OTHER);
		}
		try {
			extraField = MemorySegmentData.of(data, offset + 46 + fileNameLength, extraFieldLength);
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_FILE_EXTRA);
		} catch (Throwable t) {
			throw new ZipParseException(t, ZipParseException.Type.OTHER);
		}
		try {
			fileComment = StringData.of(data, offset + 46 + fileNameLength + extraFieldLength, fileCommentLength);
		} catch (IndexOutOfBoundsException ex) {
			throw new ZipParseException(ex, ZipParseException.Type.IOOBE_CEN_COMMENT);
		} catch (Throwable t) {
			throw new ZipParseException(t, ZipParseException.Type.OTHER);
		}
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE +
				fileNameLength +
				extraFieldLength +
				fileCommentLength;
	}

	@Nonnull
	@Override
	public PartType type() {
		return PartType.CENTRAL_DIRECTORY_FILE_HEADER;
	}

	/**
	 * Should match {@link LocalFileHeader#getFileNameLength()} but is not a strict requirement.
	 * If they do not match, trust this value instead.
	 *
	 * @return File name length.
	 */
	@Override
	public int getFileNameLength() {
		return super.getFileNameLength();
	}

	/**
	 * Should match {@link LocalFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, trust this value instead.
	 *
	 * @return File name.
	 */
	@Override
	public StringData getFileName() {
		return super.getFileName();
	}

	/**
	 * Should match {@link LocalFileHeader#getFileName()} but is not a strict requirement.
	 * If they do not match, trust this value instead.
	 *
	 * @return File name.
	 */
	@Override
	public String getFileNameAsString() {
		return super.getFileNameAsString();
	}

	/**
	 * @return The file header associated with {@link #getRelativeOffsetOfLocalHeader()}. May be {@code null}.
	 */
	public LocalFileHeader getLinkedFileHeader() {
		return linkedFileHeader;
	}

	/**
	 * @param header
	 * 		The file header associated with {@link #getRelativeOffsetOfLocalHeader()}. May be {@code null}.
	 */
	public void link(LocalFileHeader header) {
		this.linkedFileHeader = header;
	}

	/**
	 * @return Version of zip software used to make the archive.
	 */
	public int getVersionMadeBy() {
		return versionMadeBy;
	}

	/**
	 * @param versionMadeBy
	 * 		Version of zip software used to make the archive.
	 */
	public void setVersionMadeBy(int versionMadeBy) {
		this.versionMadeBy = versionMadeBy;
	}

	/**
	 * @return Disk number where the archive starts from, or {@code 0xFFFF} for ZIP64.
	 */
	public int getDiskNumberStart() {
		return diskNumberStart;
	}

	/**
	 * @param diskNumberStart
	 * 		Disk number where the archive starts from, or {@code 0xFFFF} for ZIP64.
	 */
	public void setDiskNumberStart(int diskNumberStart) {
		this.diskNumberStart = diskNumberStart;
	}

	/**
	 * The lowest bit of this field indicates, if set,
	 * that the file is apparently an ASCII or text file.
	 * If not set, that the file apparently contains binary data.
	 * <br>
	 * The {@code 0x0002} bit of this field indicates, if set,
	 * that a 4 byte variable record length control field precedes each
	 * logical record indicating the length of the record.
	 *
	 * @return Internal attributes used for inferring content type.
	 */
	public int getInternalFileAttributes() {
		return internalFileAttributes;
	}

	/**
	 * @param internalFileAttributes
	 * 		Internal attributes used for inferring content type.
	 */
	public void setInternalFileAttributes(int internalFileAttributes) {
		this.internalFileAttributes = internalFileAttributes;
	}

	/**
	 * For MS-DOS, the low order byte is the MS-DOS directory attribute byte.
	 * If input came from standard input, this field is zero.
	 *
	 * @return Host system dependent attributes.
	 */
	public int getExternalFileAttributes() {
		return externalFileAttributes;
	}

	/**
	 * @param externalFileAttributes
	 * 		Host system dependent attributes.
	 */
	public void setExternalFileAttributes(int externalFileAttributes) {
		this.externalFileAttributes = externalFileAttributes;
	}

	/**
	 * @return Offset from the start of the {@link #getDiskNumberStart() first disk} where the file appears.
	 * This should also be where the {@link LocalFileHeader} is located.  Or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public long getRelativeOffsetOfLocalHeader() {
		return relativeOffsetOfLocalHeader;
	}

	/**
	 * @param relativeOffsetOfLocalHeader
	 * 		Offset from the start of the {@link #getDiskNumberStart() first disk} where the file appears.
	 * 		This should also be where the {@link LocalFileHeader} is located.  Or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public void setRelativeOffsetOfLocalHeader(long relativeOffsetOfLocalHeader) {
		this.relativeOffsetOfLocalHeader = relativeOffsetOfLocalHeader & 0xFFFFFFFFL;
	}

	/**
	 * @return Length of {@link #getFileComment()}.
	 */
	public int getFileCommentLength() {
		return fileCommentLength;
	}


	/**
	 * @param fileCommentLength
	 * 		Length of {@link #getFileComment()}.
	 */
	public void setFileCommentLength(int fileCommentLength) {
		this.fileCommentLength = fileCommentLength & 0xFFFF;
	}

	/**
	 * @return File comment.
	 */
	public StringData getFileComment() {
		return fileComment;
	}

	/**
	 * @param fileComment
	 * 		File comment.
	 */
	public void setFileComment(StringData fileComment) {
		this.fileComment = fileComment;
	}

	/**
	 * @return File comment.
	 */
	public String getFileCommentAsString() {
		return fileComment.get();
	}

	@Override
	public String toString() {
		return "CentralDirectoryFileHeader{" +
				"  versionMadeBy=" + versionMadeBy +
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
				", fileCommentLength=" + fileCommentLength +
				", diskNumberStart=" + diskNumberStart +
				", internalFileAttributes=" + internalFileAttributes +
				", externalFileAttributes=" + externalFileAttributes +
				", relativeOffsetOfLocalHeader=" + relativeOffsetOfLocalHeader +
				", fileName='" + getFileNameAsString() + '\'' +
				", extraField='" + getExtraFieldAsString() + '\'' +
				", fileComment='" + getFileCommentAsString() + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof CentralDirectoryFileHeader that)) return false;
		if (!super.equals(o)) return false;

		if (versionMadeBy != that.versionMadeBy) return false;
		if (fileCommentLength != that.fileCommentLength) return false;
		if (diskNumberStart != that.diskNumberStart) return false;
		if (internalFileAttributes != that.internalFileAttributes) return false;
		if (externalFileAttributes != that.externalFileAttributes) return false;
		if (relativeOffsetOfLocalHeader != that.relativeOffsetOfLocalHeader) return false;
		if (!Objects.equals(linkedFileHeader, that.linkedFileHeader)) return false;
		return Objects.equals(fileComment, that.fileComment);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (linkedFileHeader != null ? linkedFileHeader.hashCode() : 0);
		result = 31 * result + versionMadeBy;
		result = 31 * result + fileCommentLength;
		result = 31 * result + (fileComment != null ? fileComment.hashCode() : 0);
		result = 31 * result + diskNumberStart;
		result = 31 * result + internalFileAttributes;
		result = 31 * result + externalFileAttributes;
		result = 31 * result + (int) (relativeOffsetOfLocalHeader ^ (relativeOffsetOfLocalHeader >>> 32));
		return result;
	}
}
