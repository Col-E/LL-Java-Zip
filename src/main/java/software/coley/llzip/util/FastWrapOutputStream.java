package software.coley.llzip.util;

import java.io.ByteArrayOutputStream;

public final class FastWrapOutputStream extends ByteArrayOutputStream {
	public BufferData wrap() {
		return BufferData.wrap(buf, 0, count);
	}
}
