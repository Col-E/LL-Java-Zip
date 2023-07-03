package software.coley.llzip.format.read;

import software.coley.llzip.format.model.JvmLocalFileHeader;
import software.coley.llzip.format.model.LocalFileHeader;

import javax.annotation.Nonnull;

/**
 * Part allocator that allocates {@link JvmLocalFileHeader special JVM local file headers}.
 *
 * @author Matt Coley
 */
public class JvmZipPartAllocator extends SimpleZipPartAllocator {
	@Nonnull
	@Override
	public LocalFileHeader newLocalFileHeader() {
		return new JvmLocalFileHeader();
	}
}
