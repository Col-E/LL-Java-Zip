package software.coley.lljzip;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.lljzip.util.MemorySegmentUtil;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilities for zip tests.
 *
 * @author Matt Coley
 */
public class Utils {
	private static final int VERSION_ZIP64 = 45;
	private static final int GP_FLAG_DATA_DESCRIPTOR = 0x0008;
	private static final long ZIP64_MAGIC_DWORD = 0xFFFFFFFFL;

	/**
	 * Asserts the string has been found and is the <b>ONLY</b> matching string in the class.
	 *
	 * @param code
	 * 		Class bytecode.
	 * @param target
	 * 		String instance to look for.
	 */
	public static void assertDefinesString(byte[] code, String target) {
		boolean[] visited = new boolean[1];
		ClassReader cr = new ClassReader(code);
		cr.accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitLdcInsn(Object value) {
						visited[0] = true;
						assertEquals(target, value);
					}
				};
			}
		}, ClassReader.SKIP_FRAMES);
		assertTrue(visited[0], "The entry did not visit any LDC constants");
	}

	/**
	 * Asserts the string has been found and is the <b>ONLY</b> matching string in the class.
	 *
	 * @param code
	 * 		Class bytecode.
	 * @param target
	 * 		String instance to look for.
	 */
	public static void assertDefinesString(MemorySegment code, String target) {
		assertDefinesString(MemorySegmentUtil.toByteArray(code), target);
	}

	@Nonnull
	static byte[] zip64OffsetArchive() throws IOException {
		List<EntrySpec> entries = List.of(
				new EntrySpec("alpha.txt", "alpha".getBytes(StandardCharsets.UTF_8), false, false),
				new EntrySpec("beta.txt", "second-entry".getBytes(StandardCharsets.UTF_8), false, false)
		);
		return createArchive(entries, false);
	}

	@Nonnull
	static byte[] zip64DataDescriptorArchive(boolean includeDescriptorSignature) throws IOException {
		List<EntrySpec> entries = List.of(
				new EntrySpec("descriptor.txt", "descriptor-data".getBytes(StandardCharsets.UTF_8),
						true, includeDescriptorSignature)
		);
		return createArchive(entries, false);
	}

	@Nonnull
	static byte[] splitZip64Archive() throws IOException {
		List<EntrySpec> entries = List.of(
				new EntrySpec("split.txt", "split-archive".getBytes(StandardCharsets.UTF_8), false, false)
		);
		return createArchive(entries, true);
	}

	@Nonnull
	private static byte[] createArchive(@Nonnull List<EntrySpec> entries, boolean splitArchive) throws IOException {
		ByteArrayOutputStream localOut = new ByteArrayOutputStream();
		List<EntryMeta> metas = new ArrayList<>();
		for (EntrySpec entry : entries) {
			byte[] name = entry.name().getBytes(StandardCharsets.UTF_8);
			long localOffset = localOut.size();
			long crc32 = crc32(entry.data());
			byte[] localExtra = localZip64Extra(entry.data().length);
			int generalPurposeBitFlag = entry.useDataDescriptor() ? GP_FLAG_DATA_DESCRIPTOR : 0;

			writeIntLE(localOut, 0x04034B50);
			writeShortLE(localOut, VERSION_ZIP64);
			writeShortLE(localOut, generalPurposeBitFlag);
			writeShortLE(localOut, 0);
			writeShortLE(localOut, 0);
			writeShortLE(localOut, 0);
			writeIntLE(localOut, entry.useDataDescriptor() ? 0L : crc32);
			writeIntLE(localOut, ZIP64_MAGIC_DWORD);
			writeIntLE(localOut, ZIP64_MAGIC_DWORD);
			writeShortLE(localOut, name.length);
			writeShortLE(localOut, localExtra.length);
			localOut.write(name);
			localOut.write(localExtra);
			localOut.write(entry.data());
			if (entry.useDataDescriptor()) {
				if (entry.includeDescriptorSignature())
					writeIntLE(localOut, 0x08074B50);
				writeIntLE(localOut, crc32);
				writeLongLE(localOut, entry.data().length);
				writeLongLE(localOut, entry.data().length);
			}

			metas.add(new EntryMeta(entry, name, localOffset, crc32));
		}

		long centralDirectoryOffset = localOut.size();
		ByteArrayOutputStream centralOut = new ByteArrayOutputStream();
		for (EntryMeta meta : metas) {
			byte[] centralExtra = centralZip64Extra(meta.entry().data().length, meta.localOffset());
			writeIntLE(centralOut, 0x02014B50);
			writeShortLE(centralOut, VERSION_ZIP64);
			writeShortLE(centralOut, VERSION_ZIP64);
			writeShortLE(centralOut, meta.entry().useDataDescriptor() ? GP_FLAG_DATA_DESCRIPTOR : 0);
			writeShortLE(centralOut, 0);
			writeShortLE(centralOut, 0);
			writeShortLE(centralOut, 0);
			writeIntLE(centralOut, meta.crc32());
			writeIntLE(centralOut, ZIP64_MAGIC_DWORD);
			writeIntLE(centralOut, ZIP64_MAGIC_DWORD);
			writeShortLE(centralOut, meta.name().length);
			writeShortLE(centralOut, centralExtra.length);
			writeShortLE(centralOut, 0);
			writeShortLE(centralOut, 0);
			writeShortLE(centralOut, 0);
			writeIntLE(centralOut, 0);
			writeIntLE(centralOut, ZIP64_MAGIC_DWORD);
			centralOut.write(meta.name());
			centralOut.write(centralExtra);
		}

		long centralDirectorySize = centralOut.size();
		localOut.write(centralOut.toByteArray());
		long zip64EndOffset = localOut.size();

		writeIntLE(localOut, 0x06064B50);
		writeLongLE(localOut, 44L);
		writeShortLE(localOut, VERSION_ZIP64);
		writeShortLE(localOut, VERSION_ZIP64);
		writeIntLE(localOut, splitArchive ? 1L : 0L);
		writeIntLE(localOut, splitArchive ? 1L : 0L);
		writeLongLE(localOut, metas.size());
		writeLongLE(localOut, metas.size());
		writeLongLE(localOut, centralDirectorySize);
		writeLongLE(localOut, centralDirectoryOffset);

		writeIntLE(localOut, 0x07064B50);
		writeIntLE(localOut, splitArchive ? 1L : 0L);
		writeLongLE(localOut, zip64EndOffset);
		writeIntLE(localOut, splitArchive ? 2L : 1L);

		writeIntLE(localOut, 0x06054B50);
		writeShortLE(localOut, splitArchive ? 1 : 0);
		writeShortLE(localOut, splitArchive ? 1 : 0);
		writeShortLE(localOut, 0xFFFF);
		writeShortLE(localOut, 0xFFFF);
		writeIntLE(localOut, ZIP64_MAGIC_DWORD);
		writeIntLE(localOut, ZIP64_MAGIC_DWORD);
		writeShortLE(localOut, 0);

		return localOut.toByteArray();
	}

	@Nonnull
	private static byte[] localZip64Extra(long size) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeShortLE(out, 0x0001);
		writeShortLE(out, 16);
		writeLongLE(out, size);
		writeLongLE(out, size);
		return out.toByteArray();
	}

	@Nonnull
	private static byte[] centralZip64Extra(long size, long localOffset) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeShortLE(out, 0x0001);
		writeShortLE(out, 24);
		writeLongLE(out, size);
		writeLongLE(out, size);
		writeLongLE(out, localOffset);
		return out.toByteArray();
	}

	private static long crc32(byte[] data) {
		CRC32 crc32 = new CRC32();
		crc32.update(data);
		return crc32.getValue();
	}

	private static void writeShortLE(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >>> 8) & 0xFF);
	}

	private static void writeIntLE(ByteArrayOutputStream out, long value) {
		out.write((int) (value & 0xFF));
		out.write((int) ((value >>> 8) & 0xFF));
		out.write((int) ((value >>> 16) & 0xFF));
		out.write((int) ((value >>> 24) & 0xFF));
	}

	private static void writeLongLE(ByteArrayOutputStream out, long value) {
		writeIntLE(out, value);
		writeIntLE(out, value >>> 32);
	}

	@FunctionalInterface
	public interface ThrowingConsumer<T> {
		void accept(T value) throws IOException;
	}

	private record EntrySpec(@Nonnull String name, @Nonnull byte[] data,
	                         boolean useDataDescriptor, boolean includeDescriptorSignature) {
	}

	private record EntryMeta(@Nonnull EntrySpec entry, @Nonnull byte[] name, long localOffset, long crc32) {
	}
}
