package software.coley.llzip.util.lazy;

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
	public LazyInt(IntSupplier lookup) {
		super(lookup);
	}

	public LazyInt add(int value) {
		return new LazyInt(() -> value + lookup.getAsInt());
	}

	public LazyInt add(LazyInt value) {
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
