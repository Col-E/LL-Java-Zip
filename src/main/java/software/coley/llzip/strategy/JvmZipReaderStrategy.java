package software.coley.llzip.strategy;

import software.coley.llzip.ZipArchive;

import java.io.IOException;

/**
 * The JVM has some edge cases in how it parses zip/jar files.
 * It allows for some tricks that most tools do not support/expect.
 *
 * @author Matt Coley
 */
public class JvmZipReaderStrategy implements ZipReaderStrategy {
	@Override
	public void read(ZipArchive zip, byte[] data) throws IOException {
		// TODO: https://github.com/openjdk/jdk8u/blob/4a4236a366eeb961baf157f0938634c1647c447f/jdk/src/share/bin/parse_manifest.c#L229
		//  - https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT
	}
}
