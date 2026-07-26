package ar.edu.itba.paw.interfaces.exceptions;

public class DateRangeException extends RuntimeException {
    private final long groupTripId;
    private final String internationError;

    public DateRangeException(String message, String internationError, long groupTripId) {
        super(message);
        this.internationError = internationError;
        this.groupTripId = groupTripId;
    }
    public long getGroupTripId() {
        return groupTripId;
    }

    @Override
    public String getLocalizedMessage() {
        return internationError;
    }
}

