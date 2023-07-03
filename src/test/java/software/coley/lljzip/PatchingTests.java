package software.coley.lljzip;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.write.ZipOutputStreamZipWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for patching the <i>"trick"</i> jars, and making them compatible with standard Java ZIP apis.
 *
 * @author Matt Coley
 */
public class PatchingTests {
	// The sample 'hello-merged-fake-empty.jar' is not included because its CEN points to the correct local entry.
	// Unlike the samples seen below, it only has one END header, not two.
	@ParameterizedTest
	@ValueSource(strings = {
			"hello-concat.jar",
			"hello-concat-junkheader.jar",
			"hello-merged.jar",
			"hello-merged-junkheader.jar",
	})
	public void testTrickJarPatched(String name) {
		try {
			Path path = Paths.get("src/test/resources/" + name);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			// Validate the original jar file yields the wrong 'Hello' class
			try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(path))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().contains("Hello.class")) {
						byte[] buffer = new byte[1024];
						baos = new ByteArrayOutputStream();
						int len = 0;
						while ((len = zis.read(buffer)) > 0)
							baos.write(buffer, 0, len);

						// The version of 'Hello' read does not contain the right message.
						// Running these jars produces the other message, seen below after patching.
						Utils.assertDefinesString(baos.toByteArray(), "Hello world!");
					}
				}
			}

			// Parse the zip with LL-Java zip, then write back using std java apis
			// in order to create a std java complaint jar.
			ZipArchive zip = ZipIO.readJvm(path);
			new ZipOutputStreamZipWriter().write(zip, baos);
			byte[] fixed = baos.toByteArray();

			// Validate the new jar bytes can be read and show the true file contents.
			try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fixed))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().contains("Hello.class")) {
						byte[] buffer = new byte[1024];
						baos = new ByteArrayOutputStream();
						int len = 0;
						while ((len = zis.read(buffer)) > 0)
							baos.write(buffer, 0, len);

						// Now check if the secret code is found.
						// If not, we extracted the wrong class.
						Utils.assertDefinesString(baos.toByteArray(), "The secret code is: ROSE");
					}
				}
			}
		} catch (IOException ex) {
			fail(ex);
		}
	}
}
