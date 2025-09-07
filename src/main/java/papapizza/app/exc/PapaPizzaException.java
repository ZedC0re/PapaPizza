package papapizza.app.exc;

public class PapaPizzaException extends Exception{
	public PapaPizzaException() {
	}

	public PapaPizzaException(String message) {
		super(message);
	}

	public PapaPizzaException(String message, Throwable cause) {
		super(message, cause);
	}

	public PapaPizzaException(Throwable cause) {
		super(cause);
	}

	public PapaPizzaException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
