package software.coley.lljzip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.model.AbstractZipFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.JvmZipReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Some edge case tests where {@link JvmZipReader} needs to match a few quirky cases from {@link ZipFile}.
 *
 * @author Matt Coley
 */
public class JvmVsZipFileEqualityEdgeCaseTests {
	@Disabled("Updating to JDK changes how ZipFile works, which no longer reads the test sample")
	@ParameterizedTest
	@ValueSource(strings = {
			"resource-pack-trick-header-N-to-1-cen-to-loc-mapping.zip",
	})
	public void test(String name) {
		Path path = Paths.get("src/test/resources/" + name);

		try {
			ZipArchive zipJvm = ZipIO.read(path, new JvmZipReader(false));
			ZipArchive zipAdapting = ZipIO.readAdaptingIO(path);
			int sizeDel = zipAdapting.getLocalFiles().size();
			int sizeJvm = zipJvm.getLocalFiles().size();
			Set<String> namesAdapting = zipAdapting.getLocalFiles().stream().map(AbstractZipFileHeader::getFileNameAsString).collect(Collectors.toCollection(TreeSet::new));
			Set<String> namesJvm = zipJvm.getLocalFiles().stream().map(AbstractZipFileHeader::getFileNameAsString).collect(Collectors.toCollection(TreeSet::new));
			Set<String> namesDifference = new TreeSet<>(namesAdapting);
			namesDifference.removeAll(namesJvm);

			// Both files should have the same number of local file headers
			assertEquals(0, namesDifference.size());
			assertEquals(sizeDel, sizeJvm);
		} catch (Exception ex) {
			fail(ex);
		}
	}
}
