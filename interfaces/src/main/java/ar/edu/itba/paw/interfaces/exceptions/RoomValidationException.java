package ar.edu.itba.paw.interfaces.exceptions;

public class RoomValidationException extends RuntimeException {
    private final String localizedMessage;

    public RoomValidationException(String message, String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
    }

    @Override
    public String getLocalizedMessage() {
        return localizedMessage;
    }
}
