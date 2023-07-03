package software.coley.llzip.format.read;

import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.LocalFileHeader;

import javax.annotation.Nonnull;

/**
 * Simple part allocator implementation.
 *
 * @author Matt Coley
 */
public class SimpleZipPartAllocator implements ZipPartAllocator{
	@Nonnull
	@Override
	public LocalFileHeader newLocalFileHeader() {
		return new LocalFileHeader();
	}

	@Nonnull
	@Override
	public CentralDirectoryFileHeader newCentralDirectoryFileHeader() {
		return new CentralDirectoryFileHeader();
	}

	@Nonnull
	@Override
	public EndOfCentralDirectory newEndOfCentralDirectory() {
		return new EndOfCentralDirectory();
	}
}
