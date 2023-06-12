package software.coley.llzip.util.lazy;

import java.util.function.LongSupplier;

/**
 * Lazy int getter.
 */
public class LazyLong extends Lazy<LongSupplier> {
	private long value;

	/**
	 * @param lookup
	 * 		Lazy lookup.
	 */
	public LazyLong(LongSupplier lookup) {
		super(lookup);
	}

	/**
	 * @param value
	 * 		Long value.
	 */
	public void set(long value) {
		set = true;
		this.value = value;
	}

	/**
	 * @return Long value.
	 */
	public long get() {
		if (!set) {
			value = lookup.getAsLong();
			set = true;
		}
		return value;
	}

	@Override
	public String toString() {
		return Long.toString(get());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LazyLong lazyLong = (LazyLong) o;

		return get() == lazyLong.get();
	}

	@Override
	public int hashCode() {
		long v = get();
		return (int) (v ^ (v >>> 32));
	}
}
