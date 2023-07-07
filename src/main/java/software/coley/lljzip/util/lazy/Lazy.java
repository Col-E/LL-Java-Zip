package software.coley.lljzip.util.lazy;

import javax.annotation.Nonnull;

/**
 * Common lazy type.
 *
 * @param <S>
 * 		Lazy supplier type.
 */
public abstract class Lazy<S> {
	protected final S lookup;
	protected boolean set;
	protected String id = "";

	public Lazy(@Nonnull S lookup) {
		this.lookup = lookup;
	}

	/**
	 * @param id
	 * 		Value id.
	 * @param <L>
	 * 		Self type.
	 *
	 * @return Self.
	 */
	@SuppressWarnings("unchecked")
	public <L extends Lazy<S>> L withId(String id) {
		this.id = id;
		return (L) this;
	}
}
