package software.coley.lljzip;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.MemorySegmentUtil;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.function.Supplier;

/**
 * Utilities for zip tests where there are embedded zip files.
 *
 * @author Matt Coley
 */
public class JarInJarUtils {
	/**
	 * @param archiveSupplier
	 * 		Supplies a zip (jar) archive.
	 *
	 * @throws IOException
	 * 		When the zip cannot be read.
	 */
	public static void handleJar(Supplier<ZipArchive> archiveSupplier) throws IOException {
		ZipArchive zipJvm = archiveSupplier.get();
		for (LocalFileHeader lfh : zipJvm.getLocalFiles()) {
			String entryName = lfh.getFileNameAsString();
			if (entryName.endsWith(".class")) {
				// We can verify the correctness of our zip model offsets and compression
				// by parsing and writing back the class files contained in the jar.
				// If anything is wrong, this process should fail.
				handleClass(entryName, lfh);
			} else if (entryName.endsWith(".jar")) {
				// We should be able to extract contents in the jar in-memory and make the same assumptions
				// as we do on the root (that classes can be parsed)
				handleJar(() -> {
					try {
						MemorySegment decompressed = ZipCompressions.decompress(lfh);
						return ZipIO.readStandard(decompressed);
					} catch (IOException ex) {
						throw new IllegalStateException("Failed to read inner jar: " + entryName, ex);
					}
				});
			}
		}
	}

	/**
	 * Attempts to parse the class file, and write it back. If there's an encoding/compression issue, this process fails.
	 *
	 * @param localFileHeader
	 * 		Local file header of class.
	 *
	 * @throws IOException
	 * 		When the class couldn't be parsed.
	 */
	public static void handleClass(String name, LocalFileHeader localFileHeader) throws IOException {
		byte[] entryData = MemorySegmentUtil.toByteArray(ZipCompressions.decompress(localFileHeader));
		try {
			ClassReader reader = new ClassReader(entryData);
			reader.accept(new ClassWriter(0), 0);
		} catch (Throwable ex) {
			throw new IOException("Failed to parse class: " + name, ex);
		}
	}
}
