package software.coley.llzip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.llzip.part.ZipPart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parse tests for {@link ZipArchive}.
 * Ensures all the {@link ZipPart}s of the archive are read as expected.
 *
 * @author Matt Coley
 */
public class PartParseTests {
	@ParameterizedTest
	@ValueSource(strings = {
			"src/test/resources/code-windows.zip",
			"src/test/resources/code-7z.zip",
	})
	public void testStandardCodeZip(String path) {
		try {
			byte[] data = Files.readAllBytes(Paths.get(path));
			ZipArchive zip = ZipIO.readStandard(data);
			assertNotNull(zip);
			// Each code zip contains these files
			assertTrue(hasFile(zip, "ClassFile.java"));
			assertTrue(hasFile(zip, "ClassMember.java"));
			assertTrue(hasFile(zip, "ConstPool.java"));
			assertTrue(hasFile(zip, "Descriptor.java"));
			assertTrue(hasFile(zip, "Field.java"));
			assertTrue(hasFile(zip, "Method.java"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testStandardJar() {
		try {
			byte[] data = Files.readAllBytes(Paths.get("src/test/resources/hello.jar"));
			ZipArchive zip = ZipIO.readStandard(data);
			assertNotNull(zip);
			// The 'hello' jar has a manifest and single class to run itself when invoked via 'java -jar'
			assertTrue(hasFile(zip, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zip, "Hello.class"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testJvmJar() {
		try {
			byte[] data = Files.readAllBytes(Paths.get("src/test/resources/hello-trick.jar"));
			ZipArchive zip = ZipIO.readJvm(data);
			assertNotNull(zip);
			// The 'hello' jar has a manifest and single class to run itself when invoked via 'java -jar'
			assertTrue(hasFile(zip, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zip, "Hello.class"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	private static boolean hasFile(ZipArchive zip, String name) {
		return zip.getCentralDirectories().stream()
				.anyMatch(cdfh -> cdfh.getLinked() != null &&
						cdfh.getFileName().equals(name) &&
						cdfh.getLinked().getFileName().equals(name));
	}
}
