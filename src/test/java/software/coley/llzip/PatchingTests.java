package software.coley.llzip;

import org.junit.jupiter.api.Test;
import software.coley.llzip.strategy.JavaZipWriterStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
	@Test
	public void testTrickJarPatched() {
		try {
			// Parse the zip with LL-Java zip, then write back using std java apis
			// in order to create a std java complaint jar.
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ZipArchive zip = ZipIO.readJvm(Paths.get("src/test/resources/hello-trick-garbagehead.jar"));
			new JavaZipWriterStrategy().write(zip, baos);
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
