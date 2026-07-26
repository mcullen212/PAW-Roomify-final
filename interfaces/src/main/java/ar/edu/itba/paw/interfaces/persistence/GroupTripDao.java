package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupTripDao {

    Optional<GroupTrip> findGroupTripById(long id);

    GroupTrip create(User user, String name, DateRange dateRange);

    List<GroupTrip> findGroupTripsByOwnerId(long ownerId, TripStatus tripStatus, int page, int pageSize);

    int countGroupTripsByOwnerId(long ownerId, TripStatus tripStatus);

    List<Trip> findTripsForGroupTripAssociation(long ownerId, TripStatus tripStatus, String country, LocalDate startDate, LocalDate endDate, int page, int pageSize);

    int countTripsForGroupTripAssociation(long ownerId, TripStatus tripStatus, String country, LocalDate startDate, LocalDate endDate);

    void updateDates(long groupTripId, DateRange dateRange);
    Optional<LocalDate> getEarliestTripStartDate(long groupTripId);
    Optional<LocalDate> getLatestTripEndDate(long groupTripId);
    boolean updateStatus(long groupTripId, TripStatus status);
    List<GroupTrip> findExpiredUpcomingTrips();
}
