package software.coley.lljzip.util;

import software.coley.lljzip.format.model.ZipPart;

import java.util.Comparator;

/**
 * For sorting by {@link ZipPart#offset()}.
 *
 * @author Matt Coley
 */
public class OffsetComparator implements Comparator<ZipPart> {
	@Override
	public int compare(ZipPart o1, ZipPart o2) {
		return Long.compare(o1.offset(), o2.offset());
	}
}
