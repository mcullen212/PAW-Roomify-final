package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.DTO.GroupTripPage;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GroupTripService {
    GroupTrip create(long ownerId, String name);
    Optional<GroupTrip> findGroupTripById(Long groupTripId);
    void recalculateDates(long groupTripId);
    List<GroupTrip> getGroupTripForUser(String email, TripStatus tripStatus, int page, int pageSize);
    List<GroupTrip> getGroupTripForUser(Long userId, TripStatus tripStatus, int page, int pageSize);
    int getGroupTripCountForUser(String email, TripStatus tripStatus);
    int getGroupTripCountForUser(Long userId, TripStatus tripStatus);
    GroupTripPage searchGroupTrips(Long userId, String country, String checkIn, String checkOut, TripStatus tripStatus, int page, int pageSize);
    List<Trip> getTripsForGroupTripAssociation(Long userId, String country, String checkIn, String checkOut, TripStatus tripStatus, int page, int pageSize);
    int getTripsForGroupTripAssociationCount(Long userId, String country, String checkIn, String checkOut, TripStatus tripStatus);
    boolean isOwnerTrip(String email, long groupTripId);
    GroupTrip updateGroupTrip(long groupId, String title, TripStatus targetStatus);
    boolean updateStatus(long groupId, TripStatus targetStatus);
    boolean finishedPlanning(long groupId);
}
