package software.coley.lljzip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.model.ZipPart;
import software.coley.lljzip.format.read.ForwardScanZipReader;
import software.coley.lljzip.util.ByteDataUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
			assertFalse(stdHelloRaw.isEmpty());
			assertFalse(jvmHelloRaw.isEmpty());
			assertTrue(stdHelloRaw.contains("Hello world"));
			assertTrue(jvmHelloRaw.contains("The secret code is: ROSE"));
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"hello-concat.jar",
			"hello-concat-junkheader.jar",
			"hello-merged.jar",
			"hello-merged-fake-empty.jar",
			"hello-merged-junkheader.jar",
			"hello-secret-0-length-locals.jar",
			"hello-secret-junkheader.jar",
			"hello-secret-trailing-slash.jar",
			"hello-secret-trailing-slash-0-length-locals.jar",
	})
	public void testJvmCanRecoverData(String name) {
		try {
			Path path = Paths.get("src/test/resources/" + name);
			ZipArchive zip = ZipIO.readJvm(path);
			List<LocalFileHeader> localFiles = zip.getNameFilteredLocalFiles(n -> n.contains(".class"));
			assertEquals(1, localFiles.size(), "More than 1 class");
			byte[] decompressed = ByteDataUtil.toByteArray(ZipCompressions.decompress(localFiles.get(0)));
			assertDoesNotThrow(() -> {
				ClassWriter cw = new ClassWriter(0);
				ClassReader cr = new ClassReader(decompressed);
				cr.accept(cw, 0);
			}, "Failed to read class, must have failed to decompress");
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testLocalHeaderDetectMismatch() {
		Path path = Paths.get("src/test/resources/hello-secret-0-length-locals.jar");

		try {
			// The 'standard' strategy does not adopt CEN values when reading local entries.
			// The 'jvm' strategy does.
			ZipArchive zipStd = ZipIO.readStandard(path);
			assertNotNull(zipStd);

			LocalFileHeader hello = zipStd.getLocalFileByName("Hello.class");
			assertNotNull(hello);
			assertEquals(0, hello.getFileData().length()); // Should be empty

			// The local file header says the contents are 0 bytes, but the central header has the real length
			assertTrue(hello.hasDifferentValuesThanCentralDirectoryHeader());

			// The solution to differing values is to adopt values in the reader strategy
			ZipArchive zipStdAndAdopt = ZipIO.read(path, new ForwardScanZipReader() {
				@Override
				public void postProcessLocalFileHeader(@Nonnull LocalFileHeader file) {
					file.adoptLinkedCentralDirectoryValues();
				}
			});
			LocalFileHeader helloAdopted = zipStdAndAdopt.getLocalFileByName("Hello.class");
			assertFalse(helloAdopted.hasDifferentValuesThanCentralDirectoryHeader());
			assertNotEquals(0, helloAdopted.getFileData().length()); // Should have data

			// The JVM strategy copies most properties, except for size.
			ZipArchive zipJvm = ZipIO.readJvm(path);
			helloAdopted = zipJvm.getLocalFileByName("Hello.class");
			assertNotEquals(0, helloAdopted.getFileData().length()); // Should have data, even if not sourced from values in the CEN
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
