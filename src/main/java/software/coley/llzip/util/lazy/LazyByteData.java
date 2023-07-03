package software.coley.llzip.util.lazy;

import software.coley.llzip.util.ByteData;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy {@link ByteData} getter.
 */
public class LazyByteData extends Lazy<Supplier<ByteData>> {
	private ByteData value;

	/**
	 * @param lookup
	 * 		Lazy lookup.
	 */
	public LazyByteData(@Nonnull Supplier<ByteData> lookup) {
		super(lookup);
	}

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LazyByteData copy() {
		LazyByteData copy = new LazyByteData(lookup);
		if (set) copy.set(value);
		return copy;
	}

	/**
	 * @param value
	 * 		Data value.
	 */
	public void set(@Nonnull ByteData value) {
		set = true;
		this.value = value;
	}

	/**
	 * @return Data value.
	 */
	@Nonnull
	public ByteData get() {
		if (!set) {
			value = lookup.get();
			set = true;
		}
		return value;
	}

	@Override
	public String toString() {
		return "data[" + get().length() + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		LazyByteData that = (LazyByteData) o;

		return Objects.equals(get(), that.get());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(get());
	}
}
