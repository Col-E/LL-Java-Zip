package software.coley.lljzip;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.transform.CentralAdoptingMapper;
import software.coley.lljzip.format.transform.IdentityZipPartMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static software.coley.lljzip.JarInJarUtils.handleJar;

/**
 * Similar to {@link DataDescriptorTests} but just checking for general handling of jar-in-jar embeddings.
 *
 * @author Matt Coley
 */
@SuppressWarnings("resource")
public class JarInJarTests {
	@ParameterizedTest
	@ValueSource(strings = {
			"hello-copyjar-at-head.jar",
			"hello-copyjar-at-tail.jar",
			"hello-jar-in-in-jar-in-jar-in-jar-in-jar.jar",
			"jar-in-jar-with-data-descriptor.jar",
	})
	public void test(String name) {
		Path path = Paths.get("src/test/resources/" + name);

		// The JVM strategy calculates file data ranges by mapping from the start
		// of the current expected data offset of a local file header, to the start of
		// the next local file header.
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
