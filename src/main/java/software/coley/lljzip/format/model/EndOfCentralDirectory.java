package software.coley.lljzip.format.model;

import software.coley.lljzip.util.data.StringData;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

import static software.coley.lljzip.util.MemorySegmentUtil.readQuad;
import static software.coley.lljzip.util.MemorySegmentUtil.readWord;

/**
 * ZIP EndOfCentralDirectory structure.
 * <pre>
 * {@code
 *     SIGNATURE Signature ;
 *     WORD  DiskNumber ;
 *     WORD  CentralDirectoryStartDisk ;
 *     WORD  CentralDirectoryStartOffset ;
 *     WORD  NumEntries ;
 *     DWORD CentralDirectorySize ;
 *     DWORD CentralDirectoryOffset ;
 *     WORD  ZipCommentLength ;
 *     char  ZipComment[ZipCommentLength] ;
 * }
 * </pre>
 *
 * @author Matt Coley
 */
public class EndOfCentralDirectory implements ZipPart, ZipRead {
	private transient long offset = -1L;
	// Zip spec elements
	private int diskNumber;
	private int centralDirectoryStartDisk;
	private int centralDirectoryStartOffset; // TODO: spec and wikipedia articles disagree about purpose?
	private int numEntries;
	private long centralDirectorySize;
	private long centralDirectoryOffset;
	private int zipCommentLength;
	private StringData zipComment;

	/**
	 * @return Copy.
	 */
	@Nonnull
	public EndOfCentralDirectory copy() {
		EndOfCentralDirectory copy = new EndOfCentralDirectory();
		copy.offset = offset;
		copy.diskNumber = diskNumber;
		copy.centralDirectoryStartDisk = centralDirectoryStartDisk;
		copy.centralDirectoryStartOffset = centralDirectoryStartOffset;
		copy.numEntries = numEntries;
		copy.centralDirectorySize = centralDirectorySize;
		copy.centralDirectoryOffset = centralDirectoryOffset;
		copy.zipCommentLength = zipCommentLength;
		copy.zipComment = zipComment;
		return copy;
	}

	@Override
	public void read(@Nonnull MemorySegment data, long offset) {
		this.offset = offset;
		diskNumber = readWord(data, offset + 4);
		centralDirectoryStartDisk = readWord(data, offset + 6);
		centralDirectoryStartOffset = readWord(data, offset + 8);
		numEntries = readWord(data, offset + 10);
		setCentralDirectorySize(readQuad(data, offset + 12));
		setCentralDirectoryOffset(readQuad(data, offset + 16));
		setZipCommentLength(readWord(data, offset + 20));
		zipComment = StringData.of(data.asSlice(offset + 22, zipCommentLength));
	}

	@Override
	public long length() {
		return 22L + zipComment.get().length();
	}

	@Nonnull
	@Override
	public PartType type() {
		return PartType.END_OF_CENTRAL_DIRECTORY;
	}

	@Override
	public long offset() {
		return offset;
	}

	/**
	 * @return Disk number for multi-file archives, or {@code 0xFFFF} for ZIP64.
	 */
	public int getDiskNumber() {
		return diskNumber;
	}

	/**
	 * @param diskNumber
	 * 		Disk number for multi-file archives, or {@code 0xFFFF} for ZIP64.
	 */
	public void setDiskNumber(int diskNumber) {
		this.diskNumber = diskNumber;
	}

	/**
	 * @return The first disk number where the central directory starts at, or {@code 0xFFFF} for ZIP64.
	 */
	public int getCentralDirectoryStartDisk() {
		return centralDirectoryStartDisk;
	}

	/**
	 * @param centralDirectoryStartDisk
	 * 		The first disk number where the central directory starts at, or {@code 0xFFFF} for ZIP64.
	 */
	public void setCentralDirectoryStartDisk(int centralDirectoryStartDisk) {
		this.centralDirectoryStartDisk = centralDirectoryStartDisk;
	}

	/**
	 * @return ?
	 */
	public int getCentralDirectoryStartOffset() {
		return centralDirectoryStartOffset;
	}

	/**
	 * @param centralDirectoryStartOffset
	 * 		?
	 */
	public void setCentralDirectoryStartOffset(int centralDirectoryStartOffset) {
		this.centralDirectoryStartOffset = centralDirectoryStartOffset;
	}

	/**
	 * @return Number of {@link CentralDirectoryFileHeader} that should exist in the archive.
	 */
	public int getNumEntries() {
		return numEntries;
	}

	/**
	 * @param numEntries
	 * 		Number of {@link CentralDirectoryFileHeader} that should exist in the archive.
	 */
	public void setNumEntries(int numEntries) {
		this.numEntries = numEntries;
	}

	/**
	 * @return Size of central directory in bytes or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public long getCentralDirectorySize() {
		return centralDirectorySize;
	}

	/**
	 * @param centralDirectorySize
	 * 		Size of central directory in bytes, or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public void setCentralDirectorySize(long centralDirectorySize) {
		this.centralDirectorySize = centralDirectorySize & 0xFFFFFFFFL;
	}

	/**
	 * @return Offset of first {@link CentralDirectoryFileHeader} with respect to
	 * {@link #getCentralDirectoryStartDisk() starting disk number}, or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public long getCentralDirectoryOffset() {
		return centralDirectoryOffset;
	}

	/**
	 * @param centralDirectoryOffset
	 * 		Offset of first {@link CentralDirectoryFileHeader} with respect to
	 *        {@link #getCentralDirectoryStartDisk() starting disk number}, or {@code 0xFFFFFFFF} for ZIP64.
	 */
	public void setCentralDirectoryOffset(long centralDirectoryOffset) {
		this.centralDirectoryOffset = centralDirectoryOffset & 0xFFFFFFFFL;
	}

	/**
	 * @return {@link #getZipComment() Comment} length.
	 */
	public int getZipCommentLength() {
		return zipCommentLength;
	}

	/**
	 * @param zipCommentLength
	 *        {@link #getZipComment() Comment} length.
	 */
	public void setZipCommentLength(int zipCommentLength) {
		this.zipCommentLength = zipCommentLength & 0xFFFF;
	}

	/**
	 * @return Optional comment, or empty string.
	 */
	public StringData getZipComment() {
		return zipComment;
	}

	/**
	 * @param zipComment
	 * 		Optional comment, or empty string.
	 */
	public void setZipComment(StringData zipComment) {
		this.zipComment = zipComment;
	}

	/**
	 * @return Optional comment, or empty string.
	 */
	public String getZipCommentAsString() {
		return zipComment.get();
	}

	@Override
	public String toString() {
		return "EndOfCentralDirectory{" +
				"offset=" + offset +
				", diskNumber=" + diskNumber +
				", centralDirectoryStartDisk=" + centralDirectoryStartDisk +
				", centralDirectoryStartOffset=" + centralDirectoryStartOffset +
				", numEntries=" + numEntries +
				", centralDirectorySize=" + centralDirectorySize +
				", centralDirectoryOffset=" + centralDirectoryOffset +
				", zipCommentLength=" + zipCommentLength +
				", zipComment='" + getZipCommentAsString() + '\'' +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof EndOfCentralDirectory that)) return false;

		if (offset != that.offset) return false;
		if (diskNumber != that.diskNumber) return false;
		if (centralDirectoryStartDisk != that.centralDirectoryStartDisk) return false;
		if (centralDirectoryStartOffset != that.centralDirectoryStartOffset) return false;
		if (numEntries != that.numEntries) return false;
		if (centralDirectorySize != that.centralDirectorySize) return false;
		if (centralDirectoryOffset != that.centralDirectoryOffset) return false;
		if (zipCommentLength != that.zipCommentLength) return false;
		return Objects.equals(zipComment, that.zipComment);
	}

	@Override
	public int hashCode() {
		int result = (int) (offset ^ (offset >>> 32));
		result = 31 * result + diskNumber;
		result = 31 * result + centralDirectoryStartDisk;
		result = 31 * result + centralDirectoryStartOffset;
		result = 31 * result + numEntries;
		result = 31 * result + (int) (centralDirectorySize ^ (centralDirectorySize >>> 32));
		result = 31 * result + (int) (centralDirectoryOffset ^ (centralDirectoryOffset >>> 32));
		result = 31 * result + zipCommentLength;
		result = 31 * result + (zipComment != null ? zipComment.hashCode() : 0);
		return result;
	}
}
