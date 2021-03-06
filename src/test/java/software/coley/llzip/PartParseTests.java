package software.coley.llzip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.llzip.part.ZipPart;

import java.io.IOException;
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
			ZipArchive zip = ZipIO.readStandard(Paths.get(path));
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
			ZipArchive zip = ZipIO.readStandard(Paths.get("src/test/resources/hello.jar"));
			assertNotNull(zip);
			// The 'hello' jar has a manifest and single class to run itself when invoked via 'java -jar'
			assertTrue(hasFile(zip, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zip, "Hello.class"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testJvmStandardJar() {
		try {
			// Even with edge case parsing being the focus, the JVM reader should be able to handle this jar fine.
			ZipArchive zip = ZipIO.readJvm(Paths.get("src/test/resources/hello.jar"));
			assertNotNull(zip);
			// The 'hello' jar has a manifest and single class to run itself when invoked via 'java -jar'
			assertTrue(hasFile(zip, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zip, "Hello.class"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testJvmTrickJar() {
		try {
			ZipArchive zip = ZipIO.readJvm(Paths.get("src/test/resources/hello-trick.jar"));
			assertNotNull(zip);
			// The 'hello' jar has a manifest and single class to run itself when invoked via 'java -jar'
			assertTrue(hasFile(zip, "META-INF/MANIFEST.MF"));
			// There are two classes with deceiving names in the trick jar
			//  - The central directory names are authoritative in Java.
			//  - The local file names are ignored, so they can be anything, even `\0`
			assertTrue(hasFile(zip, "Hello.class/"));
			assertTrue(hasFile(zip, "Hello.class\1"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	private static boolean hasFile(ZipArchive zip, String name) {
		return zip.getCentralDirectories().stream()
				.anyMatch(cdfh -> cdfh.getLinkedFileHeader() != null &&
						cdfh.getFileNameAsString().equals(name));
	}
}
