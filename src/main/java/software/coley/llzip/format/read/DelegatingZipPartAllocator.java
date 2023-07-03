package software.coley.llzip.format.read;

import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.EndOfCentralDirectory;
import software.coley.llzip.format.model.LocalFileHeader;

import javax.annotation.Nonnull;

/**
 * Allocator that delegates to another.
 *
 * @author Matt Coley
 */
public class DelegatingZipPartAllocator implements ZipPartAllocator {
	private final ZipPartAllocator delegate;

	/**
	 * @param delegate
	 * 		Delegate allocator.
	 */
	public DelegatingZipPartAllocator(@Nonnull ZipPartAllocator delegate) {
		this.delegate = delegate;
	}

	@Nonnull
	@Override
	public LocalFileHeader newLocalFileHeader() {
		return delegate.newLocalFileHeader();
	}

	@Nonnull
	@Override
	public CentralDirectoryFileHeader newCentralDirectoryFileHeader() {
		return delegate.newCentralDirectoryFileHeader();
	}

	@Nonnull
	@Override
	public EndOfCentralDirectory newEndOfCentralDirectory() {
		return delegate.newEndOfCentralDirectory();
	}
}
