package software.coley.llzip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.llzip.format.compression.ZipCompressions;
import software.coley.llzip.format.model.ZipArchive;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Showcase different behavior in handling tampered ZIP files between ZIP parsing techniques.
 *
 * @author Matt Coley
 */
@Disabled("only used for demonstration purposes, and does not make any assertions")
public class ZipComparisonShowcaseTest {
	@ParameterizedTest
	@ValueSource(strings = {
			"hello-concat.jar",
			"hello-concat-junkheader.jar",
			"hello-merged.jar",
			"hello-merged-junkheader.jar",
			"hello-merged-fake-empty.jar",
			"hello-secret-0-length-locals.jar"
	})
	public void testConcatAndMerged(String name) {
		Path path = Paths.get("src/test/resources/" + name);

		try {
			System.out.println("==== LL-J-ZIP (jvm-strategy) ====");
			ZipArchive zipJvm = ZipIO.readJvm(path);
			zipJvm.getLocalFiles().forEach(lfh -> {
				System.out.println(lfh.getFileNameAsString());
				try {
					ZipCompressions.decompress(lfh);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (Exception ex) {
			fail(ex);
		}

		try {
			System.out.println("==== ZipInputStream ====");
			ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path));
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				System.out.println(entry.getName());
			}
			zipInputStream.close();
		} catch (Exception ex) {
			// fail(ex);
		}

		try {
			System.out.println("==== ZipFile ====");
			try (ZipFile zipFile = new ZipFile(path.toFile())) {
				Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
				while (zipEntries.hasMoreElements()) {
					String fileName = zipEntries.nextElement().getName();
					System.out.println(fileName);
				}
			}
		} catch (Exception ex) {
			// fail(ex);
		}

		try {
			System.out.println("==== FileSystem ====");
			FileSystem zipFs = FileSystems.newFileSystem(path, null);
			for (Path root : zipFs.getRootDirectories()) {
				Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						System.out.println(file.toString().substring(1));
						return FileVisitResult.CONTINUE;
					}
				});
			}
		} catch (Exception ex) {
			// fail(ex);
		}
	}

	private static boolean hasFile(ZipArchive zip, String name) {
		return !zip.getNameFilteredLocalFiles(name::equals).isEmpty();
	}
}
