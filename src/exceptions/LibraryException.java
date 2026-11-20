package exceptions;

public class LibraryException extends Exception {
    private String errorCode;

    public LibraryException(String message) {
        super(message);
        this.errorCode = "LIB-001";
    }

    public LibraryException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return "LibraryException [" + errorCode + "]: " + getMessage();
    }
}
