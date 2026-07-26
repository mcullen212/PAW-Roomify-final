package ar.edu.itba.paw.interfaces.exceptions;

public class SwapException extends RuntimeException {
    private final String localizedMessage;
    private final Long contactId;
    private final long roomId;

    public SwapException(String message, String LocalizedMessage, Long contactId, long roomId) {
        super(message);
        this.localizedMessage = LocalizedMessage;
        this.contactId = contactId;
        this.roomId = roomId;
    }

    public SwapException(String message, String LocalizedMessage, Long roomId) {
        super(message);
        this.localizedMessage = LocalizedMessage;
        this.contactId = null;
        this.roomId = roomId;
    }

    @Override
    public String getLocalizedMessage() {
        return localizedMessage;
    }

    public Long getContactId() { return contactId; }
    public long getRoomId() { return roomId; }
}
