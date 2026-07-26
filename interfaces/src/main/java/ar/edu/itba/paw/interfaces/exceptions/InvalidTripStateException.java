package ar.edu.itba.paw.interfaces.exceptions;

public class InvalidTripStateException extends IllegalStateException {
    private final long groupId;
    private final String localizedMsg;
    public InvalidTripStateException(String message, String localizedMsg, long groupId) {
        super(message);
        this.groupId = groupId;
        this.localizedMsg = localizedMsg;
    }

    public long getGroupId() {
        return groupId;
    }

    @Override
    public String getLocalizedMessage() {
        return localizedMsg;
    }
}
