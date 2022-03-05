package software.coley.llzip;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.llzip.part.CentralDirectoryFileHeader;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.strategy.Decompressor;
import software.coley.llzip.strategy.DeflateDecompressor;
import software.coley.llzip.strategy.JvmZipReaderStrategy;

import java.io.IOException;
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
			byte[] data = Files.readAllBytes(Paths.get("src/test/resources/hello.jar"));
			ZipArchive zip = ZipIO.readStandard(data);
			LocalFileHeader localFileHeader = zip.getLocalFiles().get(0);
			assertEquals("Hello.class", localFileHeader.getFileName());
			byte[] classData = localFileHeader.decompress(new DeflateDecompressor());
			assertDefinesString(classData, "Hello world!");
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testDeflateJvmJar() {
		try {
			byte[] data = Files.readAllBytes(Paths.get("src/test/resources/hello-trick.jar"));
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
			ZipArchive zip = ZipIO.readJvm(data);
			// The red herring class that most zip tools see
			CentralDirectoryFileHeader redHerringCentralDir = zip.getCentralDirectories().get(0);
			assertEquals("Hello.class", redHerringCentralDir.getFileName());
			assertNull( redHerringCentralDir.getLinked(), "The red herring central directory got linked");
			byte[] redHerringClassData = zip.getLocalFiles().get(1).decompress(new DeflateDecompressor());
			assertDefinesString(redHerringClassData, "Hello world!");
			// The real class that gets run by the JVM
			CentralDirectoryFileHeader jvmCentralDir = zip.getCentralDirectories().get(3);
			assertEquals("Hello.class/", jvmCentralDir.getFileName());
			assertNotEquals("Hello.class/", jvmCentralDir.getLinked().getFileName());
			byte[] classData = jvmCentralDir.getLinked().decompress(new DeflateDecompressor());
			assertDefinesString(classData, "The secret code is: ROSE");
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testDeflateJvmJarWithGarbageHeader() {
		try {
			// The trick jar is similar to the above, but with extra garbage at the beginning of the file.
			// This test shows that garbage bytes in the beginning of the file can be bypassed.
			byte[] data = Files.readAllBytes(Paths.get("src/test/resources/hello-trick-garbagehead.jar"));
			ZipArchive zip = ZipIO.readJvm(data);
			// The red herring class that most zip tools see
			CentralDirectoryFileHeader redHerringCentralDir = zip.getCentralDirectories().get(1);
			assertEquals("Hello\t.class", redHerringCentralDir.getFileName());
			byte[] redHerringClassData = redHerringCentralDir.getLinked().decompress(new DeflateDecompressor());
			assertDefinesString(redHerringClassData, "Hello world!");
			// The real class that gets run by the JVM
			CentralDirectoryFileHeader jvmCentralDir = zip.getCentralDirectories().get(0);
			assertEquals("Hello.class/", jvmCentralDir.getFileName());
			assertNotEquals("Hello.class/", jvmCentralDir.getLinked().getFileName());
			byte[] classData = jvmCentralDir.getLinked().decompress(new DeflateDecompressor());
			assertDefinesString(classData, "The secret code is: ROSE");
		} catch (IOException ex) {
			fail(ex);
		}
	}



	public static void assertDefinesString(byte[] code, String target) {
		boolean[] visited = new boolean[1];
		ClassReader cr = new ClassReader(code);
		cr.accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitLdcInsn(Object value) {
						visited[0] = true;
						assertEquals(target, value);
					}
				};
			}
		}, ClassReader.SKIP_FRAMES);
		assertTrue(visited[0], "The entry did not visit any LDC constants");
	}
}
