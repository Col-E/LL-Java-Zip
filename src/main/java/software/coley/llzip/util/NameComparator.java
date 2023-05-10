package software.coley.llzip.util;

import software.coley.llzip.format.model.CentralDirectoryFileHeader;
import software.coley.llzip.format.model.LocalFileHeader;
import software.coley.llzip.format.model.ZipPart;
import software.coley.llzip.format.write.JavaZipWriterStrategy;

import java.util.Comparator;
import java.util.Optional;

/**
 * For sorting by {@link CentralDirectoryFileHeader#getFileName()} and {@link LocalFileHeader#getFileName()}.
 * This is intended to be used in cases like {@link JavaZipWriterStrategy} where offset information is ignored anyways.
 *
 * @author Matt Coley
 */
public class NameComparator implements Comparator<ZipPart> {
	private final Comparator<ZipPart> fallback;

	/**
	 * @param fallback
	 * 		Fallback comparator for
	 */
	public NameComparator(Comparator<ZipPart> fallback) {
		this.fallback = fallback;
	}

	@Override
	public int compare(ZipPart o1, ZipPart o2) {
		if (o1 instanceof LocalFileHeader && o2 instanceof LocalFileHeader) {
			LocalFileHeader header1 = (LocalFileHeader) o1;
			LocalFileHeader header2 = (LocalFileHeader) o2;
			String name1 = Optional.ofNullable(header1.getLinkedDirectoryFileHeader())
					.map(CentralDirectoryFileHeader::getFileNameAsString)
					.orElse(header1.getFileNameAsString());
			String name2 = Optional.ofNullable(header2.getLinkedDirectoryFileHeader())
					.map(CentralDirectoryFileHeader::getFileNameAsString)
					.orElse(header2.getFileNameAsString());
			return name1.compareTo(name2);
		} else if (o1 instanceof CentralDirectoryFileHeader && o2 instanceof CentralDirectoryFileHeader) {
			CentralDirectoryFileHeader header1 = (CentralDirectoryFileHeader) o1;
			CentralDirectoryFileHeader header2 = (CentralDirectoryFileHeader) o2;
			String name1 = header1.getFileNameAsString();
			String name2 = header2.getFileNameAsString();
			return name1.compareTo(name2);
		}
		return fallback.compare(o1, o2);
	}
}
