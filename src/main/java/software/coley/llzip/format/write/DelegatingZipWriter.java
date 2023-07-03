package software.coley.llzip.format.write;

import software.coley.llzip.format.model.ZipArchive;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writer that delegates to another.
 * <p>
 * Typically useful as convenience for writing chained writers where the first in the chain manipulate input data
 * and the last in the chain is responsible for writing to the output stream.
 *
 * @author Matt Coley
 */
public class DelegatingZipWriter implements ZipWriter {
	private final ZipWriter delegate;

	/**
	 * @param delegate Delegate writer.
	 */
	public DelegatingZipWriter(@Nullable ZipWriter delegate) {
		this.delegate = delegate;
	}

	@Override
	public void write(@Nonnull ZipArchive archive, @Nonnull OutputStream os) throws IOException {
		if (delegate != null) delegate.write(archive, os);
	}
}
