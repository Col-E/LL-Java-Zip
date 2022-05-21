package software.coley.llzip.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
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
	 * @throws IllegalStateException
	 * 		If the environment is locked up and
	 * 		file is larger than 2GB.
	 */
	public static ByteData map(Path path) throws IOException {
		if (MAP == null) {
			long size = Files.size(path);
			if (size <= Integer.MAX_VALUE) {
				try(FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
					long length = fc.size();
					MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0L, length);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					return BufferData.wrap(buffer);
				}
			}
			throw new IllegalStateException("Cannot map more than 2GB of data in locked up environment");
		}
		try(FileChannel fc = FileChannel.open(path, StandardOpenOption.READ)) {
			long length = fc.size();
			long address;
			try {
				if (OLD_MAP) {
					address = (long) MAP.invoke(fc, 0, 0L, length);
				} else {
					address = (long) MAP.invoke(fc, 0, 0L, length, false);
				}
			} catch(InvocationTargetException | IllegalAccessException ex) {
				throw new IllegalStateException("Could not invoke map0", ex);
			}
			ByteData mappedFile = new UnsafeMappedFile(address, length, () -> {
				try {
					UNMAP.invoke(null, address, length);
				} catch(IllegalAccessException | InvocationTargetException ex) {
					throw new InternalError(ex);
				}
			});
			return mappedFile;
		}
	}

	static {
		boolean oldMap = false;
		Method map = null;
		Method unmap = null;
		get:
		{
			Class<?> c;
			try {
				c = Class.forName("sun.nio.ch.FileChannelImpl");
			} catch(ClassNotFoundException ignored) {
				break get;
			}
			try {
				map = c.getDeclaredMethod("map0", int.class, long.class, long.class, boolean.class);
			} catch(NoSuchMethodException ex) {
				try {
					map = c.getDeclaredMethod("map0", int.class, long.class, long.class);
					oldMap = true;
				} catch(NoSuchMethodException ignored) {
					break get;
				}
			}
			try {
				map.setAccessible(true);
				unmap = c.getDeclaredMethod("unmap0", long.class, long.class);
				unmap.setAccessible(true);
			} catch(Exception ex) {
				// Locked up environment, probably threw InaccessibleObjectException
				map = null;
				unmap = null;
			}
		}
		MAP = map;
		UNMAP = unmap;
		OLD_MAP = oldMap;
	}
}
