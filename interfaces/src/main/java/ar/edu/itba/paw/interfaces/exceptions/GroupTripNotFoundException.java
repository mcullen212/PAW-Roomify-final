package ar.edu.itba.paw.interfaces.exceptions;

public class GroupTripNotFoundException extends RuntimeException {
    public GroupTripNotFoundException(long groupTripId) {
        super("Group trip not found with id: " + groupTripId);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.groupTrip.notFound";
    }
}
