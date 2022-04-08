package software.coley.llzip.strategy;

import software.coley.llzip.ZipCompressions;
import software.coley.llzip.part.LocalFileHeader;
import software.coley.llzip.util.BufferData;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Decompressor implementation for {@link ZipCompressions#DEFLATED}.
 *
 * @author Matt Coley
 */
public class DeflateDecompressor implements Decompressor {
	@Override
	public ByteData decompress(LocalFileHeader header, ByteData bytes) throws IOException {
		if (header.getCompressionMethod() != ZipCompressions.DEFLATED)
			throw new IOException("LocalFileHeader contents not using 'Deflated'!");
		Inflater inflater = new Inflater(true);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			InflaterInputStream in = new InflaterInputStream(new ByteDataInputStream(bytes), inflater);
			// We can't trust the uncompressed size, so we will keep going until the inflater stream says we're done.
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) != -1)
				out.write(buffer, 0, len);
		} finally {
			inflater.end();
		}
		return BufferData.wrap(out.toByteArray());
	}
}
