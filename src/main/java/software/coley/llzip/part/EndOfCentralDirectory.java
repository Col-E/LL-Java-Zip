package software.coley.llzip.part;

import software.coley.llzip.util.Array;


/**
 * ZIP EndOfCentralDirectory structure.
 *
 * @author Matt Coley
 */
public class EndOfCentralDirectory implements ZipPart, ZipRead {
	private transient int offset = -1;
	// Zip spec elements
	private int diskNumber;
	private int centralDirectoryStartDisk;
	private int centralDirectoryStartOffset;
	private int numEntries;
	private int centralDirectorySize;
	private int centralDirectoryOffset;
	private int zipCommentLength;
	private String zipComment;

	@Override
	public void read(byte[] data, int offset) {
		this.offset = offset;
		diskNumber = Array.readWord(data, offset + 4);
		centralDirectoryStartDisk = Array.readWord(data, offset + 6);
		centralDirectoryStartOffset = Array.readWord(data, offset + 8);
		numEntries = Array.readWord(data, offset + 10);
		centralDirectorySize = Array.readQuad(data, offset + 12);
		centralDirectoryOffset = Array.readQuad(data, offset + 16);
		zipCommentLength = Array.readWord(data, offset + 20);
		zipComment = Array.readString(data, offset + 22, zipCommentLength);
	}

	@Override
	public int length() {
		return 22 + zipComment.length();
	}

	@Override
	public PartType type() {
		return PartType.END_OF_CENTRAL_DIRECTORY;
	}

	@Override
	public int offset() {
		return offset;
	}

	public int getDiskNumber() {
		return diskNumber;
	}

	public void setDiskNumber(int diskNumber) {
		this.diskNumber = diskNumber;
	}

	public int getCentralDirectoryStartDisk() {
		return centralDirectoryStartDisk;
	}

	public void setCentralDirectoryStartDisk(int centralDirectoryStartDisk) {
		this.centralDirectoryStartDisk = centralDirectoryStartDisk;
	}

	public int getCentralDirectoryStartOffset() {
		return centralDirectoryStartOffset;
	}

	public void setCentralDirectoryStartOffset(int centralDirectoryStartOffset) {
		this.centralDirectoryStartOffset = centralDirectoryStartOffset;
	}

	public int getNumEntries() {
		return numEntries;
	}

	public void setNumEntries(int numEntries) {
		this.numEntries = numEntries;
	}

	public int getCentralDirectorySize() {
		return centralDirectorySize;
	}

	public void setCentralDirectorySize(int centralDirectorySize) {
		this.centralDirectorySize = centralDirectorySize;
	}

	public int getCentralDirectoryOffset() {
		return centralDirectoryOffset;
	}

	public void setCentralDirectoryOffset(int centralDirectoryOffset) {
		this.centralDirectoryOffset = centralDirectoryOffset;
	}

	public int getZipCommentLength() {
		return zipCommentLength;
	}

	public void setZipCommentLength(int zipCommentLength) {
		this.zipCommentLength = zipCommentLength;
	}

	public String getZipComment() {
		return zipComment;
	}

	public void setZipComment(String zipComment) {
		this.zipComment = zipComment;
	}
}
