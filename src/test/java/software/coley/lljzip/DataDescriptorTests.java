package software.coley.lljzip;

import org.junit.jupiter.api.Test;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.ForwardScanZipReader;
import software.coley.lljzip.format.read.JvmZipReader;
import software.coley.lljzip.format.transform.CentralAdoptingMapper;
import software.coley.lljzip.format.transform.IdentityZipPartMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static software.coley.lljzip.JarInJarUtils.handleJar;

/**
 * Parse tests ensuring {@link ForwardScanZipReader} and {@link JvmZipReader} correctly
 * handle ZIP's with data-descriptors attached to {@link LocalFileHeader} entries.
 *
 * @author Matt Coley
 */
@SuppressWarnings("resource")
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
			try {
				ZipArchive archive = ZipIO.readStandard(path);
				return archive.withMapping(new CentralAdoptingMapper(new IdentityZipPartMapper()));
			} catch (IOException ex) {
				fail(ex);
				return null;
			}
		}), "Failed to read with read(standard) + mapping(central-adoption)");
	}
}
