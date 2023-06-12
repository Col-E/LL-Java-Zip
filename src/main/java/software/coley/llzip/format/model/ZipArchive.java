package software.coley.llzip.format.model;

import software.coley.llzip.util.OffsetComparator;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Zip file outline.
 *
 * @author Matt Coley
 */
public class ZipArchive implements AutoCloseable {
	private final List<ZipPart> parts = new ArrayList<>();
	private final Closeable closableBackingResource;

	/**
	 * @param closableBackingResource
	 * 		Closable resource backing the zip archive.
	 */
	public ZipArchive(Closeable closableBackingResource) {
		this.closableBackingResource = closableBackingResource;
	}

	/**
	 * @return All parts of the zip archive.
	 */
	public List<ZipPart> getParts() {
		return parts;
	}

	/**
	 * @param nameFilter
	 * 		Filter to limit entries with by file path name.
	 *
	 * @return Central directory header entries matching the given file path filter.
	 */
	public List<CentralDirectoryFileHeader> getNameFilteredCentralDirectories(Predicate<String> nameFilter) {
		return getCentralDirectories().stream()
				.filter(c -> nameFilter.test(c.getFileNameAsString()))
				.collect(Collectors.toList());
	}

	/**
	 * @param nameFilter
	 * 		Filter to limit entries with by file path name.
	 *
	 * @return Local file header entries matching the given file path filter.
	 */
	public List<LocalFileHeader> getNameFilteredLocalFiles(Predicate<String> nameFilter) {
		return getCentralDirectories().stream()
				.filter(c -> nameFilter.test(c.getFileNameAsString())) // Use central names, as they are authoritative
				.map(CentralDirectoryFileHeader::getLinkedFileHeader)
				.collect(Collectors.toList());
	}

	/**
	 * Searches for a local file entry for the given name.
	 * The authoritative {@link CentralDirectoryFileHeader#getFileName()} is used,
	 * not the {@link LocalFileHeader#getFileName()}.
	 *
	 * @param name
	 * 		Name to fetch contents of.
	 *
	 * @return Local file header for the path, or {@code null} if no such entry for the name exists.
	 */
	public LocalFileHeader getLocalFileByName(String name) {
		List<LocalFileHeader> matches = getNameFilteredLocalFiles(name::equals);
		if (matches.isEmpty()) return null;
		return matches.get(0);
	}

	/**
	 * @return Local file header entries.
	 */
	public List<LocalFileHeader> getLocalFiles() {
		return parts.stream()
				.filter(part -> part.type() == PartType.LOCAL_FILE_HEADER)
				.map(part -> (LocalFileHeader) part)
				.collect(Collectors.toList());
	}

	/**
	 * @return Central directory header entries.
	 */
	public List<CentralDirectoryFileHeader> getCentralDirectories() {
		return parts.stream()
				.filter(part -> part.type() == PartType.CENTRAL_DIRECTORY_FILE_HEADER)
				.map(part -> (CentralDirectoryFileHeader) part)
				.collect(Collectors.toList());
	}

	/**
	 * @return End of central directory.
	 */
	public EndOfCentralDirectory getEnd() {
		return parts.stream()
				.filter(part -> part.type() == PartType.END_OF_CENTRAL_DIRECTORY)
				.sorted(new OffsetComparator().reversed()) // Yield the LAST entry
				.limit(1)
				.map(part -> (EndOfCentralDirectory) part)
				.findFirst().orElse(null);
	}

	/**
	 * @return Closable resource backing the zip archive.
	 */
	protected Closeable getClosableBackingResource() {
		return closableBackingResource;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ZipArchive that = (ZipArchive) o;

		return parts.equals(that.parts);
	}

	@Override
	public int hashCode() {
		return parts.hashCode();
	}

	@Override
	public void close() throws IOException {
		if (closableBackingResource != null)
			closableBackingResource.close();
	}
}
