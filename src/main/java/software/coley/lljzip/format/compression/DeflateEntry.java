package software.coley.lljzip.format.compression;

import software.coley.lljzip.util.InflaterHackery;

import java.util.zip.Inflater;

/**
 * Wrapper for deflation information.
 *
 * @author xDark
 */
public final class DeflateEntry {
	/** wrapped inflater. */
	public final Inflater inflater = new Inflater(true);
	final byte[] decompress = new byte[16384];
	final byte[] buffer = new byte[8192];
	public int state;
	public int offset;

	/**
	 * Reset deflation state.
	 */
	void reset() {
		InflaterHackery.reset(inflater);
		state = offset = 0;
	}
}
