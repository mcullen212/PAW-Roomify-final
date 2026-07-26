package ar.edu.itba.paw.interfaces.exceptions;

public class CancelException extends RuntimeException {
    public CancelException(final String message) {
        super(message);
    }
}
