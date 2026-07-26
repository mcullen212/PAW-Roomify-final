package ar.edu.itba.paw.interfaces.exceptions;

public class TripContactsNotOwnerException extends RuntimeException {
    public TripContactsNotOwnerException(long tripId) {
        super("User is not the owner of the group trip for trip " + tripId);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.trip.contactsNotOwner";
    }
}
