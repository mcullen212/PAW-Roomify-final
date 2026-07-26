package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ContactPage;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.ContactView;
import ar.edu.itba.paw.model.swaps.SwapStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContactService {
    Contact createContact(long roomRequestedId,
                          LocalDate startDate,
                          LocalDate endDate,
                          boolean isSwap,
                          BigDecimal moneyOffer,
                          String username,
                          Long roomOfferedId);
    Optional<Contact> getContactById(long contactId);
    Contact acceptedContact(long contactId, LocalDate startDate, LocalDate endDate);
    Contact updateContactStatus(long contactId, SwapStatus status, LocalDate checkIn, LocalDate checkOut, String email);
    void notifyPendingUserReject(Contact acceptedContact);
    void rejectedContact(long contactId);
    boolean exists(long contactId);
    List<Contact> findByOwnerEmail(String email);
    List<Contact> findAcceptedRequestsByUser(String email, int page, int pageSize);
    List<Contact> findAcceptedOffersByUser(String email, int page, int pageSize);
    List<DateRange> contactAcceptedDatesForRoomBetween(long roomId, LocalDate startDate, LocalDate endDate);
    long getRoomOfferedId(long contactId);
    boolean isSwap(long contactId);
    List<Contact> upcomingTrips(String email, LocalDate today, int page, int pageSize);
    List<Contact> pastTrips(String email, LocalDate today, int page, int pageSize);
    boolean hasActiveContacts(long roomId);
    Room userIsGoingTo(long contactId, String email);
    int getStayDurationDays(long contactId);
    List<Boolean> upcomingTripsCanBeCancelled(String email, LocalDate today, int page, int pageSize);
    void cancelSwap(long contactId, String username, LocalDate date);
    List<String> upcomingUsersOffering(String email, LocalDate today, int page, int pageSize);
    int countUpcomingTrips(String username, LocalDate today);
    List<Contact> receivedSwaps(String email, int page, int pageSize);
    List<String> receivedUsersOffering(String username, int page, int pageSize);
    long countReceivedSwaps(final String email);
    List<String> pastUsersOffering(String email, LocalDate today, int page, int pageSize);
    int countPastTripActions(String username, LocalDate today);
    List<Contact> canceledTripsPage(String email, int page, int pageSize);
    long countCanceledTrips(String email);
    List<Contact> getRequestedSwapsByUser(String username, int page, int pageSize);
    List<String> getRequestedOwners(String username, int page, int pageSize);
    long countRequestedSwaps(String username);
    List<Contact> findAvailableContactsForTrip(String email, long tripId, int page, int pageSize);
    boolean isAccepter(String email, long contactId);
    boolean belongsToSwap(String email, long contactId);
    BigDecimal getTotalMoneyEarnedByUser(long userId);
    BigDecimal getTotalMoneySpentByUser(long userId);
    long countCompletedSwapsByUser(long userId);
    List<Contact> findContactsForRoom(long roomId);
    void notifyRoomUpdate(long roomId, String oldTitle, String oldDescription, List<Amenity> oldAmenities,
                          String newTitle, String newDescription, List<Amenity> newAmenities, List<Contact> contacts);
    List<Contact> expiredSwapsPage(String email, int page, int pageSize);
    long countExpiredSwaps(String email);
    ContactPage findContactsPage(String email, ContactView view, int page, int pageSize);
    ContactPage findContactsPage(String email, ContactView view, Long tripId, int page, int pageSize);
}
