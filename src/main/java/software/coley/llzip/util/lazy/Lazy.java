package software.coley.llzip.util.lazy;

import javax.annotation.Nonnull;

/**
 * Common lazy type.
 *
 * @param <S>
 * 		Lazy supplier type.
 */
public class Lazy<S> {
	protected final S lookup;
	protected boolean set;

	public Lazy(@Nonnull S lookup) {
		this.lookup = lookup;
	}
}
