package software.coley.llzip.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Maps large files into memory.
 *
 * @author xDark
 */
public class FileMapUtil {
	private static final Method MAP;
	private static final Method UNMAP;
	private static final boolean OLD_MAP;

	/**
	 * Maps file into memory.
	 *
	 * @param path
	 * 		Path to a file to map.
	 *
	 * @return Mapped file.
	 *
	 * @throws IOException
	 * 		If any I/O error occurs.
	 */
	public static ByteData map(Path path) throws IOException {
		try (FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
			long length = fc.size();
			long address;
			try {
				if (OLD_MAP) {
					address = (long) MAP.invoke(fc, 0, 0L, length);
				} else {
					address = (long) MAP.invoke(fc, 0, 0L, length, false);
				}
			} catch (InvocationTargetException | IllegalAccessException ex) {
				throw new IllegalStateException("Could not invoke map0", ex);
			}
			ByteData mappedFile = new UnsafeMappedFile(address, length, () -> {
				try {
					UNMAP.invoke(null, address, length);
				} catch (IllegalAccessException | InvocationTargetException ex) {
					throw new InternalError(ex);
				}
			});
			return mappedFile;
		}
	}

	static {
		boolean oldMap = false;
		try {
			Class<?> c = Class.forName("sun.nio.ch.FileChannelImpl");
			Method map;
			try {
				map = c.getDeclaredMethod("map0", int.class, long.class, long.class, boolean.class);
			} catch (NoSuchMethodException ex) {
				map = c.getDeclaredMethod("map0", int.class, long.class, long.class);
				oldMap = true;
			}
			map.setAccessible(true);
			Method unmap = c.getDeclaredMethod("unmap0", long.class, long.class);
			unmap.setAccessible(true);
			MAP = map;
			UNMAP = unmap;
			OLD_MAP = oldMap;
		} catch (ClassNotFoundException | NoSuchMethodException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}
}
