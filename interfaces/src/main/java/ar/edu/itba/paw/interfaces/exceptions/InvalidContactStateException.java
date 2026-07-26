package ar.edu.itba.paw.interfaces.exceptions;

public class InvalidContactStateException extends RuntimeException {
    public InvalidContactStateException(String message) {
        super(message);
    }
}
