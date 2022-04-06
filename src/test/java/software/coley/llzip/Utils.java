package software.coley.llzip;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.llzip.util.Buffers;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilities for zip tests.
 *
 * @author Matt Coley
 */
public class Utils {
	/**
	 * Asserts the string has been found and is the <b>ONLY</b> matching string in the class.
	 *
	 * @param code
	 * 		Class bytecode.
	 * @param target
	 * 		String instance to look for.
	 */
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

	/**
	 * Asserts the string has been found and is the <b>ONLY</b> matching string in the class.
	 *
	 * @param code
	 * 		Class bytecode.
	 * @param target
	 * 		String instance to look for.
	 */
	public static void assertDefinesString(ByteBuffer code, String target) {
		assertDefinesString(Buffers.toByteArray(code), target);
	}
}
