package ar.edu.itba.paw.interfaces.exceptions;

public class ForbiddenUserOperationException extends RuntimeException {
    private final String localizedMessage;

    public ForbiddenUserOperationException(String message) {
        super(message);
        this.localizedMessage = "error.forbidden";
    }

    public ForbiddenUserOperationException(String message, String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
    }

    @Override
    public String getLocalizedMessage() {
        return localizedMessage;
    }
}
