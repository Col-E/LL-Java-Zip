package software.coley.llzip.format.compression;

import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.util.BufferData;
import software.coley.llzip.util.ByteData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * Decompressor implementation for {@link ZipCompressions#DEFLATED}.
 *
 * @author Matt Coley
 */
public class DeflateDecompressor implements Decompressor {
	private static final Deque<Inflater> INFLATERS = new ArrayDeque<>();

	@Override
	public ByteData decompress(LocalFileHeader header, ByteData data) throws IOException {
		if (header.getCompressionMethod() != ZipCompressions.DEFLATED)
			throw new IOException("LocalFileHeader contents not using 'Deflated'!");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Inflater inflater;
		Deque<Inflater> inflaters = INFLATERS;
		synchronized (inflaters) {
			inflater = inflaters.poll();
			if (inflater == null) {
				inflater = new Inflater(true);
			}
		}
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
			} while (!inflater.finished() && !inflater.needsDictionary());
		}catch (DataFormatException e) {
			String s = e.getMessage();
			throw (ZipException) new ZipException(s != null ? null : "Invalid ZLIB data format").initCause(e);
		} finally {
			end: {
				if (inflaters.size() < 32) {
					synchronized (inflaters) {
						if (inflaters.size() < 32) {
							inflaters.push(inflater);
							break end;
						}
					}
				}
				inflater.end();
			}
		}
		return BufferData.wrap(out.toByteArray());
	}
}
