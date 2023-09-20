package software.coley.lljzip.format.compression;

import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.util.ByteData;
import software.coley.lljzip.util.FastWrapOutputStream;
import software.coley.lljzip.util.InflaterHackery;

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
	public static final UnsafeDeflateDecompressor INSTANCE = new UnsafeDeflateDecompressor();
	private static final int DEFLATE_CACHE_LIMIT = 64;
	private static final Deque<DeflateEntry> DEFLATE_ENTRIES = new ArrayDeque<>();

	private UnsafeDeflateDecompressor() {
		// deny construction
	}

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
				if (remaining > 0) {
					int written = InflaterHackery.inflate(entry, buffer, remaining, output);
					if (written != 0) {
						out.write(output, 0, written);
					}
					int state = entry.state;
					if ((state & 0b10) == 0b10) {
						// FIXME: This shouldn't happen, but if you're on JDK 9+ and the new inflate method is not found
						//  then the existing handling does not properly reset the inflater for some odd reason.
						//   - If this ever gets fixed, remove this if block.
						if (!InflaterHackery.NEW_INFLATE) {
							entry.inflater.reset();
						}
						break;
					}
					needsInput = state == 1;
				} else {
					break;
				}
			} while (true);
		} catch (DataFormatException e) {
			String s = e.getMessage();
			throw (ZipException) new ZipException(s != null ? s : "Invalid ZLIB data format").initCause(e);
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
