package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripMatchDecision;

import java.util.Optional;

/**
 * Resultado de evaluar si un cuarto (por su pais) y un rango de fechas caen dentro
 * de alguno de los trips activos del usuario. Lo usa el flujo de reserva para decidir
 * si el cuarto se agrega al trip o si hay que avisar que las fechas no coinciden.
 */
public class TripMatch {

    private final TripMatchDecision decision;
    private final Trip trip; // el trip involucrado (contenedor si CONTAINED, el mas proximo del pais si DATES_OUTSIDE); null si NONE

    private TripMatch(TripMatchDecision decision, Trip trip) {
        this.decision = decision;
        this.trip = trip;
    }

    public static TripMatch contained(Trip trip) {
        return new TripMatch(TripMatchDecision.CONTAINED, trip);
    }

    public static TripMatch datesOutside(Trip trip) {
        return new TripMatch(TripMatchDecision.DATES_OUTSIDE, trip);
    }

    public static TripMatch none() {
        return new TripMatch(TripMatchDecision.NONE, null);
    }

    public TripMatchDecision getDecision() {
        return decision;
    }

    public Optional<Trip> getTrip() {
        return Optional.ofNullable(trip);
    }
}
