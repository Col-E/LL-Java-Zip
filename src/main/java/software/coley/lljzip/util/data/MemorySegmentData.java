package software.coley.lljzip.util.data;

import software.coley.lljzip.util.MemorySegmentUtil;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;

/**
 * Wrapper for reading {@link MemorySegment} content.
 *
 * @author Matt Coley
 */
public interface MemorySegmentData {
	@Nonnull
	MemorySegment get();

	default long length() {
		return get().byteSize();
	}

	@Nonnull
	default MemorySegmentData copy() {
		return of(get());
	}

	@Nonnull
	static MemorySegmentData empty() {
		return FullSegment.EMPTY;
	}

	@Nonnull
	static MemorySegmentData of(@Nonnull byte[] source) {
		if (source.length == 0)
			return empty();
		return of(MemorySegment.ofArray(source));
	}

	@Nonnull
	static MemorySegmentData of(@Nonnull MemorySegment segment) {
		if (segment.byteSize() == 0)
			return empty();
		return new FullSegment(segment);
	}

	@Nonnull
	static MemorySegmentData of(@Nonnull MemorySegment segment, long offset, long length) {
		if (length == 0)
			return empty();
		return new Caching(new PartialSegment(segment, offset, length));
	}

	class Caching implements MemorySegmentData {
		private final MemorySegmentData delegate;
		private MemorySegment cached;

		public Caching(@Nonnull MemorySegmentData delegate) {
			this.delegate = delegate;
		}

		@Nonnull
		@Override
		public MemorySegment get() {
			if (cached == null)
				cached = delegate.get();
			return cached;
		}
	}

	class PartialSegment implements MemorySegmentData {
		private final MemorySegment segment;
		private final long offset;
		private final long length;

		public PartialSegment(@Nonnull MemorySegment segment, long offset, long length) {
			this.segment = segment;
			this.offset = offset;
			this.length = length;
		}

		@Nonnull
		@Override
		public MemorySegment get() {
			return segment.asSlice(offset, length);
		}
	}

	class FullSegment implements MemorySegmentData {
		private static final FullSegment EMPTY = new FullSegment(MemorySegmentUtil.EMPTY);
		private final MemorySegment segment;

		public FullSegment(@Nonnull MemorySegment segment) {
			this.segment = segment;
		}

		@Nonnull
		@Override
		public MemorySegment get() {
			return segment;
		}

		@Nonnull
		@Override
		public MemorySegmentData copy() {
			return this;
		}
	}
}
