package software.coley.lljzip;

import org.junit.jupiter.api.Test;
import software.coley.lljzip.format.model.AbstractZipFileHeader;
import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.util.MemorySegmentUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests covering ZIP64 input handling across the standard and JVM reader strategies.
 *
 * @author Matt Coley
 */
public class Zip64Tests {
	@Test
	public void testZip64OffsetsAndParity() throws IOException {
		withArchive(Utils.zip64OffsetArchive(), path -> {
			// For the test archive, all three strategies that can reasonably take ZIP64 input
			// should be able to read the same local file headers, and the same central directory entries.
			try (ZipArchive zipStd = ZipIO.readStandard(path);
			     ZipArchive zipJvm = ZipIO.readJvm(path);
			     ZipArchive zipAdapting = ZipIO.readAdaptingIO(path)) {
				assertEquals(2, zipStd.getLocalFiles().size());
				assertEquals(2, zipJvm.getLocalFiles().size());
				assertEquals(2, zipAdapting.getLocalFiles().size());

				LocalFileHeader stdEntry = zipStd.getLocalFileByName("beta.txt");
				LocalFileHeader jvmEntry = zipJvm.getLocalFileByName("beta.txt");
				assertNotNull(stdEntry);
				assertNotNull(jvmEntry);
				assertEquals("second-entry", MemorySegmentUtil.toString(stdEntry.getFileData()));
				assertEquals("second-entry", MemorySegmentUtil.toString(jvmEntry.getFileData()));

				CentralDirectoryFileHeader stdDirectory = zipStd.getNameFilteredCentralDirectories("beta.txt"::equals).getFirst();
				CentralDirectoryFileHeader jvmDirectory = zipJvm.getNameFilteredCentralDirectories("beta.txt"::equals).getFirst();
				assertTrue(stdDirectory.hasZip64RelativeOffsetOfLocalHeader());
				assertTrue(jvmDirectory.hasZip64RelativeOffsetOfLocalHeader());
				assertTrue(stdDirectory.getRelativeOffsetOfLocalHeader() > 0L);
				assertTrue(jvmDirectory.getRelativeOffsetOfLocalHeader() > 0L);

				Set<String> adaptingNames = zipAdapting.getLocalFiles().stream()
						.map(AbstractZipFileHeader::getFileNameAsString)
						.collect(Collectors.toCollection(TreeSet::new));
				Set<String> stdNames = zipStd.getLocalFiles().stream()
						.map(AbstractZipFileHeader::getFileNameAsString)
						.collect(Collectors.toCollection(TreeSet::new));
				Set<String> jvmNames = zipJvm.getLocalFiles().stream()
						.map(AbstractZipFileHeader::getFileNameAsString)
						.collect(Collectors.toCollection(TreeSet::new));
				assertEquals(adaptingNames, stdNames);
				assertEquals(adaptingNames, jvmNames);
			}
		});
	}

	@Test
	public void testZip64DataDescriptorWithSignature() throws IOException {
		// We should be able to read the expected contents from the test archive with data descriptors.
		assertZip64DataDescriptorArchive(Utils.zip64DataDescriptorArchive(true));
	}

	@Test
	public void testZip64DataDescriptorWithoutSignature() throws IOException {
		// We should be able to read the expected contents from the test archive without any data descriptors.
		assertZip64DataDescriptorArchive(Utils.zip64DataDescriptorArchive(false));
	}

	private void assertZip64DataDescriptorArchive(byte[] archiveBytes) throws IOException {
		withArchive(archiveBytes, path -> {
			try (ZipArchive zipStd = ZipIO.readStandard(path);
			     ZipArchive zipJvm = ZipIO.readJvm(path)) {
				LocalFileHeader stdEntry = zipStd.getLocalFileByName("descriptor.txt");
				LocalFileHeader jvmEntry = zipJvm.getLocalFileByName("descriptor.txt");
				assertNotNull(stdEntry);
				assertNotNull(jvmEntry);

				byte[] expected = "descriptor-data".getBytes();
				assertArrayEquals(expected, MemorySegmentUtil.toByteArray(stdEntry.getFileData()));
				assertArrayEquals(expected, MemorySegmentUtil.toByteArray(jvmEntry.getFileData()));
				assertEquals(expected.length, stdEntry.getCompressedSize());
				assertEquals(expected.length, jvmEntry.getCompressedSize());
			}
		});
	}

	private static void withArchive(byte[] archiveBytes, @Nonnull Utils.ThrowingConsumer<Path> consumer) throws IOException {
		Path path = Files.createTempFile("lljzip-zip64-input-", ".zip");
		path.toFile().deleteOnExit();
		try {
			Files.write(path, archiveBytes);
			consumer.accept(path);
		} finally {
			try {
				Files.deleteIfExists(path);
			} catch (IOException ignored) {
				// Mapped zip channels on Windows may still be releasing the file at test teardown.
			}
		}
	}
}
