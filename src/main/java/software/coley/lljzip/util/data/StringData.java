package software.coley.lljzip.util.data;

import software.coley.lljzip.util.MemorySegmentUtil;

import javax.annotation.Nonnull;
import java.lang.foreign.MemorySegment;

/**
 * Wrapper for reading {@link String} content from a variety of sources.
 *
 * @author Matt Coley
 */
public interface StringData {
	@Nonnull
	String get();

	@Nonnull
	default StringData substring(int begin, int end) {
		return of(get().substring(begin, end));
	}

	@Nonnull
	default StringData copy() {
		return of(get());
	}

	@Nonnull
	static StringData empty() {
		return Empty.INSTANCE;
	}

	@Nonnull
	static StringData of(@Nonnull String source) {
		if (source.isEmpty())
			return empty();
		return new Literal(source);
	}

	@Nonnull
	static StringData of(@Nonnull MemorySegment segment) {
		if (segment.byteSize() == 0)
			return empty();
		return new Caching(new FullSegment(segment));
	}

	@Nonnull
	static StringData of(@Nonnull MemorySegment segment, long offset, long length) {
		if (length == 0)
			return empty();
		return new Caching(new PartialSegment(segment, offset, length));
	}

	class Caching implements StringData {

		private final StringData delegate;
		private String cached;

		public Caching(@Nonnull StringData delegate) {
			this.delegate = delegate;
		}

		@Nonnull
		@Override
		public String get() {
			if (cached == null)
				cached = delegate.get();
			return cached;
		}
	}

	class PartialSegment implements StringData {
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
		public String get() {
			return MemorySegmentUtil.readString(segment, offset, length);
		}
	}

	class FullSegment implements StringData {
		private final MemorySegment segment;

		public FullSegment(@Nonnull MemorySegment segment) {
			this.segment = segment;
		}

		@Nonnull
		@Override
		public String get() {
			return MemorySegmentUtil.toString(segment);
		}
	}

	class Literal implements StringData {
		private final String content;

		public Literal(@Nonnull String content) {
			this.content = content;
		}

		@Nonnull
		@Override
		public String get() {
			return content;
		}

		@Nonnull
		@Override
		public StringData copy() {
			return this;
		}
	}

	class Empty implements StringData {
		private static final Empty INSTANCE = new Empty();

		private Empty() {}

		@Nonnull
		@Override
		public String get() {
			return "";
		}
	}
}
