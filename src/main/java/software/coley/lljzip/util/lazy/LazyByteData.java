package software.coley.lljzip.util.lazy;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Lazy {@link MemorySegment} getter.
 */
public class LazyByteData extends Lazy<Supplier<MemorySegment>> {
	private MemorySegment value;

	/**
	 * @param lookup
	 * 		Lazy lookup.
	 */
	public LazyByteData(@Nonnull Supplier<MemorySegment> lookup) {
		super(lookup);
	}

	/**
	 * @return Copy.
	 */
	@Nonnull
	public LazyByteData copy() {
		LazyByteData copy = new LazyByteData(lookup);
		copy.id = id;
		if (set) copy.set(value);
		return copy;
	}

	/**
	 * @param value
	 * 		Data value.
	 */
	public void set(@Nonnull MemorySegment value) {
		set = true;
		this.value = value;
	}

	/**
	 * @return Data value.
	 */
	@Nonnull
	public MemorySegment get() {
		if (!set) {
			value = lookup.get();
			set = true;
		}
		return value;
	}

	@Override
	public String toString() {
		return id + " data[" + get().byteSize() + "]";
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
