package software.coley.lljzip.util.lazy;

import javax.annotation.Nonnull;
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
	public LazyLong(@Nonnull LongSupplier lookup) {
		super(lookup);
	}

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LazyLong copy() {
		LazyLong copy = new LazyLong(lookup);
		copy.id = id;
		if (set) copy.set(value);
		return copy;
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
		return id + " " + get();
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
