package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.trip.Trip;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ContactJpaDao implements ContactDao {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Contact> findContactById(long id) {
        return findContactsByIds(List.of(id), "c.id").stream().findFirst();
    }

    @Override
    public Contact create(long offerUserId, // Recibimos el ID
                          Room requestedRoom,
                          Room offeredRoom,
                          LocalDateTime contactDate,
                          SwapStatus status,
                          boolean isSwap,
                          BigDecimal moneyOffer,
                          DateRange requestedRange,
                          DateRange offeredRange) {

        User offerUser = em.getReference(User.class, offerUserId);

        Contact contact = new Contact(
                requestedRoom,
                contactDate,
                status,
                isSwap,
                moneyOffer,
                offerUser,
                offeredRoom,
                requestedRange,
                offeredRange);
        em.persist(contact);
        return contact;
    }

    @Override
    public Contact confirmedDateRange(long contactId, DateRange newOfferedRange) {
        Contact contact = em.find(Contact.class, contactId);

        contact.setOfferedRange(newOfferedRange);
        contact.setStatus(SwapStatus.ACCEPTED);

        return contact;
    }

    @Override
    public Contact acceptMoneyOffer(long contactId) {
        Contact contact = em.find(Contact.class, contactId);
        if (contact == null)
            throw new IllegalArgumentException("Contact not found");

        contact.setStatus(SwapStatus.ACCEPTED);
        return contact;
    }

    @Override
    public boolean hasAcceptedContactInRangeRequestedSide(long roomId, DateRange requestedRange) {
        Query query = em.createNativeQuery(
                "SELECT EXISTS ( SELECT 1 FROM contact WHERE requested_room_id = ? AND UPPER(status) = 'ACCEPTED' AND NOT (requested_end_date < ? OR requested_start_date > ?) LIMIT 1 )"
        );
        query.setParameter(1, roomId);
        query.setParameter(2, requestedRange.getStartDate());
        query.setParameter(3, requestedRange.getEndDate());
        return (Boolean) query.getSingleResult();
    }

    @Override
    public boolean hasAcceptedContactInRangeOfferedSide(long roomId, DateRange requestedRange) {
        Query query = em.createNativeQuery(
                "SELECT EXISTS ( SELECT 1 FROM contact WHERE room_offer_id = ? AND UPPER(status) = 'ACCEPTED' AND NOT (offered_end_date < ? OR offered_start_date > ?) LIMIT 1 )"
        );
        query.setParameter(1, roomId);
        query.setParameter(2, requestedRange.getStartDate());
        query.setParameter(3, requestedRange.getEndDate());
        return (Boolean) query.getSingleResult();
    }

    @Override
    public List<Contact> findByOwnerId(long ownerId) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE requested_room.owner_id = :ownerId
                          AND c.status = 'PENDING'
                        ORDER BY c.contact_date DESC, c.id DESC
                        """)
                .setParameter("ownerId", ownerId);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public List<Contact> findAcceptedRequestsByUser(long userId, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        WHERE c.offer_user_id = :userId
                          AND c.status = 'ACCEPTED'
                        ORDER BY c.contact_date DESC, c.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("userId", userId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public int countAcceptedRequestsByUser(long userId) {
        Number count = (Number) em.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM contact c
                        WHERE c.offer_user_id = :userId
                          AND c.status = 'ACCEPTED'
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        return count.intValue();
    }

    @Override
    public List<Contact> findAcceptedOffersByUser(long ownerId, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE requested_room.owner_id = :ownerId
                          AND c.status = 'ACCEPTED'
                        ORDER BY c.contact_date DESC, c.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("ownerId", ownerId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public int countAcceptedOffersByUser(long ownerId) {
        Number count = (Number) em.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE requested_room.owner_id = :ownerId
                          AND c.status = 'ACCEPTED'
                        """)
                .setParameter("ownerId", ownerId)
                .getSingleResult();
        return count.intValue();
    }

    @Override
    public void updateStatus(long contactId, SwapStatus status) {
        Query query = em.createQuery(
                "UPDATE Contact c SET c.status = :status WHERE c.id = :id"
        );
        query.setParameter("status", status);
        query.setParameter("id", contactId);
        query.executeUpdate();
    }

    @Override
    public List<Contact> findPendingRequestedOverlap(long contactId) {
        Query idQuery = em.createNativeQuery("""
                SELECT c2.id
                FROM contact AS c2
                WHERE c2.id != :contactId
                  AND UPPER(c2.status) = 'PENDING'
                  AND c2.requested_room_id = (SELECT requested_room_id FROM contact WHERE id = :contactId)
                  AND c2.requested_start_date <= (SELECT requested_end_date FROM contact WHERE id = :contactId)
                  AND c2.requested_end_date >= (SELECT requested_start_date FROM contact WHERE id = :contactId)
                ORDER BY c2.contact_date DESC, c2.id DESC
                """)
                .setParameter("contactId", contactId);

        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public List<Long> deletePendingRequestedOverlap(long contactId) {
        @SuppressWarnings("unchecked")
        Query query = em.createNativeQuery(
                "DELETE FROM contact AS c2 WHERE c2.id != ? AND UPPER(c2.status) = 'PENDING' " +
                        "  AND c2.requested_room_id = (SELECT requested_room_id FROM contact WHERE id = ?) " +
                        "  AND ( c2.requested_start_date <= (SELECT requested_end_date FROM contact WHERE id = ?) " +
                        "    AND c2.requested_end_date >= (SELECT requested_start_date FROM contact WHERE id = ?) ) " +
                        "RETURNING c2.offer_user_id"
        );

        query.setParameter(1, contactId);
        query.setParameter(2, contactId);
        query.setParameter(3, contactId);
        query.setParameter(4, contactId);

        return (List<Long>) query.getResultList().stream()
                .map(it -> ((Number) it).longValue())
                .collect(Collectors.toList());
    }

    @Override
    public List<DateRange> contactAcceptedDatesForRoom(long roomId, int page, int pageSize) {
        @SuppressWarnings("unchecked")
        Query query = em.createNativeQuery(
                "(SELECT requested_start_date AS start_date, requested_end_date AS end_date FROM contact WHERE requested_room_id = ? AND UPPER(status) = 'ACCEPTED') " +
                        "UNION ALL " +
                        "(SELECT offered_start_date AS start_date, offered_end_date AS end_date FROM contact WHERE room_offer_id = ? AND UPPER(status) = 'ACCEPTED' AND is_swap = TRUE) " +
                        "ORDER BY start_date"
        );
        query.setParameter(1, roomId);
        query.setParameter(2, roomId);
        query.setFirstResult((page - 1) * pageSize);
        query.setMaxResults(pageSize);
        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(row -> new DateRange(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((java.sql.Date) row[1]).toLocalDate()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<DateRange> contactAcceptedDatesForRoomBetween(long roomId, LocalDate startDate, LocalDate endDate) {
        Query query = em.createNativeQuery("""
                (SELECT requested_start_date AS start_date, requested_end_date AS end_date
                 FROM contact
                 WHERE requested_room_id = :roomId
                   AND UPPER(status) = 'ACCEPTED'
                   AND requested_start_date <= :endDate
                   AND requested_end_date >= :startDate)
                UNION ALL
                (SELECT offered_start_date AS start_date, offered_end_date AS end_date
                 FROM contact
                 WHERE room_offer_id = :roomId
                   AND UPPER(status) = 'ACCEPTED'
                   AND is_swap = TRUE
                   AND offered_start_date <= :endDate
                   AND offered_end_date >= :startDate)
                ORDER BY start_date
                """)
                .setParameter("roomId", roomId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate);

        List<Object[]> results = query.getResultList();
        return results.stream()
                .map(row -> new DateRange(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((java.sql.Date) row[1]).toLocalDate()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<DateRange> contactAcceptedDatesForRoomOffered(long roomId, int page, int pageSize) {
        Query query = em.createNativeQuery("""
                        SELECT c.offered_start_date, c.offered_end_date
                        FROM contact c
                        WHERE c.room_offer_id = :roomId
                          AND c.status = 'ACCEPTED'
                        ORDER BY c.offered_start_date, c.id
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("roomId", roomId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<?> rows = query.getResultList();
        return rows.stream()
                .map(value -> (Object[]) value)
                .map(row -> new DateRange(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        ((java.sql.Date) row[1]).toLocalDate()))
                .toList();
    }

    @Override
    public int countAcceptedDatesForRoomOffered(long roomId) {
        Number count = (Number) em.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM contact c
                        WHERE c.room_offer_id = :roomId
                          AND c.status = 'ACCEPTED'
                        """)
                .setParameter("roomId", roomId)
                .getSingleResult();

        return count.intValue();
    }

    @Override
    public long getRoomOfferedId(long contactId){
        TypedQuery<Long> query = em.createQuery(
                "SELECT c.roomOffered.id FROM Contact c WHERE c.id = :id",
                Long.class
        );
        query.setParameter("id", contactId);
        return query.getSingleResult();
    }

    @Override
    public int getStayDurationDays(long contactId) {
        Query query = em.createNativeQuery(
                "SELECT (requested_end_date - requested_start_date) AS days FROM contact WHERE id = ?"
        );
        query.setParameter(1, contactId);
        Number result = (Number) query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }

    @Override
    public boolean isSwap(long contactId) {
        TypedQuery<Boolean> query = em.createQuery(
                "SELECT c.isSwap FROM Contact c WHERE c.id = :id",
                Boolean.class
        );
        query.setParameter("id", contactId);
        return query.getSingleResult();
    }

    @Override
    public boolean hasActiveContacts(long roomId) {
        Query query = em.createQuery("""
            SELECT 1
            FROM Contact c
            WHERE (
                c.roomRequested.id = :roomId
                AND c.status = :accepted
                AND c.requestedRange.endDate >= CURRENT_DATE
            )
            OR (
                c.roomOffered.id = :roomId
                AND c.status = :accepted
                AND c.offeredRange.endDate >= CURRENT_DATE
            )
        """);
        query.setParameter("roomId", roomId);
        query.setParameter("accepted", SwapStatus.ACCEPTED);
        query.setMaxResults(1);
        return !query.getResultList().isEmpty();
    }

    @Override
    public List<Contact> findPendingContactsForRoom(long roomId) {
        TypedQuery<Contact> query = em.createQuery("""
            SELECT DISTINCT c
            FROM Contact c
            JOIN FETCH c.roomRequested rr
            JOIN FETCH rr.owner rro
            LEFT JOIN FETCH c.roomOffered ro
            LEFT JOIN FETCH ro.owner roo
            LEFT JOIN FETCH c.offerUser ou
            WHERE c.status = :pending
              AND (rr.id = :roomId OR ro.id = :roomId)
            ORDER BY c.contactDate DESC
        """, Contact.class);
        query.setParameter("roomId", roomId);
        query.setParameter("pending", SwapStatus.PENDING);
        return query.getResultList();
    }

    @Override
    public void deletePendingContactsForRoom(long roomId) {
        em.createQuery("""
            DELETE FROM TripContact tc
            WHERE tc.contact.id IN (
                SELECT c.id
                FROM Contact c
                WHERE c.status = :pending
                  AND (c.roomRequested.id = :roomId OR c.roomOffered.id = :roomId)
            )
        """)
                .setParameter("roomId", roomId)
                .setParameter("pending", SwapStatus.PENDING)
                .executeUpdate();

        em.createQuery("""
            DELETE FROM Contact c
            WHERE c.status = :pending
              AND (c.roomRequested.id = :roomId OR c.roomOffered.id = :roomId)
        """)
                .setParameter("roomId", roomId)
                .setParameter("pending", SwapStatus.PENDING)
                .executeUpdate();
    }

    @Override
    public boolean replySameSwap(long roomRequestedId,LocalDate startDate,LocalDate endDate,long roomOfferedId){
        SwapStatus pendingStatus = SwapStatus.PENDING;

        TypedQuery<Long> query = em.createQuery("""
        SELECT COUNT(c.id) FROM Contact c WHERE c.roomRequested.id = :roomRequestedId
          AND c.roomOffered.id = :roomOfferedId AND c.requestedRange.startDate = :startDate
          AND c.requestedRange.endDate = :endDate
          AND c.status = :pendingStatus""", Long.class);

        query.setParameter("roomRequestedId", roomRequestedId);
        query.setParameter("roomOfferedId", roomOfferedId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("pendingStatus", pendingStatus);

        Long count = query.getSingleResult();
        return count != null && count > 0;
    }
    @Override
    public boolean replySameSwapMoney(long roomRequestedId,LocalDate startDate,LocalDate endDate,BigDecimal money){
        SwapStatus pendingStatus = SwapStatus.PENDING;

        TypedQuery<Long> query = em.createQuery("""
        SELECT COUNT(c.id) FROM Contact c WHERE c.roomRequested.id = :roomRequestedId
          AND c.requestedRange.startDate = :startDate AND c.requestedRange.endDate = :endDate
          AND c.moneyOffer = :money
          AND c.status = :pendingStatus""", Long.class);

        query.setParameter("roomRequestedId", roomRequestedId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        query.setParameter("money", money);
        query.setParameter("pendingStatus", pendingStatus);
        Long count = query.getSingleResult();
        return count != null && count > 0;
    }

    @Override
    public void cancelSwap(long contactId) {

        Contact contact = em.find(Contact.class, contactId);
        if (contact == null) {
            throw new IllegalArgumentException("Contact not found");
        }
        if (contact.getStatus() != SwapStatus.ACCEPTED && contact.getStatus() != SwapStatus.PENDING) {
            throw new IllegalStateException("Only accepted or pending contacts can be canceled");
        }
        contact.setStatus(SwapStatus.CANCELED);
    }

    @Override
    public long countCancelSwapsUser(long userId) {
        Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM contact c
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE c.status = 'CANCELED'
                  AND (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                """)
                .setParameter("userId", userId)
                .getSingleResult();
        return count.longValue();
    }

    @Override
    public long countExpiredSwapsUser(long userId){
        Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM contact c
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE c.status = 'EXPIRED'
                  AND (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                """)
                .setParameter("userId", userId)
                .getSingleResult();
        return count.longValue();
    }

    @Override
    public List<Contact> findCanceledByUserPage(long userId, int page, int pageSize) {
        return findByUserAndStatusPage(userId, SwapStatus.CANCELED.name(), page, pageSize);
    }
    @Override
    public List<Contact> findExpiredByUserPage(long userId, int page, int pageSize) {
        return findByUserAndStatusPage(userId,SwapStatus.EXPIRED.name(), page, pageSize);
    }

    private List<Contact> findByUserAndStatusPage(long userId, String status, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                SELECT c.id
                FROM contact c
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE c.status = :status
                  AND (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                ORDER BY c.contact_date DESC, c.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }


    @Override
    public List<Contact> findByOfferUserId(long offerUserId) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        WHERE c.offer_user_id = :offerUserId
                          AND c.status = 'PENDING'
                        ORDER BY c.contact_date DESC, c.id DESC
                        """)
                .setParameter("offerUserId", offerUserId);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public List<Contact> findAvailableContactsForTrip(long userId, Trip trip, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE (requested_room.owner_id = :userId OR c.offer_user_id = :userId)
                          AND NOT EXISTS (
                              SELECT 1
                              FROM trip_contact tc
                              WHERE tc.trip_id = :tripId
                                AND tc.contact_id = c.id
                          )
                          AND c.status IN ('ACCEPTED', 'PENDING')
                        ORDER BY c.contact_date DESC, c.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("userId", userId)
                .setParameter("tripId", trip.getId())
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        return findContactsByIds(toLongIds(idQuery.getResultList()), "c.contactDate DESC, c.id DESC");
    }

    @Override
    public List<Contact> findPendingReceivedByUserPage(final long userId, final int page, final int pageSize) {
        Query idQuery = em.createNativeQuery("""
                SELECT c.id
                FROM contact c
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE c.status = 'PENDING'
                  AND requested_room.owner_id = :userId
                ORDER BY c.contact_date DESC, c.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("userId", userId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public long countPendingReceivedUser(final long userId) {
        Number count = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM contact c
                JOIN room requested_room ON requested_room.id = c.requested_room_id
                WHERE c.status = 'PENDING'
                  AND requested_room.owner_id = :userId
                """)
                .setParameter("userId", userId)
                .getSingleResult();
        return count.longValue();
    }

    @Override
    public List<Contact> findPastTripsByEmail(long userId, LocalDate today, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                          AND c.status = 'ACCEPTED'
                          AND CASE
                                WHEN requested_room.owner_id = :userId AND c.is_swap = TRUE
                                  THEN COALESCE(c.offered_end_date, c.requested_end_date)
                                ELSE c.requested_end_date
                              END < :today
                        ORDER BY c.requested_start_date DESC, c.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("userId", userId)
                .setParameter("today", today)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.requestedRange.startDate DESC, c.id DESC");
    }

    @Override
    public int countPastTripsByEmail(long userId, LocalDate today) {
        Number count = (Number) em.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                          AND c.status = 'ACCEPTED'
                          AND CASE
                                WHEN requested_room.owner_id = :userId AND c.is_swap = TRUE
                                  THEN COALESCE(c.offered_end_date, c.requested_end_date)
                                ELSE c.requested_end_date
                              END < :today
                        """)
                .setParameter("userId", userId)
                .setParameter("today", today)
                .getSingleResult();
        return count.intValue();
    }

    @Override
    public List<Contact> findUpcomingTripsByUserId(long userId, LocalDate today, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                          AND c.status = 'ACCEPTED'
                          AND CASE
                                WHEN requested_room.owner_id = :userId AND c.is_swap = TRUE
                                  THEN COALESCE(c.offered_end_date, c.requested_end_date)
                                ELSE c.requested_end_date
                              END >= :today
                        ORDER BY c.requested_start_date, c.id
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("userId", userId)
                .setParameter("today", today)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.requestedRange.startDate ASC, c.id ASC");
    }
    @Override
    public int countUpcomingTripsByUserId(long userId, LocalDate today) {
        Number count = (Number) em.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM contact c
                        JOIN room requested_room ON requested_room.id = c.requested_room_id
                        WHERE (c.offer_user_id = :userId OR requested_room.owner_id = :userId)
                          AND c.status = 'ACCEPTED'
                          AND CASE
                                WHEN requested_room.owner_id = :userId AND c.is_swap = TRUE
                                  THEN COALESCE(c.offered_end_date, c.requested_end_date)
                                ELSE c.requested_end_date
                              END >= :today
                        """)
                .setParameter("userId", userId)
                .setParameter("today", today)
                .getSingleResult();
        return count.intValue();
    }

    @Override
    public List<Contact> findRequestedSwapsByUserPage(long userId, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT c.id
                        FROM contact c
                        WHERE c.offer_user_id = :userId
                          AND c.status = 'PENDING'
                        ORDER BY c.contact_date DESC, c.id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("userId", userId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findContactsByIds(ids, "c.contactDate DESC, c.id DESC");
    }

    @Override
    public long countRequestedSwapsByUser(long userId) {
        Number count = (Number) em.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM contact c
                        WHERE c.offer_user_id = :userId
                          AND c.status = 'PENDING'
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        return count.longValue();
    }
    @Override
    public BigDecimal getTotalMoneyEarnedByUser(long userId) {
        return getTotalMoneyForUser(userId, true);
    }

    @Override
    public BigDecimal getTotalMoneySpentByUser(long userId) {
        return getTotalMoneyForUser(userId, false);
    }

    private BigDecimal getTotalMoneyForUser(long userId, boolean earned) {

        String userCondition = earned
                ? "c.roomRequested.owner.id = :userId"
                : "c.offerUser.id = :userId";

        String jpql =
                "SELECT COALESCE(SUM(c.moneyOffer), 0) " +
                        "FROM Contact c " +
                        "WHERE c.status = :status " +
                        "AND c.isSwap = false " +
                        "AND " + userCondition + " " +
                        "AND c.requestedRange.endDate < CURRENT_DATE";

        BigDecimal result = em.createQuery(jpql, BigDecimal.class)
                .setParameter("status", SwapStatus.ACCEPTED)
                .setParameter("userId", userId)
                .getSingleResult();

        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public long countCompletedSwapsByUser(long userId){
        Long count = em.createQuery(
                        "SELECT COUNT(c) " +
                                "FROM Contact c " +
                                "WHERE c.status = :status " +
                                "AND c.requestedRange.endDate < CURRENT_DATE " +
                                "AND (c.offerUser.id = :userId " +
                                "     OR c.roomRequested.owner.id = :userId)",
                        Long.class
                )
                .setParameter("status", SwapStatus.ACCEPTED).setParameter("userId", userId).getSingleResult();
        return count != null ? count : 0L;
    }

    @Override
    public List<Contact> findContactsForRoom(long roomId) {
        String jpql = """
                    SELECT DISTINCT c
                    FROM Contact c
                    JOIN FETCH c.roomRequested rr
                    LEFT JOIN FETCH c.roomOffered ro
                    LEFT JOIN FETCH c.offerUser ou
                    WHERE (
                        (rr.id = :roomId AND c.requestedRange.endDate >= CURRENT_DATE)
                        OR (c.isSwap = TRUE AND ro.id = :roomId AND c.offeredRange.endDate >= CURRENT_DATE)
                    )
                    AND c.status IN (:accepted, :pending)
                    ORDER BY c.contactDate DESC
                """;

        TypedQuery<Contact> query = em.createQuery(jpql, Contact.class);
        query.setParameter("roomId", roomId);
        query.setParameter("accepted", SwapStatus.ACCEPTED);
        query.setParameter("pending", SwapStatus.PENDING);

        List<Contact> contacts = query.getResultList();
        return contacts;
    }

    @Override
    public List<Contact> findExpiredPendingSwaps() {
        String jpql = """
                        SELECT c
                        FROM Contact c
                        WHERE c.status = :status
                          AND c.requestedRange.startDate < CURRENT_DATE
                    """;

        TypedQuery<Contact> query = em.createQuery(jpql, Contact.class);
        query.setParameter("status", SwapStatus.PENDING);

        return query.getResultList();
    }

    private List<Contact> findContactsByIds(List<Long> ids, String orderBy) {
        if (ids.isEmpty()) return List.of();

        return em.createQuery("""
                SELECT c
                FROM Contact c
                JOIN FETCH c.roomRequested rr
                JOIN FETCH rr.owner
                LEFT JOIN FETCH c.roomOffered ro
                LEFT JOIN FETCH ro.owner
                LEFT JOIN FETCH c.offerUser
                WHERE c.id IN :ids
                """ + " ORDER BY " + orderBy, Contact.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    private List<Long> toLongIds(List<?> rawIds) {
        return rawIds.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }

}
