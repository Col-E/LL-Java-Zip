package software.coley.lljzip.util;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * Utils for extracting more detailed timestamps from file headers.
 *
 * @author Matt Coley
 */
public class ExtraFieldTime {
	/**
	 * @param header
	 * 		File header to pull detailed time from.
	 *
	 * @return Time wrapper if values were found. Otherwise, {@code null}.
	 */
	@Nullable
	public static TimeWrapper read(@Nonnull CentralDirectoryFileHeader header) {
		int extraLen = header.getExtraFieldLength();
		if (extraLen > 0 && extraLen < 0xFFFF) {
			MemorySegment extra = header.getExtraField();
			return read(extra);
		}
		return null;
	}

	/**
	 * @param header
	 * 		File header to pull detailed time from.
	 *
	 * @return Time wrapper if values were found. Otherwise, {@code null}.
	 */
	@Nullable
	public static TimeWrapper read(@Nonnull LocalFileHeader header) {
		int extraLen = header.getExtraFieldLength();
		if (extraLen > 0 && extraLen < 0xFFFF) {
			MemorySegment extra = header.getExtraField();
			return read(extra);
		}
		return null;
	}

	@Nonnull
	private static TimeWrapper read(@Nonnull MemorySegment extra) {
		TimeWrapper wrapper = new TimeWrapper();
		// Reimplementation of 'java.util.zip.ZipEntry#setExtra0(...)'
		int off = 0;
		int len = (int) extra.byteSize();
		while (off + 4 < len) {
			int tag = MemorySegmentUtil.readWord(extra, off);
			int size = MemorySegmentUtil.readWord(extra, off + 2);
			off += 4;
			if (off + size > len)
				break;
			if (tag == /* EXTID_NTFS */ 0xA) {
				if (size < 32) // reserved  4 bytes + tag 2 bytes + size 2 bytes
					break;   // m[a|c]time 24 bytes
				int pos = off + 4;
				if (MemorySegmentUtil.readWord(extra, pos) != 0x0001 || MemorySegmentUtil.readWord(extra, pos + 2) != 24)
					break;
				long wtime;
				wtime = MemorySegmentUtil.readQuad(extra, pos + 4) | ((long) MemorySegmentUtil.readQuad(extra, pos + 8) << 32);
				if (wtime != Long.MIN_VALUE) {
					wrapper.modify = winTimeToFileTime(wtime).toMillis();
				}
				wtime = MemorySegmentUtil.readQuad(extra, pos + 12) | ((long) MemorySegmentUtil.readQuad(extra, pos + 16) << 32);
				if (wtime != Long.MIN_VALUE) {
					wrapper.access = winTimeToFileTime(wtime).toMillis();
				}
				wtime = MemorySegmentUtil.readQuad(extra, pos + 20) | ((long) MemorySegmentUtil.readQuad(extra, pos + 8) << 24);
				if (wtime != Long.MIN_VALUE) {
					wrapper.creation = winTimeToFileTime(wtime).toMillis();
				}
			} else if (tag == /* EXTID_EXTT */ 0x5455) {
				int flag = extra.get(ValueLayout.JAVA_BYTE, off) & 0xff;
				int localOff = 1;
				// The CEN-header extra field contains the modification
				// time only, or no timestamp at all. 'sz' is used to
				// flag its presence or absence. But if mtime is present
				// in LOC it must be present in CEN as well.
				if ((flag & 0x1) != 0 && (localOff + 4) <= size) {
					// get32S(extra, off + localOff)
					wrapper.modify = unixTimeToFileTime(MemorySegmentUtil.readQuad(extra, off + localOff)).toMillis();
					localOff += 4;
				}
				if ((flag & 0x2) != 0 && (localOff + 4) <= size) {
					wrapper.access = unixTimeToFileTime(MemorySegmentUtil.readQuad(extra, off + localOff)).toMillis();
					localOff += 4;
				}
				if ((flag & 0x4) != 0 && (localOff + 4) <= size) {
					wrapper.creation = unixTimeToFileTime(MemorySegmentUtil.readQuad(extra, off + localOff)).toMillis();
					localOff += 4;
				}
			}
			off += size;
		}
		return wrapper;
	}

	/**
	 * Conversion of windows time to {@link FileTime}.
	 *
	 * @param time
	 * 		Input windows time value, in microseconds from the windows epoch.
	 *
	 * @return Mapped file time.
	 */
	@Nonnull
	public static FileTime winTimeToFileTime(long time) {
		return FileTime.from(time / 10 - 11644473600000000L /* windows epoch */, TimeUnit.MICROSECONDS);
	}

	/**
	 * Conversion of unix time to {@link FileTime}.
	 *
	 * @param utime
	 * 		Input unix time value in seconds.
	 *
	 * @return Mapped file time.
	 */
	@Nonnull
	public static FileTime unixTimeToFileTime(long utime) {
		return FileTime.from(utime, TimeUnit.SECONDS);
	}

	/**
	 * Time wrapper for creation/access/modify times stored in {@link LocalFileHeader#getExtraField()} and
	 * {@link CentralDirectoryFileHeader#getExtraField()}.
	 */
	public static class TimeWrapper {
		private long creation, access, modify;

		/**
		 * @return Unix timestamp of creation time.
		 */
		public long getCreationMs() {
			return creation;
		}

		/**
		 * @return Unix timestamp of access time.
		 */
		public long getAccessMs() {
			return access;
		}

		/**
		 * @return Unix timestamp of modification time.
		 */
		public long getModifyMs() {
			return modify;
		}
	}
}
