package software.coley.llzip.util.lazy;

/**
 * Lazy int getter.
 */
public class LazyIntChain extends LazyInt {
	/**
	 * @param values
	 * 		Chained values to combined as a lookup.
	 */
	public LazyIntChain(LazyInt... values) {
		super(() -> {
			int sum = 0;
			for (LazyInt lazy : values) {
				sum += lazy.get();
			}
			return sum;
		});
	}
}
