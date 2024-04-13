package software.coley.lljzip.util;

import software.coley.lljzip.format.model.CentralDirectoryFileHeader;
import software.coley.lljzip.format.model.LocalFileHeader;
import software.coley.lljzip.format.model.ZipPart;
import software.coley.lljzip.format.write.ZipOutputStreamZipWriter;

import java.util.Comparator;
import java.util.Optional;

/**
 * For sorting by {@link CentralDirectoryFileHeader#getFileName()} and {@link LocalFileHeader#getFileName()}.
 * This is intended to be used in cases like {@link ZipOutputStreamZipWriter} where offset information is ignored anyways.
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
		if (o1 instanceof LocalFileHeader header1 && o2 instanceof LocalFileHeader header2) {
			String name1 = Optional.ofNullable(header1.getLinkedDirectoryFileHeader())
					.map(CentralDirectoryFileHeader::getFileNameAsString)
					.orElse(header1.getFileNameAsString());
			String name2 = Optional.ofNullable(header2.getLinkedDirectoryFileHeader())
					.map(CentralDirectoryFileHeader::getFileNameAsString)
					.orElse(header2.getFileNameAsString());
			return name1.compareTo(name2);
		} else if (o1 instanceof CentralDirectoryFileHeader header1 && o2 instanceof CentralDirectoryFileHeader header2) {
			String name1 = header1.getFileNameAsString();
			String name2 = header2.getFileNameAsString();
			return name1.compareTo(name2);
		}
		return fallback.compare(o1, o2);
	}
}
