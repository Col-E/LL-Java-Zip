package software.coley.llzip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Tests that reading larger strings from zip files works correctly.
 */
public class LargeStringTests {
	@Test
	public void testLargeStrings() {
		try {
			ZipIO.readStandard(Paths.get("src/test/resources/large-strings.zip"));
		} catch (IOException error) {
			Assertions.fail(error);
		}
	}
}
