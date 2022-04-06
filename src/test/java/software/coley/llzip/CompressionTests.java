package software.coley.llzip;

import org.junit.jupiter.api.Test;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.strategy.Decompressor;
import software.coley.llzip.strategy.DeflateDecompressor;
import software.coley.llzip.util.Buffers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LocalFileHeader#decompress(Decompressor)}.
 *
 * @author Matt Coley
 */
public class CompressionTests {
	@Test
	public void testDeflateStandardJar() {
		try {
			ZipArchive zip = ZipIO.readStandard(Paths.get("src/test/resources/hello.jar"));
			LocalFileHeader localFileHeader = zip.getLocalFiles().get(0);
			assertEquals("Hello.class", Buffers.toString(localFileHeader.getFileName()));
			ByteBuffer classData = localFileHeader.decompress(new DeflateDecompressor());
			Utils.assertDefinesString(classData, "Hello world!");
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testDeflateJvmJar() {
		try {
			// When parsed there should be these sections in order
			/////// MISSING: LocalFileHeader - Not read by JVM
			/////// MISSING: LocalFileHeader - Not read by JVM
			/////// MISSING: LocalFileHeader - Not read by JVM
			// 0 = {CentralDirectoryFileHeader} offset=642
			// 1 = {CentralDirectoryFileHeader} offset=735
			// 2 = {CentralDirectoryFileHeader} offset=826
			/////// MISSING: EndOfCentralDirectory - Not read by JVM <--- This is where most tools read from, but not the JVM!
			// 3 = {LocalFileHeader}            offset=950  <--- name=<blank>, message=The secret code is: ROSE
			// 4 = {LocalFileHeader}            offset=1243 <--- name=<blank>, message=Hello world!
			// 5 = {LocalFileHeader}            offset=1523
			// 6 = {CentralDirectoryFileHeader} offset=1592 <--- name="Hello.class/" file=ROSE
			// 7 = {CentralDirectoryFileHeader} offset=1650 <--- name="Hello.class " file=Hello world!
			// 8 = {CentralDirectoryFileHeader} offset=1708
			// 9 = {EndOfCentralDirectory}      offset=1774
			ZipArchive zip = ZipIO.readJvm(Paths.get("src/test/resources/hello-trick.jar"));
			// The red herring class that most zip tools see
			CentralDirectoryFileHeader redHerringCentralDir = zip.getCentralDirectories().get(0);
			assertEquals("Hello.class", redHerringCentralDir.getFileNameAsString());
			assertNull( redHerringCentralDir.getLinkedFileHeader(), "The red herring central directory got linked");
			ByteBuffer redHerringClassData = zip.getLocalFiles().get(1).decompress(new DeflateDecompressor());
			Utils.assertDefinesString(redHerringClassData, "Hello world!");
			// The real class that gets run by the JVM
			CentralDirectoryFileHeader jvmCentralDir = zip.getCentralDirectories().get(3);
			assertEquals("Hello.class/", jvmCentralDir.getFileNameAsString());
			assertNotEquals("Hello.class/", jvmCentralDir.getLinkedFileHeader().getFileName());
			ByteBuffer classData = jvmCentralDir.getLinkedFileHeader().decompress(new DeflateDecompressor());
			Utils.assertDefinesString(classData, "The secret code is: ROSE");
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testDeflateJvmJarWithGarbageHeader() {
		try {
			// The trick jar is similar to the above, but with extra garbage at the beginning of the file.
			// This test shows that garbage bytes in the beginning of the file can be bypassed.
			ZipArchive zip = ZipIO.readJvm(Paths.get("src/test/resources/hello-trick-garbagehead.jar"));
			// The red herring class that most zip tools see
			CentralDirectoryFileHeader redHerringCentralDir = zip.getCentralDirectories().get(1);
			assertEquals("Hello\t.class", redHerringCentralDir.getFileNameAsString());
			ByteBuffer redHerringClassData = redHerringCentralDir.getLinkedFileHeader().decompress(new DeflateDecompressor());
			Utils.assertDefinesString(redHerringClassData, "Hello world!");
			// The real class that gets run by the JVM
			CentralDirectoryFileHeader jvmCentralDir = zip.getCentralDirectories().get(0);
			assertEquals("Hello.class/", jvmCentralDir.getFileNameAsString());
			assertNotEquals("Hello.class/", jvmCentralDir.getLinkedFileHeader().getFileName());
			ByteBuffer classData = jvmCentralDir.getLinkedFileHeader().decompress(new DeflateDecompressor());
			Utils.assertDefinesString(classData, "The secret code is: ROSE");
		} catch (IOException ex) {
			fail(ex);
		}
	}
}
