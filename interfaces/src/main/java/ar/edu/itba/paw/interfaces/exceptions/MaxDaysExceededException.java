package ar.edu.itba.paw.interfaces.exceptions;

public class MaxDaysExceededException extends SwapException {
    public MaxDaysExceededException(String message, String localizedMessage, Long contactId, long roomId) {
        super(message, localizedMessage, contactId, roomId);
    }
}