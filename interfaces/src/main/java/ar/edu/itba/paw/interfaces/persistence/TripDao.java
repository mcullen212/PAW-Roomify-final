package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripDao {
    Optional<Trip> findTripById(long id);
    Trip create(long groupTripId, String country, DateRange dateRange);
    List<Trip> findTripsByGroupTripId(long groupTripId, int page, int pageSize);
    boolean existsOverlappingTrip(long groupTripId, DateRange dateRange);
    int countTripsByGroupTripId(long groupTripId);
    List<Trip> bringMytrips(String country, LocalDate startDate, LocalDate endDate, User user, int page, int pageSize);
    int countMyTrips(String country, LocalDate startDate, LocalDate endDate, User user);
    Optional<Trip> findContainingActiveTrip(String country, LocalDate startDate, LocalDate endDate, User user);
    Optional<Trip> findNearestActiveTripInCountry(String country, User user);
}
