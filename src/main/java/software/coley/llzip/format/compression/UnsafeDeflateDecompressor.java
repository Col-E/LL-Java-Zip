package software.coley.llzip.format.compression;

import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.FastWrapOutputStream;
import software.coley.llzip.util.InflaterHackery;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.ZipException;

/**
 * Optimized implementation of {@link DeflateDecompressor} with unsafe resetting for more throughput.
 *
 * @author xDark
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class UnsafeDeflateDecompressor implements Decompressor {
	private static final int DEFLATE_CACHE_LIMIT = 64;
	private static final Deque<DeflateEntry> DEFLATE_ENTRIES = new ArrayDeque<>();
	private static final byte[] emptyBuf = new byte[0];

	static {
		Deque<DeflateEntry> entries = DEFLATE_ENTRIES;
		for (int i = 0; i < DEFLATE_CACHE_LIMIT; i++) {
			entries.push(new DeflateEntry());
		}
	}

	@Override
	public ByteData decompress(LocalFileHeader header, ByteData data) throws IOException {
		if (header.getCompressionMethod() != ZipCompressions.DEFLATED)
			throw new IOException("LocalFileHeader contents not using 'Deflated'!");
		FastWrapOutputStream out = new FastWrapOutputStream();
		DeflateEntry entry;
		Deque<DeflateEntry> inflaters = DEFLATE_ENTRIES;
		synchronized (inflaters) {
			entry = inflaters.poll();
		}
		if (entry == null) {
			entry = new DeflateEntry();
		} else {
			InflaterHackery.reset(entry.inflater);
		}
		try {
			byte[] output = entry.decompress;
			byte[] buffer = entry.buffer;
			Inflater inflater = entry.inflater;
			long position = 0L;
			long length = data.length();
			inflater.setInput(emptyBuf);
			do {
				if (inflater.needsInput()) {
					int remaining = (int) Math.min(buffer.length, length);
					if (remaining != 0) {
						data.get(position, buffer, 0, remaining);
						length -= remaining;
						position += remaining;
						inflater.setInput(buffer, 0, remaining);
					}
				}
				int count = inflater.inflate(output);
				if (count != 0) {
					out.write(output, 0, count);
				}
			} while (!inflater.finished());
		} catch (DataFormatException e) {
			String msg = e.getMessage();
			throw (ZipException) new ZipException(msg != null ? null : "Invalid ZLIB data format").initCause(e);
		} finally {
			end:
			{
				if (inflaters.size() < DEFLATE_CACHE_LIMIT) {
					synchronized (inflaters) {
						if (inflaters.size() < DEFLATE_CACHE_LIMIT) {
							inflaters.addFirst(entry);
							break end;
						}
					}
				}
				entry.inflater.end();
			}
		}
		return out.wrap();
	}

	private static final class DeflateEntry {
		final Inflater inflater = new Inflater(true);
		final byte[] decompress = new byte[1024];
		final byte[] buffer = new byte[8192];
	}
}
