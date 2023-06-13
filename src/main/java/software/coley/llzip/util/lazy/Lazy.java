package software.coley.llzip.util.lazy;

/**
 * Common lazy type.
 *
 * @param <S>
 * 		Lazy supplier type.
 */
public class Lazy<S> {
	protected final S lookup;
	protected boolean set;

	public Lazy(S lookup) {
		this.lookup = lookup;
	}
}
