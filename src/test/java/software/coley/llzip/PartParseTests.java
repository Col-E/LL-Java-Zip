package software.coley.llzip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.llzip.format.compression.ZipCompressions;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.format.model.ZipPart;
import software.coley.llzip.format.read.JvmZipReaderStrategy;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.nio.file.Path;
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
			"src/test/resources/sample-code-7z.zip", // ZIP made from 7z
			"src/test/resources/sample-code-windows.zip", // ZIP made from windows built in 'send to zip'
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

	@ParameterizedTest
	@ValueSource(strings = {
			"hello.jar",
			"hello-secret.jar",
			"hello-secret-0-length-locals.jar",
			"hello-secret-junkheader.jar",
	})
	public void testHello(String name) {
		try {
			Path data = Paths.get("src/test/resources/" + name);
			ZipArchive zipStd = ZipIO.readStandard(data);
			ZipArchive zipJvm = ZipIO.readJvm(data);
			assertNotNull(zipStd);
			assertNotNull(zipJvm);
			assertEquals(zipJvm, zipJvm);

			// The 'hello' jars has a manifest and single class to run itself when invoked via 'java -jar'
			assertTrue(hasFile(zipStd, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zipStd, "Hello.class"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"hello-concat.jar",
			"hello-concat-junkheader.jar",
			"hello-merged.jar",
			"hello-merged-junkheader.jar",
	})
	public void testConcatAndMerged(String name) {
		try {
			Path path = Paths.get("src/test/resources/" + name);
			ZipArchive zipStd = ZipIO.readStandard(path);
			ZipArchive zipJvm = ZipIO.readJvm(path);
			assertNotNull(zipStd);
			assertNotNull(zipJvm);
			assertNotEquals(zipJvm, zipStd);
			assertNotEquals(zipJvm.getEnd(), zipStd.getEnd());
			assertTrue(hasFile(zipJvm, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zipJvm, "Hello.class"));

			// Assert that the standard ZIP reader read the 'first' version of the class
			// and the JVM reader read the 'second' version of the class.
			LocalFileHeader stdHello = zipStd.getLocalFileByName("Hello.class");
			LocalFileHeader jvmHello = zipJvm.getLocalFileByName("Hello.class");
			assertNotEquals(stdHello, jvmHello);
			String stdHelloRaw = ByteDataUtil.toString(ZipCompressions.decompress(stdHello));
			String jvmHelloRaw = ByteDataUtil.toString(ZipCompressions.decompress(jvmHello));
			assertTrue(stdHelloRaw.contains("Hello world"));
			assertTrue(jvmHelloRaw.contains("The secret code is: ROSE"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testLocalHeaderDetectMismatch() {
		Path path = Paths.get("src/test/resources/hello-secret-0-length-locals.jar");

		try {
			ZipArchive zipJvm = ZipIO.readJvm(path);
			assertNotNull(zipJvm);

			LocalFileHeader hello = zipJvm.getLocalFileByName("Hello.class");
			assertNotNull(hello);

			// The local file header says the contents are 0 bytes, but the central header has the real length
			assertTrue(hello.hasDifferentValuesThanCentralDirectoryHeader());

			// The solution to differing values is to adopt values in the reader strategy
			ZipArchive zipJvmAndAdopt = ZipIO.read(path, new JvmZipReaderStrategy() {
				@Override
				public void postProcessLocalFileHeader(LocalFileHeader file) {
					file.adoptLinkedCentralDirectoryValues();
				}
			});
			LocalFileHeader helloAdopted = zipJvmAndAdopt.getLocalFileByName("Hello.class");
			assertFalse(helloAdopted.hasDifferentValuesThanCentralDirectoryHeader());
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testMergedFakeEmpty() {
		try (ZipArchive zipJvm = ZipIO.readJvm(Paths.get("src/test/resources/hello-merged-fake-empty.jar"))) {
			assertNotNull(zipJvm);
			assertTrue(hasFile(zipJvm, "META-INF/MANIFEST.MF"));
			assertTrue(hasFile(zipJvm, "Hello.class/")); // has trailing slash in class name
		} catch (IOException ex) {
			fail(ex);
		}
	}

	private static boolean hasFile(ZipArchive zip, String name) {
		return !zip.getNameFilteredLocalFiles(name::equals).isEmpty();
	}
}
