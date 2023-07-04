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
			entry.reset();
		}
		try {
			byte[] output = entry.decompress;
			byte[] buffer = entry.buffer;
			Inflater inflater = entry.inflater;
			long position = 0L;
			long length = data.length();
			int remaining = 0;
			boolean needsInput = true;
			do {
				if (needsInput) {
					remaining = (int) Math.min(buffer.length, length);
					if (remaining != 0) {
						data.get(position, buffer, 0, remaining);
						length -= remaining;
						position += remaining;
						inflater.setInput(buffer, 0, remaining);
					}
					entry.offset = 0;
				}
				int written = InflaterHackery.inflate(entry, buffer, remaining, output);
				if (written != 0) {
					out.write(output, 0, written);
				}
				int state = entry.state;
				if (state == 2) {
					break;
				}
				needsInput = state == 1;
			} while (true);
		} catch (DataFormatException e) {
			String s = e.getMessage();
			throw (ZipException) new ZipException(s != null ? null : "Invalid ZLIB data format").initCause(e);
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


}