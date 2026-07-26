package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContactDao {
    Contact create(long initiatorUserId,
                   Room requestedRoom,
                   Room offeredRoom,
                   LocalDateTime contactDate,
                   SwapStatus status,
                   boolean isSwap,
                   BigDecimal moneyOffer,
                   DateRange requestedRange,
                   DateRange offeredRange);

    Contact confirmedDateRange(long contactId, DateRange newOfferedRange);

    List<Contact> findPendingRequestedOverlap(long contactId);

    List<Long> deletePendingRequestedOverlap(long contactId);

    Optional<Contact> findContactById(long id);

    List<Contact> findByOwnerId(long ownerId);

    void updateStatus(long contactId, SwapStatus status);

    boolean hasAcceptedContactInRangeRequestedSide(long roomId, DateRange requestedRange);

    boolean hasAcceptedContactInRangeOfferedSide(long roomId, DateRange requestedRange);
    boolean replySameSwap(long roomRequestedId,LocalDate startDate,LocalDate endDate,long roomOfferedId);
    boolean replySameSwapMoney(long roomRequestedId,LocalDate startDate,LocalDate endDate,BigDecimal money);

    List<Contact> findAcceptedOffersByUser(long ownerId, int page, int pageSize);
    int countAcceptedOffersByUser(long ownerId);

    List<Contact> findAcceptedRequestsByUser(long userId, int page, int pageSize);
    int countAcceptedRequestsByUser(long userId);

    List<DateRange> contactAcceptedDatesForRoom(long roomId, int page, int pageSize);
    List<DateRange> contactAcceptedDatesForRoomBetween(long roomId, LocalDate startDate, LocalDate endDate);

    List<DateRange> contactAcceptedDatesForRoomOffered(long roomId, int page, int pageSize);
    int countAcceptedDatesForRoomOffered(long roomId);

    long getRoomOfferedId(long contactId);

    int getStayDurationDays(long contactId);

    boolean isSwap(long contactId);

    Contact acceptMoneyOffer(long contactId);

    boolean hasActiveContacts(long roomId);

    void cancelSwap(long contactId);

    List<Contact> findPendingContactsForRoom(long roomId);

    void deletePendingContactsForRoom(long roomId);

    long countCancelSwapsUser(long userId);

    List<Contact> findCanceledByUserPage(long userId, int page, int pageSize);

    long countExpiredSwapsUser(long userId);

    List<Contact> findExpiredByUserPage(long userId, int page, int pageSize);

    List<Contact> findByOfferUserId(long offerUserId);

    List<Contact> findAvailableContactsForTrip(long userId, Trip trip, int page, int pageSize);

    List<Contact> findPendingReceivedByUserPage(final long userId, final int page, final int pageSize);

    long countPendingReceivedUser(final long userId);

    List<Contact> findPastTripsByEmail(long userId, LocalDate today, int page, int pageSize);
    int countPastTripsByEmail(long userId, LocalDate today);
    List<Contact> findUpcomingTripsByUserId(long userId, LocalDate today, int page, int pageSize);
    int countUpcomingTripsByUserId(long userId, LocalDate today);
    List<Contact> findRequestedSwapsByUserPage(long userId, int page, int pageSize);
    long countRequestedSwapsByUser(long userId);
    BigDecimal getTotalMoneyEarnedByUser(long userId);

    BigDecimal getTotalMoneySpentByUser(long userId);

    long countCompletedSwapsByUser(long userId);
    List<Contact> findContactsForRoom(long roomId);

    List<Contact> findExpiredPendingSwaps();


    }
