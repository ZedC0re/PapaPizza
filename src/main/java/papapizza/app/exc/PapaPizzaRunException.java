package papapizza.app.exc;

public class PapaPizzaRunException extends RuntimeException{
	public PapaPizzaRunException(){
		super();
	}

	public PapaPizzaRunException(String message) {
		super(message);
	}

	public PapaPizzaRunException(String message, Throwable cause) {
		super(message, cause);
	}
}
