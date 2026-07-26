package ar.edu.itba.paw.interfaces.exceptions;

public class TripNotFoundException extends RuntimeException {
    public TripNotFoundException(long tripId) {
        super("Trip not found with id " + tripId);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.trip.notFound";
    }
}
