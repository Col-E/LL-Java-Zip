package software.coley.llzip.format.model;

import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import static software.coley.llzip.util.ByteDataUtil.*;

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
	private ByteData fileComment;
	private int diskNumberStart;
	private int internalFileAttributes;
	private int externalFileAttributes;
	private long relativeOffsetOfLocalHeader;

	// String cache values
	private transient String fileCommentCache;

	@Override
	public void read(ByteData data, long offset) {
		super.read(data, offset);
		data = data.sliceOf(offset, data.length() - offset);
		versionMadeBy = readWord(data, 4);
		versionMadeBy = readWord(data, 4);
		versionNeededToExtract = readWord(data, 6);
		generalPurposeBitFlag = readWord(data, 8);
		compressionMethod = readWord(data, 10);
		lastModFileTime = readWord(data, 12);
		lastModFileDate = readWord(data, 14);
		crc32 = readQuad(data, 16);
		compressedSize = readUnsignedQuad(data, 20);
		uncompressedSize = readUnsignedQuad(data, 24);
		fileNameLength = readWord(data, 28);
		extraFieldLength = readWord(data, 30);
		fileCommentLength = readWord(data, 32);
		diskNumberStart = readWord(data, 34);
		internalFileAttributes = readWord(data, 36);
		externalFileAttributes = readQuad(data, 38);
		relativeOffsetOfLocalHeader = readUnsignedQuad(data, 42);
		fileName = data.sliceOf(46, fileNameLength);
		extraField = data.sliceOf(46 + fileNameLength, extraFieldLength);
		fileComment = data.sliceOf(46 + fileNameLength + extraFieldLength, fileCommentLength);
	}

	@Override
	public long length() {
		return MIN_FIXED_SIZE +
				fileNameLength +
				extraFieldLength +
				fileCommentLength;
	}

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
		this.relativeOffsetOfLocalHeader = relativeOffsetOfLocalHeader;
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
		this.fileCommentLength = fileCommentLength;
	}

	/**
	 * @return File comment.
	 */
	public ByteData getFileComment() {
		return fileComment;
	}

	/**
	 * @param fileComment
	 * 		File comment.
	 */
	public void setFileComment(ByteData fileComment) {
		this.fileComment = fileComment;
	}

	/**
	 * @return File comment.
	 */
	public String getFileCommentAsString() {
		String fileCommentCache = this.fileCommentCache;
		if (fileCommentCache == null) {
			return this.fileCommentCache = ByteDataUtil.toString(fileComment);
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
		return offset == that.offset;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(offset);
	}
}
