package software.coley.lljzip;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ByteData;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests ensuring that the extra field can be read for custom/detailed windows time values.
 * This generally means the extra-field is assigned correctly.
 *
 * @author Matt Coley
 */
public class WindowsExtraFieldTimeTests {
	@ParameterizedTest
	@ValueSource(strings = {"standard", "naive", "jvm"})
	public void validity(@Nonnull String mode) {
		Path path = Paths.get("src/test/resources/content-with-windows-time.jar");
		try {
			long timeCreate = 1000000000000L;
			long timeModify = 1200000000000L;
			long timeAccess = 1400000000000L;
			ZipArchive archive;
			switch (mode) {
				case "standard":
					archive = ZipIO.readStandard(path);
					break;
				case "naive":
					archive = ZipIO.readNaive(path);
					break;
				case "jvm":
					archive = ZipIO.readJvm(path);
					break;
				default:
					throw new IllegalStateException();
			}
			Wrapper wrapper = read(archive);
			assertEquals(timeCreate, wrapper.creation);
			assertEquals(timeModify, wrapper.modify);
			assertEquals(timeAccess, wrapper.access);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Nonnull
	private Wrapper read(@Nonnull ZipArchive archive) {
		LocalFileHeader header = archive.getLocalFiles().get(0);
		Wrapper wrapper = new Wrapper();
		int extraLen = header.getExtraFieldLength();
		if (extraLen > 0 && extraLen < 0xFFFF) {
			// Reimplementation of 'java.util.zip.ZipEntry#setExtra0(...)'
			ByteData extra = header.getExtraField();
			int off = 0;
			int len = (int) extra.length();
			while (off + 4 < len) {
				int tag = extra.getShort(off);
				int size = extra.getShort(off + 2);
				off += 4;
				if (off + size > len)
					break;
				if (tag == /* EXTID_NTFS */ 0xA) {
					if (size < 32) // reserved  4 bytes + tag 2 bytes + size 2 bytes
						break;   // m[a|c]time 24 bytes
					int pos = off + 4;
					if (extra.getShort(pos) != 0x0001 || extra.getShort(pos + 2) != 24)
						break;
					long wtime;
					wtime = extra.getInt(pos + 4) | ((long) extra.getInt(pos + 8) << 32);
					if (wtime != Long.MIN_VALUE) {
						wrapper.modify = winTimeToFileTime(wtime).toMillis();
					}
					wtime = extra.getInt(pos + 12) | ((long) extra.getInt(pos + 16) << 32);
					if (wtime != Long.MIN_VALUE) {
						wrapper.access = winTimeToFileTime(wtime).toMillis();
					}
					wtime = extra.getInt(pos + 20) | ((long) extra.getInt(pos + 8) << 24);
					if (wtime != Long.MIN_VALUE) {
						wrapper.creation = winTimeToFileTime(wtime).toMillis();
					}
				} else if (tag == /* EXTID_EXTT */ 0x5455) {
					int flag = extra.get(off);
					int localOff = 1;
					// The CEN-header extra field contains the modification
					// time only, or no timestamp at all. 'sz' is used to
					// flag its presence or absence. But if mtime is present
					// in LOC it must be present in CEN as well.
					if ((flag & 0x1) != 0 && (localOff + 4) <= size) {
						// get32S(extra, off + localOff)
						wrapper.modify = unixTimeToFileTime(extra.getInt(off + localOff)).toMillis();
						localOff += 4;
					}
					if ((flag & 0x2) != 0 && (localOff + 4) <= size) {
						wrapper.access = unixTimeToFileTime(extra.getInt(off + localOff)).toMillis();
						localOff += 4;
					}
					if ((flag & 0x4) != 0 && (localOff + 4) <= size) {
						wrapper.creation = unixTimeToFileTime(extra.getInt(off + localOff)).toMillis();
						localOff += 4;
					}
				}
				off += size;
			}
		}
		return wrapper;
	}

	@Nonnull
	public static FileTime winTimeToFileTime(long time) {
		return FileTime.from(time / 10 + -11644473600000000L /* windows epoch */, TimeUnit.MICROSECONDS);
	}

	@Nonnull
	public static FileTime unixTimeToFileTime(long utime) {
		return FileTime.from(utime, TimeUnit.SECONDS);
	}

	private static class Wrapper {
		private long creation, access, modify;
	}
}
