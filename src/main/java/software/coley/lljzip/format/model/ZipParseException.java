package software.coley.lljzip.format.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exception thrown when parsing {@link ZipPart} contents from a data source.
 *
 * @author Matt Coley
 * @see Type
 */
public class ZipParseException extends Exception {
	private final Type type;

	public ZipParseException(@Nullable Throwable cause, @Nonnull Type type) {
		super(type.getMessage(), cause);
		this.type = type;
	}

	/**
	 * @return Parse failure case.
	 */
	@Nonnull
	public Type getType() {
		return type;
	}

	/**
	 * Enum of common parse failure cases.
	 */
	public enum Type {
		IOOBE_FILE_NAME("Bounds check failed reading file name"),
		IOOBE_FILE_DATA("Bounds check failed reading file data"),
		IOOBE_FILE_EXTRA("Bounds check failed reading file extra"),
		IOOBE_CEN_COMMENT("Bounds check failed reading directory comment"),
		IOOBE_OTHER("Bounds check failed"),
		OTHER("Unknown zip parse error");

		private final String message;

		Type(String message) {
			this.message = message;
		}

		String getMessage() {
			return message;
		}
	}
}
