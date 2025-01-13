package software.coley.lljzip.format.read;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.EndOfCentralDirectory;
import software.coley.lljzip.format.model.LocalFileHeader;

import javax.annotation.Nonnull;

/**
 * Simple part allocator implementation.
 *
 * @author Matt Coley
 */
public class SimpleZipPartAllocator implements ZipPartAllocator {
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
