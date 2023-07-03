package software.coley.llzip.format.model;

import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;
import software.coley.llzip.util.lazy.LazyByteData;
import software.coley.llzip.util.lazy.LazyInt;
import software.coley.llzip.util.lazy.LazyLong;

import javax.annotation.Nonnull;
import java.util.Objects;

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
	private LazyInt versionMadeBy;
	private LazyInt fileCommentLength;
	private LazyByteData fileComment;
	private LazyInt diskNumberStart;
	private LazyInt internalFileAttributes;
	private LazyInt externalFileAttributes;
	private LazyLong relativeOffsetOfLocalHeader;

	// String cache values
	private transient String fileCommentCache;

	/**
	 * @return Copy.
	 */
	@Nonnull
	public CentralDirectoryFileHeader copy() {
		CentralDirectoryFileHeader copy = new CentralDirectoryFileHeader();
		copy.data = data;
		copy.offset = offset;
		copy.versionMadeBy = versionMadeBy.copy();
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
		copy.fileCommentLength = fileCommentLength.copy();
		copy.diskNumberStart = diskNumberStart.copy();
		copy.internalFileAttributes = internalFileAttributes.copy();
		copy.externalFileAttributes = externalFileAttributes.copy();
		copy.relativeOffsetOfLocalHeader = relativeOffsetOfLocalHeader.copy();
		copy.fileName = fileName.copy();
		copy.extraField = extraField.copy();
		copy.fileComment = fileComment.copy();
		return copy;
	}

	@Override
	public void read(@Nonnull ByteData data, long offset) {
		super.read(data, offset);
		versionMadeBy = ByteDataUtil.readLazyWord(data, offset, 4);
		versionNeededToExtract = ByteDataUtil.readLazyWord(data, offset, 6);
		generalPurposeBitFlag = ByteDataUtil.readLazyWord(data, offset, 8);
		compressionMethod = ByteDataUtil.readLazyWord(data, offset, 10);
		lastModFileTime = ByteDataUtil.readLazyWord(data, offset, 12);
		lastModFileDate = ByteDataUtil.readLazyWord(data, offset, 14);
		crc32 = ByteDataUtil.readLazyQuad(data, offset, 16);
		compressedSize = ByteDataUtil.readLazyMaskedLongQuad(data, offset, 20);
		uncompressedSize = ByteDataUtil.readLazyMaskedLongQuad(data, offset, 24);
		fileNameLength = ByteDataUtil.readLazyWord(data, offset, 28);
		extraFieldLength = ByteDataUtil.readLazyWord(data, offset, 30);
		fileCommentLength = ByteDataUtil.readLazyWord(data, offset, 32);
		diskNumberStart = ByteDataUtil.readLazyWord(data, offset, 34);
		internalFileAttributes = ByteDataUtil.readLazyWord(data, offset, 36);
		externalFileAttributes = ByteDataUtil.readLazyQuad(data, offset, 38);
		relativeOffsetOfLocalHeader = ByteDataUtil.readLazyMaskedLongQuad(data, offset, 42);
		fileName = ByteDataUtil.readLazySlice(data, offset, new LazyInt(() -> 46), fileNameLength);
		extraField = ByteDataUtil.readLazySlice(data, offset, fileNameLength.add(46), extraFieldLength);
		fileComment = ByteDataUtil.readLazySlice(data, offset, fileNameLength.add(46).add(extraFieldLength), fileCommentLength);
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE +
				fileNameLength.get() +
				extraFieldLength.get() +
				fileCommentLength.get();
	}

	@Nonnull
	@Override
	public PartType type() {
		return PartType.CENTRAL_DIRECTORY_FILE_HEADER;
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
		return versionMadeBy.get();
	}

	/**
	 * @param versionMadeBy
	 * 		Version of zip software used to make the archive.
	 */
	public void setVersionMadeBy(int versionMadeBy) {
		this.versionMadeBy.set(versionMadeBy);
	}

	/**
	 * @return Disk number where the archive starts from, or {@code 0xFFFF} for ZIP64.
	 */
	public int getDiskNumberStart() {
		return diskNumberStart.get();
	}

	/**
	 * @param diskNumberStart
	 * 		Disk number where the archive starts from, or {@code 0xFFFF} for ZIP64.
	 */
	public void setDiskNumberStart(int diskNumberStart) {
		this.diskNumberStart.set(diskNumberStart);
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
		return internalFileAttributes.get();
	}

	/**
	 * @param internalFileAttributes
	 * 		Internal attributes used for inferring content type.
	 */
	public void setInternalFileAttributes(int internalFileAttributes) {
		this.internalFileAttributes.set(internalFileAttributes);
	}

	/**
	 * For MS-DOS, the low order byte is the MS-DOS directory attribute byte.
	 * If input came from standard input, this field is zero.
	 *
	 * @return Host system dependent attributes.
	 */
	public int getExternalFileAttributes() {
		return externalFileAttributes.get();
	}

	/**
	 * @param externalFileAttributes
	 * 		Host system dependent attributes.
	 */
	public void setExternalFileAttributes(int externalFileAttributes) {
		this.externalFileAttributes.set(externalFileAttributes);
	}

	/**
	 * @return Offset from the start of the {@link #getDiskNumberStart() first disk} where the file appears.
	 * This should also be where the {@link LocalFileHeader} is located.  Or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public long getRelativeOffsetOfLocalHeader() {
		return relativeOffsetOfLocalHeader.get();
	}

	/**
	 * @param relativeOffsetOfLocalHeader
	 * 		Offset from the start of the {@link #getDiskNumberStart() first disk} where the file appears.
	 * 		This should also be where the {@link LocalFileHeader} is located.  Or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public void setRelativeOffsetOfLocalHeader(long relativeOffsetOfLocalHeader) {
		this.relativeOffsetOfLocalHeader.set(relativeOffsetOfLocalHeader & 0xFFFFFFFFL);
	}

	/**
	 * @return Length of {@link #getFileComment()}.
	 */
	public int getFileCommentLength() {
		return fileCommentLength.get();
	}


	/**
	 * @param fileCommentLength
	 * 		Length of {@link #getFileComment()}.
	 */
	public void setFileCommentLength(int fileCommentLength) {
		this.fileCommentLength.set(fileCommentLength & 0xFFFF);
	}

	/**
	 * @return File comment.
	 */
	public ByteData getFileComment() {
		return fileComment.get();
	}

	/**
	 * @param fileComment
	 * 		File comment.
	 */
	public void setFileComment(ByteData fileComment) {
		this.fileComment.set(fileComment);
	}

	/**
	 * @return File comment.
	 */
	public String getFileCommentAsString() {
		String fileCommentCache = this.fileCommentCache;
		if (fileCommentCache == null) {
			return this.fileCommentCache = ByteDataUtil.toString(fileComment.get());
		}
		return fileCommentCache;
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
		if (o == null || getClass() != o.getClass()) return false;

		CentralDirectoryFileHeader that = (CentralDirectoryFileHeader) o;

		if (!Objects.equals(linkedFileHeader, that.linkedFileHeader)) return false;
		if (!Objects.equals(versionMadeBy, that.versionMadeBy)) return false;
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
		if (!Objects.equals(fileCommentLength, that.fileCommentLength)) return false;
		if (!Objects.equals(diskNumberStart, that.diskNumberStart)) return false;
		if (!Objects.equals(internalFileAttributes, that.internalFileAttributes)) return false;
		if (!Objects.equals(externalFileAttributes, that.externalFileAttributes)) return false;
		if (!Objects.equals(relativeOffsetOfLocalHeader, that.relativeOffsetOfLocalHeader)) return false;
		if (!Objects.equals(fileName, that.fileName)) return false;
		if (!Objects.equals(extraField, that.extraField)) return false;
		return Objects.equals(fileComment, that.fileComment);
	}

	@Override
	public int hashCode() {
		int result = linkedFileHeader != null ? linkedFileHeader.hashCode() : 0;
		result = 31 * result + (versionMadeBy != null ? versionMadeBy.hashCode() : 0);
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
		result = 31 * result + (fileCommentLength != null ? fileCommentLength.hashCode() : 0);
		result = 31 * result + (diskNumberStart != null ? diskNumberStart.hashCode() : 0);
		result = 31 * result + (internalFileAttributes != null ? internalFileAttributes.hashCode() : 0);
		result = 31 * result + (externalFileAttributes != null ? externalFileAttributes.hashCode() : 0);
		result = 31 * result + (relativeOffsetOfLocalHeader != null ? relativeOffsetOfLocalHeader.hashCode() : 0);
		result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
		result = 31 * result + (extraField != null ? extraField.hashCode() : 0);
		result = 31 * result + (fileComment != null ? fileComment.hashCode() : 0);
		return result;
	}
}
