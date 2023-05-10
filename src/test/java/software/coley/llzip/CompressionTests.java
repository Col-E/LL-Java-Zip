package software.coley.llzip;

import org.junit.jupiter.api.Test;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.compression.Decompressor;
import software.coley.llzip.format.compression.DeflateDecompressor;
import software.coley.llzip.format.model.ZipArchive;
import software.coley.llzip.util.ByteData;
import software.coley.llzip.util.ByteDataUtil;

import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests for {@link LocalFileHeader#decompress(Decompressor)}.
 *
 * @author Matt Coley
 */
public class CompressionTests {
	@Test
	public void testDeflate() {
		try {
			ZipArchive zip = ZipIO.readStandard(Paths.get("src/test/resources/hello.jar"));
			LocalFileHeader localFileHeader = zip.getLocalFiles().get(0);
			assertEquals("Hello.class", ByteDataUtil.toString(localFileHeader.getFileName()));
			ByteData classData = localFileHeader.decompress(new DeflateDecompressor());
			Utils.assertDefinesString(classData, "Hello world!");
		} catch (IOException ex) {
			fail(ex);
		}
	}
}
