package ar.edu.itba.paw.interfaces.exceptions;

public class BookedDateException extends SwapException {
    private final String localizedMessage;
    private boolean isRequest;

    public BookedDateException(String message, String localizedMessage, Long contactId, long roomId) {
        super(message, localizedMessage, contactId, roomId);
        this.isRequest = false;
        this.localizedMessage = localizedMessage;
    }

    public BookedDateException(String message, String localizedMessage, long roomId) {
        super(message, localizedMessage, roomId);
        this.isRequest = true;
        this.localizedMessage = localizedMessage;

    }

    public boolean getIsRequest(){
        return isRequest;
    }
}
