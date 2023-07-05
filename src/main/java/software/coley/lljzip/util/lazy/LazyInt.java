package software.coley.lljzip.util.lazy;

import javax.annotation.Nonnull;
import java.util.function.IntSupplier;

/**
 * Lazy int getter.
 */
public class LazyInt extends Lazy<IntSupplier> {
	private int value;

	/**
	 * @param lookup
	 * 		Lazy lookup.
	 */
	public LazyInt(@Nonnull IntSupplier lookup) {
		super(lookup);
	}

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LazyInt copy() {
		LazyInt copy = new LazyInt(lookup);
		if (set) copy.set(value);
		return copy;
	}

	/**
	 * @param value
	 * 		Value to add to the current.
	 *
	 * @return New value that maps the current value plus the additional value.
	 */
	public LazyInt add(int value) {
		return new LazyInt(() -> value + lookup.getAsInt());
	}

	/**
	 * @param value
	 * 		Value to add to the current.
	 *
	 * @return New value that maps the current value plus the additional value.
	 */
	public LazyInt add(@Nonnull LazyInt value) {
		return new LazyInt(() -> value.get() + lookup.getAsInt());
	}

	/**
	 * @param value
	 * 		Int value.
	 */
	public void set(int value) {
		set = true;
		this.value = value;
	}

	/**
	 * @return Int value.
	 */
	public int get() {
		if (!set) {
			value = lookup.getAsInt();
			set = true;
		}
		return value;
	}

	@Override
	public String toString() {
		return Integer.toString(get());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LazyInt lazyInt = (LazyInt) o;

		return get() == lazyInt.get();
	}

	@Override
	public int hashCode() {
		return get();
	}
}
