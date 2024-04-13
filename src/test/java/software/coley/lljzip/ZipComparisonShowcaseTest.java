package software.coley.lljzip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.compression.ZipCompressions;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.MemorySegmentUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Showcase different behavior in handling tampered ZIP files between ZIP parsing techniques.
 *
 * @author Matt Coley
 */
@SuppressWarnings("ConstantValue")
@Disabled("only used for demonstration purposes, and does not make any assertions")
public class ZipComparisonShowcaseTest {
	// Flag to toggle skipping ZipEntry.isDirectory() values, which you see a lot of in application usage.
	private static final boolean SKIP_DIRS = true;

	@ParameterizedTest
	@ValueSource(strings = {
			"hello.jar",
			"hello.png",
			"hello-concat.jar",
			"hello-concat-junkheader.jar",
			"hello-copyjar-at-head.jar",
			"hello-copyjar-at-tail.jar",
			"hello-deceptive.jar",
			"hello-end-declares-0-entries.jar",
			"hello-end-declares-0-entries-0-offset.jar",
			"hello-jar-in-in-jar-in-jar-in-jar-in-jar.jar",
			"hello-junk-dir-length.jar",
			"hello-junk-eocd.jar",
			"hello-junk-local-length.jar",
			"hello-merged.jar",
			"hello-merged-fake-empty.jar",
			"hello-merged-junkheader.jar",
			"hello-no-local-length.jar",
			"hello-no-local-names.jar",
			"hello-secret.jar",
			"hello-secret-0-length-locals.jar",
			"hello-secret-junkheader.jar",
			"hello-secret-trailing-slash.jar",
			"hello-secret-trailing-slash-0-length-locals.jar",
			"hello-total-junk.jar",
			"hello-total-junk-large.jar",
			"hello-txt-stored.jar",
			"hello-txt-type-0.jar",
			"hello-wrong-crc-both.jar",
			"hello-wrong-crc-central.jar",
			"hello-wrong-crc-local.jar",
			"hello-wrong-local-compression.jar",
			"hello-zeroed-locals.jar",
	})
	public void test(String name) {
		Path path = Paths.get("src/test/resources/" + name);

		try {
			System.out.println("==== LL-J-ZIP (jvm-strategy) ====");
			ZipArchive zipJvm = ZipIO.readJvm(path);
			for (LocalFileHeader lfh : zipJvm.getLocalFiles()) {
				String entryName = lfh.getFileNameAsString();
				byte[] entryData = MemorySegmentUtil.toByteArray(ZipCompressions.decompress(lfh));
				handle(entryName, entryData);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			System.out.println("==== LL-J-ZIP (std-strategy) ====");
			ZipArchive zipJvm = ZipIO.readStandard(path);
			for (LocalFileHeader lfh : zipJvm.getLocalFiles()) {
				String entryName = lfh.getFileNameAsString();
				byte[] entryData = MemorySegmentUtil.toByteArray(ZipCompressions.decompress(lfh));
				handle(entryName, entryData);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			System.out.println("==== LL-J-ZIP (naive-strategy) ====");
			ZipArchive zipJvm = ZipIO.readNaive(path);
			for (LocalFileHeader lfh : zipJvm.getLocalFiles()) {
				String entryName = lfh.getFileNameAsString();
				byte[] entryData = MemorySegmentUtil.toByteArray(ZipCompressions.decompress(lfh));
				handle(entryName, entryData);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			System.out.println("==== ZipInputStream ====");
			ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path));
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				if (SKIP_DIRS && entry.isDirectory())
					continue;
				ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
				byte[] buffer = new byte[2048];
				int read;
				while ((read = zipInputStream.read(buffer)) != -1) {
					dataOut.write(buffer, 0, read);
				}
				String entryName = entry.getName();
				byte[] entryData = dataOut.toByteArray();
				handle(entryName, entryData);
			}
			zipInputStream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			System.out.println("==== ZipFile ====");
			try (ZipFile zipFile = new ZipFile(path.toFile())) {
				Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
				while (zipEntries.hasMoreElements()) {
					ZipEntry entry = zipEntries.nextElement();
					if (SKIP_DIRS && entry.isDirectory())
						continue;
					InputStream inputStream = zipFile.getInputStream(entry);
					ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
					byte[] buffer = new byte[2048];
					int read;
					while ((read = inputStream.read(buffer)) != -1) {
						dataOut.write(buffer, 0, read);
					}

					String entryName = entry.getName();
					byte[] entryData = dataOut.toByteArray();
					handle(entryName, entryData);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			System.out.println("==== FileSystem ====");
			ClassLoader loader = null;
			FileSystem zipFs = FileSystems.newFileSystem(path, loader);
			for (Path root : zipFs.getRootDirectories()) {
				Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						try {
							String entryName = file.toString().substring(1);
							byte[] entryData = Files.readAllBytes(file);
							handle(entryName, entryData);
						} catch (IOException ex) {
							System.err.println("Failed to read ZIP contents: " + file);
						}
						return FileVisitResult.CONTINUE;
					}
				});
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void handle(String name, byte[] data) {
		if (name.contains("Hello")) {
			System.out.println(name + " --> Has secret message: " + new String(data).contains("The secret code is: ROSE"));
		} else {
			System.out.println(name);
		}
	}
}
