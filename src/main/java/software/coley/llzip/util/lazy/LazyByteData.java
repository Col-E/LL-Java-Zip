package software.coley.llzip.util.lazy;

import software.coley.llzip.util.ByteData;

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
	public LazyByteData(Supplier<ByteData> lookup) {
		super(lookup);
	}

	/**
	 * @param value
	 * 		Data value.
	 */
	public void set(ByteData value) {
		set = true;
		this.value = value;
	}

	/**
	 * @return Data value.
	 */
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
