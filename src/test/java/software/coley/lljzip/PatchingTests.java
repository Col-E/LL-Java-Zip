package software.coley.lljzip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.coley.lljzip.format.ZipPatterns;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.ForwardScanZipReader;
import software.coley.lljzip.format.read.NaiveLocalFileZipReader;
import software.coley.lljzip.format.read.SimpleZipPartAllocator;
import software.coley.lljzip.format.read.ZipPartAllocator;
import software.coley.lljzip.format.transform.IdentityZipPartMapper;
import software.coley.lljzip.format.transform.JvmClassDirectoryMapper;
import software.coley.lljzip.format.write.ZipOutputStreamZipWriter;
import software.coley.lljzip.util.MemorySegmentUtil;
import software.coley.lljzip.util.data.MemorySegmentData;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.lljzip.util.MemorySegmentUtil.readLongSlice;

/**
 * Tests for patching the <i>"trick"</i> jars, and making them compatible with standard Java ZIP apis.
 *
 * @author Matt Coley
 */
public class PatchingTests {
	// The sample 'hello-merged-fake-empty.jar' is not included because its CEN points to the correct local entry.
	// Unlike the samples seen below, it only has one END header, not two.
	@ParameterizedTest
	@ValueSource(strings = {
			"hello-concat.jar",
			"hello-concat-junkheader.jar",
			"hello-merged.jar",
			"hello-merged-junkheader.jar",
	})
	public void testTrickJarPatched(String name) {
		try {
			Path path = Paths.get("src/test/resources/" + name);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			// Validate the original jar file yields the wrong 'Hello' class
			try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(path))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().contains("Hello.class")) {
						byte[] buffer = new byte[1024];
						baos = new ByteArrayOutputStream();
						int len;
						while ((len = zis.read(buffer)) > 0)
							baos.write(buffer, 0, len);

						// The version of 'Hello' read does not contain the right message.
						// Running these jars produces the other message, seen below after patching.
						Utils.assertDefinesString(baos.toByteArray(), "Hello world!");
					}
				}
			}

			// Parse the zip with LL-Java zip, then write back using std java apis
			// in order to create a std java compliant jar.
			ZipArchive zip = ZipIO.readJvm(path);
			new ZipOutputStreamZipWriter().write(zip, baos);
			byte[] fixed = baos.toByteArray();

			// Validate the new jar bytes can be read and show the true file contents.
			try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fixed))) {
				ZipEntry entry;
				while ((entry = zis.getNextEntry()) != null) {
					if (entry.getName().contains("Hello.class")) {
						byte[] buffer = new byte[1024];
						baos = new ByteArrayOutputStream();
						int len;
						while ((len = zis.read(buffer)) > 0)
							baos.write(buffer, 0, len);

						// Now check if the secret code is found.
						// If not, we extracted the wrong class.
						Utils.assertDefinesString(baos.toByteArray(), "The secret code is: ROSE");
					}
				}
			}
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Test
	@SuppressWarnings("resource")
	public void testReadIgnoringFileLengths() {
		ZipPartAllocator allocator = lengthIgnoringAllocator();

		Path path = Paths.get("src/test/resources/resource-pack-trick-data-ioobe.zip");

		// Naive strategy finds the file with the trailing '/'
		NaiveLocalFileZipReader naiveStrategy = new NaiveLocalFileZipReader(allocator);
		ZipArchive zip = assertDoesNotThrow(() -> ZipIO.read(path, naiveStrategy));
		assertNotNull(zip.getLocalFileByName("assets/luxbl/lang/en_us.json/"), "Missing 'en_us' file");

		// Standard strategy finds it, but its authoritative CEN defines the file name to not include the trailing '/'
		ForwardScanZipReader forwardStrategy = new ForwardScanZipReader(allocator);
		zip = assertDoesNotThrow(() -> ZipIO.read(path, forwardStrategy));
		assertNotNull(zip.getLocalFileByName("assets/luxbl/lang/en_us.json"), "Missing 'en_us' file");
	}

	@Test
	@SuppressWarnings("resource")
	public void testTrailingSlashTransform() {
		// The 'JvmClassDirectoryMapper' maps 'Name.class/' paths to 'Name.class'
		Path path = Paths.get("src/test/resources/hello-secret-trailing-slash.jar");
		ZipArchive zip = assertDoesNotThrow(() -> ZipIO.readStandard(path)
				.withMapping(new JvmClassDirectoryMapper(new IdentityZipPartMapper())));
		assertNotNull(zip.getLocalFileByName("Hello.class"), "Trailing slash was not patched");
	}

	private static ZipPartAllocator lengthIgnoringAllocator() {
		return new SimpleZipPartAllocator() {
			@Nonnull
			@Override
			public LocalFileHeader newLocalFileHeader() {
				return new LocalFileHeader() {
					@Nonnull
					@Override
					protected MemorySegmentData readFileData(@Nonnull MemorySegment data, long headerOffset) {
						long localOffset = MIN_FIXED_SIZE + getFileNameLength() + getExtraFieldLength();
						long nextStart = MemorySegmentUtil.indexOfQuad(data, headerOffset + localOffset, ZipPatterns.LOCAL_FILE_HEADER_QUAD);
						long fileDataLength = nextStart > headerOffset ?
								nextStart - (headerOffset + localOffset) :
								data.byteSize() - (headerOffset + localOffset);
						return MemorySegmentData.of(readLongSlice(data, headerOffset, localOffset, fileDataLength));
					}
				};
			}
		};
	}
}
