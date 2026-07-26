package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.model.DTO.TripMatch;
import ar.edu.itba.paw.model.trip.Trip;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripService {
    Trip create(long groupTripId, String country, LocalDate startDate, LocalDate endDate) throws DateRangeException;
    Optional<Trip> findTripById(long tripId);
    Trip findTripByGroupTripId(long groupTripId, long tripId);
    List<Trip> findTripsByGroupTripId(long groupTripId, int page, int pageSize);
    int countTripsByGroupTripId(long groupTripId);
    List<Trip> bringMyTrips(String country, String checkIn, String checkOut, Long userId, int page, int pageSize);
    int countMyTrips(String country, String checkIn, String checkOut, Long userId);
    Trip matchRoomToTrip(Long roomId, long groupTripId, long tripId);
    TripMatch matchRoomToTrip(long roomId, LocalDate checkIn, LocalDate checkOut, String email);
}
