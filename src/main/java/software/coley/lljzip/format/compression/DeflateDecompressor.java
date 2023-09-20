package software.coley.lljzip.format.compression;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.FastWrapOutputStream;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * Decompressor implementation for {@link ZipCompressions#DEFLATED}.
 *
 * @author Matt Coley
 */
public class DeflateDecompressor implements Decompressor {
	public static final DeflateDecompressor INSTANCE = new DeflateDecompressor();

	private DeflateDecompressor() {
		// deny construction
	}

	@Override
	public ByteData decompress(LocalFileHeader header, ByteData data) throws IOException {
		if (header.getCompressionMethod() != ZipCompressions.DEFLATED)
			throw new IOException("LocalFileHeader contents not using 'Deflated'!");
		Inflater inflater = new Inflater(true);
		FastWrapOutputStream out = new FastWrapOutputStream();
		try {
			byte[] output = new byte[1024];
			byte[] buffer = new byte[1024];
			long position = 0L;
			long length = data.length();
			do {
				if (inflater.needsInput()) {
					int remaining = (int) Math.min(buffer.length, length);
					if (remaining == 0) {
						break;
					}
					data.get(position, buffer, 0, remaining);
					length -= remaining;
					position += remaining;
					inflater.setInput(buffer, 0, remaining);
				}
				int count = inflater.inflate(output);
				if (count != 0) {
					out.write(output, 0, count);
				}
			} while (!inflater.finished());
		} catch (DataFormatException e) {
			String s = e.getMessage();
			throw (ZipException) new ZipException(s != null ? s : "Invalid ZLIB data format").initCause(e);
		} finally {
			inflater.end();
		}
		return out.wrap();
	}
}
