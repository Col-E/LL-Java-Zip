package software.coley.lljzip;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.ExtraFieldTime;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests ensuring that the extra field can be read for custom/detailed windows time values.
 * This generally means the extra-field is assigned correctly.
 *
 * @author Matt Coley
 */
public class WindowsExtraFieldTimeTests {
	@ParameterizedTest
	@ValueSource(strings = {"standard", "naive", "jvm"})
	public void validity(@Nonnull String mode) {
		Path path = Paths.get("src/test/resources/content-with-windows-time.jar");
		try {
			long timeCreate = 1000000000000L;
			long timeModify = 1200000000000L;
			long timeAccess = 1400000000000L;
			ZipArchive archive;
			switch (mode) {
				case "standard":
					archive = ZipIO.readStandard(path);
					break;
				case "naive":
					archive = ZipIO.readNaive(path);
					break;
				case "jvm":
					archive = ZipIO.readJvm(path);
					break;
				default:
					throw new IllegalStateException();
			}
			ExtraFieldTime.TimeWrapper wrapper = read(archive);
			assertEquals(timeCreate, wrapper.getCreationMs());
			assertEquals(timeModify, wrapper.getModifyMs());
			assertEquals(timeAccess, wrapper.getAccessMs());
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Nonnull
	private ExtraFieldTime.TimeWrapper read(@Nonnull ZipArchive archive) {
		LocalFileHeader header = archive.getLocalFiles().get(0);
		return Objects.requireNonNull(ExtraFieldTime.read(header), "Missing time data");
	}

}
