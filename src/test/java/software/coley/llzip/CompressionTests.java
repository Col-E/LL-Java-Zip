package software.coley.llzip;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.strategy.Decompressor;
import software.coley.llzip.strategy.DeflateDecompressor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
			byte[] decompressed = localFileHeader.decompress(new DeflateDecompressor());
			ClassReader cr = new ClassReader(decompressed);
			assertEquals("Hello", cr.getClassName());
			cr.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					return new MethodVisitor(Opcodes.ASM9) {
						@Override
						public void visitLdcInsn(Object value) {
							assertEquals("Hello world!", value);
						}
					};
				}
			}, ClassReader.SKIP_FRAMES);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	public void testDeflateJvmJar() {
		try {
			byte[] data = Files.readAllBytes(Paths.get("src/test/resources/hello-trick.jar"));
			ZipArchive zip = ZipIO.readJvm(data);
			for (LocalFileHeader localFileHeader : zip.getLocalFiles()) {
				if (!localFileHeader.getFileName().endsWith(".class"))
					continue;
				assertEquals("Hello.class", localFileHeader.getFileName());
				byte[] decompressed = localFileHeader.decompress(new DeflateDecompressor());
				ClassReader cr = new ClassReader(decompressed);
				assertEquals("Hello", cr.getClassName());
				cr.accept(new ClassVisitor(Opcodes.ASM9) {
					@Override
					public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
						return new MethodVisitor(Opcodes.ASM9) {
							@Override
							public void visitLdcInsn(Object value) {
								assertEquals("Hello world!", value);
							}
						};
					}
				}, ClassReader.SKIP_FRAMES);
			}
		} catch (IOException ex) {
			fail(ex);
		}
	}
}
