package software.coley.lljzip;

import org.junit.jupiter.api.Test;
import software.coley.lljzip.format.model.ZipArchive;
import software.coley.lljzip.format.read.AdaptingZipReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AdaptingZipReader} targeting buffer loop regressions.
 *
 * @author TheWakz
 */
public class AdaptingZipReaderTests {

    @Test
    public void testReadArchiveExceedingBufferSize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            byte[] dummyData = new byte[20 * 1024];

            ZipEntry entry = new ZipEntry("large_entry.txt");
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(dummyData.length);
            entry.setCompressedSize(dummyData.length);

            CRC32 crc = new CRC32();
            crc.update(dummyData);
            entry.setCrc(crc.getValue());

            zos.putNextEntry(entry);
            zos.write(dummyData);
            zos.closeEntry();
        }
        byte[] zipBytes = baos.toByteArray();

        assertTrue(zipBytes.length > 16384, "Test archive size must exceed 16KB");

        ZipArchive archive = assertDoesNotThrow(() -> ZipIO.read(zipBytes, new AdaptingZipReader()),
                "Failed to read with read(adapting) when size exceeds buffer");

        assertNotNull(archive, "Archive is null");
        assertNotNull(archive.getLocalFileByName("large_entry.txt"),
                "Missing 'large_entry.txt' file due to truncation");
    }
}
