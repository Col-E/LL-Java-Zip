package software.coley.lljzip;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.ForwardScanZipReader;
import software.coley.lljzip.format.read.JvmZipReader;
import software.coley.lljzip.format.transform.CentralAdoptingMapper;
import software.coley.lljzip.format.transform.IdentityZipPartMapper;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.ByteDataUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Parse tests ensuring {@link ForwardScanZipReader} and {@link JvmZipReader} correctly
 * handle ZIP's with data-descriptors attached to {@link LocalFileHeader} entries.
 *
 * @author Matt Coley
 */
public class DataDescriptorTests {
	@Test
	public void validity() {
		// We're going to read a file that utilizes data-descriptors.
		// This is an optional element outlined in section 4.3.9 of the ZIP spec
		// which is present AFTER the file data.
		Path path = Paths.get("src/test/resources/jar-in-jar-with-data-descriptor.jar");

		// The JVM strategy calculates file data ranges by mapping from the start
		// of the current expected data offset of a local file header, to the start of
		// the next local file header. If there is a data-descriptor, the length needs to
		// accommodate for that and cut off the 16 bytes used by the data-descriptor.
		assertDoesNotThrow(() -> handleJar(() -> {
			try {
				return ZipIO.readJvm(path);
			} catch (IOException ex) {
				fail(ex);
				return null;
			}
		}), "Failed to read with read(jvm)");

		// The standard strategy calculates file data ranges by using the compressed size.
		// The value is assumed to be correct. We'll want to use the authoritative values from
		// the central directory file header though. The local sizes can be bogus.
		//
		// Because the sizes should themselves accommodate for the data-descriptor being present
		// we do not need any special handling like we do in the JVM local file parser logic.
		assertDoesNotThrow(() -> handleJar(() -> {
			try (ZipArchive archive = ZipIO.readStandard(path)) {
				return archive.withMapping(new CentralAdoptingMapper(new IdentityZipPartMapper()));
			} catch (IOException ex) {
				fail(ex);
				return null;
			}
		}), "Failed to read with read(standard) + mapping(central-adoption)");
	}

	/**
	 * @param archiveSupplier
	 * 		Supplies a zip (jar) archive.
	 *
	 * @throws IOException
	 * 		When the zip cannot be read.
	 */
	private static void handleJar(Supplier<ZipArchive> archiveSupplier) throws IOException {
		try (ZipArchive zipJvm = archiveSupplier.get()) {
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
							ByteData decompressed = ZipCompressions.decompress(lfh);
							return ZipIO.readStandard(decompressed);
						} catch (IOException ex) {
							throw new IllegalStateException("Failed to read inner jar: " + entryName, ex);
						}
					});
				}
			}
		}
	}

	/**
	 * @param localFileHeader
	 * 		Local file header of class.
	 *
	 * @throws IOException
	 * 		When the class couldn't be parsed.
	 */
	private static void handleClass(String name, LocalFileHeader localFileHeader) throws IOException {
		byte[] entryData = ByteDataUtil.toByteArray(ZipCompressions.decompress(localFileHeader));
		try {
			ClassReader reader = new ClassReader(entryData);
			reader.accept(new ClassWriter(0), 0);
		} catch (Throwable ex) {
			throw new IOException("Failed to parse class: " + name, ex);
		}
	}
}
