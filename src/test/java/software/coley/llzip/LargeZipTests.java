package software.coley.llzip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Tests that reading larger strings from zip files works correctly.
 */
public class LargeZipTests {

	@Test
	public void testLargeStrings() {
		try {
			ZipIO.readStandard(Paths.get("src/test/resources/sample-long-name.zip"));
		} catch (IOException error) {
			Assertions.fail(error);
		}
	}

	// Left this test out for obvious reasons, all you need to run it is a zip file containing a single file >2.1GB
	/*
	@Test
	public void testLargeFiles() {
		try {
			ZipArchive zip = ZipIO.readStandard(Paths.get("src/test/resources/large.zip"));
			// Just need a file bigger than 2.1GB to validate this test works
			Assertions.assertTrue(zip.getLocalFiles().get(0).getFileData().length() > 0x80000000L);
		} catch (IOException error) {
			Assertions.fail(error);
		}
	}
	 */
}
