package software.coley.lljzip.util.lazy;

import javax.annotation.Nonnull;

/**
 * Lazy int getter.
 */
public class LazyIntChain extends LazyInt {
	/**
	 * @param values
	 * 		Chained values to combined as a lookup.
	 */
	public LazyIntChain(@Nonnull LazyInt... values) {
		super(() -> {
			int sum = 0;
			for (LazyInt lazy : values) {
				sum += lazy.get();
			}
			return sum;
		});
	}
}
