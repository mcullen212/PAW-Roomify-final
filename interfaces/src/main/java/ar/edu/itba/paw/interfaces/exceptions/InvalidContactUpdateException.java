package ar.edu.itba.paw.interfaces.exceptions;

public class InvalidContactUpdateException extends RuntimeException {
    public InvalidContactUpdateException(String message) {
        super(message);
    }
}
