package software.coley.lljzip.format.read;

import javax.annotation.Nonnull;

/**
 * Base outline for zip readers that use a pluggable {@link ZipPartAllocator}.
 *
 * @author Matt Coley
 */
public abstract class AbstractZipReader extends DelegatingZipPartAllocator implements ZipReader {
	/**
	 * @param allocator
	 * 		Allocator to use.
	 */
	public AbstractZipReader(@Nonnull ZipPartAllocator allocator) {
		super(allocator);
	}
}
