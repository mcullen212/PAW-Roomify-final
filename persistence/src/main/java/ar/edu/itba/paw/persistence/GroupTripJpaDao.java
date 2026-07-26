package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.GroupTripDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupTripJpaDao implements GroupTripDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<GroupTrip> findGroupTripById(long id) {
        return Optional.ofNullable(em.find(GroupTrip.class, id));
    }

    @Override
    public GroupTrip create(User user, String name, DateRange dateRange) { // <-- Modificado
        User owner = em.find(User.class, user.getId());

        if (owner == null) {
            throw new IllegalArgumentException("User with ID " + user.getId() + " not found.");
        }

        GroupTrip groupTrip = new GroupTrip(owner, name, dateRange);
        em.persist(groupTrip);
        return groupTrip;
    }

    @Override
    public List<GroupTrip> findGroupTripsByOwnerId(long ownerId, TripStatus tripStatus, int page, int pageSize) {
        TypedQuery<GroupTrip> query = em.createQuery("""
                        SELECT gt
                        FROM GroupTrip gt
                        JOIN FETCH gt.owner
                        WHERE gt.owner.id = :ownerId
                          AND (:tripStatus IS NULL OR gt.status = :tripStatus)
                        ORDER BY gt.dateRange.startDate DESC, gt.id DESC
                        """, GroupTrip.class)
                .setParameter("ownerId", ownerId)
                .setParameter("tripStatus", tripStatus);

        return query
                .setFirstResult((page - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
    }

    @Override
    public int countGroupTripsByOwnerId(long ownerId, TripStatus tripStatus) {
        TypedQuery<Long> query = em.createQuery("""
                        SELECT COUNT(gt.id)
                        FROM GroupTrip gt
                        WHERE gt.owner.id = :ownerId
                          AND (:tripStatus IS NULL OR gt.status = :tripStatus)
                        """, Long.class)
                .setParameter("ownerId", ownerId)
                .setParameter("tripStatus", tripStatus);
        Long count = query.getSingleResult();
        return count.intValue();
    }

    @Override
    public List<Trip> findTripsForGroupTripAssociation(long ownerId, TripStatus tripStatus, String country, LocalDate startDate, LocalDate endDate, int page, int pageSize) {
        StringBuilder sql = new StringBuilder("""
                        SELECT MIN(t.id)
                        FROM trip t
                        JOIN group_trip gt ON gt.id = t.id_group_trip
                        WHERE gt.id_user = :ownerId
                        """);
        appendAssociationFilters(sql, tripStatus, country, startDate, endDate);
        sql.append("""
                        GROUP BY gt.id, gt.start_date
                        ORDER BY gt.start_date DESC, gt.id DESC
                        LIMIT :limit OFFSET :offset
                        """);

        Query query = em.createNativeQuery(sql.toString())
                .setParameter("ownerId", ownerId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        setAssociationFilterParameters(query, tripStatus, country, startDate, endDate);

        List<Long> tripIds = toLongIds(query.getResultList());
        return findTripsByIds(tripIds);
    }

    @Override
    public int countTripsForGroupTripAssociation(long ownerId, TripStatus tripStatus, String country, LocalDate startDate, LocalDate endDate) {
        StringBuilder sql = new StringBuilder("""
                        SELECT COUNT(DISTINCT gt.id)
                        FROM trip t
                        JOIN group_trip gt ON gt.id = t.id_group_trip
                        WHERE gt.id_user = :ownerId
                        """);
        appendAssociationFilters(sql, tripStatus, country, startDate, endDate);

        Query query = em.createNativeQuery(sql.toString())
                .setParameter("ownerId", ownerId);
        setAssociationFilterParameters(query, tripStatus, country, startDate, endDate);

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public void updateDates(long groupTripId, DateRange dateRange) {
        GroupTrip groupTrip = em.find(GroupTrip.class, groupTripId);
        if (groupTrip != null) {
            groupTrip.setDateRange(dateRange);
            em.merge(groupTrip);
        }
    }

    @Override
    public Optional<LocalDate> getEarliestTripStartDate(long groupTripId) {
        TypedQuery<LocalDate> query = em.createQuery(
                "SELECT MIN(t.dateRange.startDate) FROM Trip t WHERE t.groupTrip.id = :groupTripId",
                LocalDate.class
        );
        query.setParameter("groupTripId", groupTripId);
        return Optional.ofNullable(query.getSingleResult());
    }

    @Override
    public Optional<LocalDate> getLatestTripEndDate(long groupTripId) {
        TypedQuery<LocalDate> query = em.createQuery(
                "SELECT MAX(t.dateRange.endDate) FROM Trip t WHERE t.groupTrip.id = :groupTripId",
                LocalDate.class
        );
        query.setParameter("groupTripId", groupTripId);
        return Optional.ofNullable(query.getSingleResult());
    }

    @Override
    public boolean updateStatus(long groupTripId, TripStatus status){
        GroupTrip groupTrip = em.find(GroupTrip.class, groupTripId);
        if (groupTrip != null) {
            groupTrip.setStatus(status);
            em.merge(groupTrip);
            return true;
        }
        return false;
    }

    public List<GroupTrip> findExpiredUpcomingTrips() {
        String jpql = """
                        SELECT gt
                        FROM GroupTrip gt
                        WHERE gt.status = :upcomingStatus
                          AND gt.dateRange.endDate < CURRENT_DATE
                    """;

        TypedQuery<GroupTrip> query = em.createQuery(jpql, GroupTrip.class);
        query.setParameter("upcomingStatus", TripStatus.UPCOMING);

        return query.getResultList();
    }

    private void appendAssociationFilters(StringBuilder sql, TripStatus tripStatus, String country, LocalDate startDate, LocalDate endDate) {
        if (tripStatus != null) {
            sql.append(" AND gt.status = :tripStatus");
        }
        if (normalizeOptional(country) != null) {
            sql.append(" AND LOWER(t.country) = LOWER(:country)");
        }
        if (startDate != null && endDate != null) {
            sql.append("""
                         AND t.start_date <= :startDate
                         AND t.end_date >= :endDate
                        """);
        }
    }

    private void setAssociationFilterParameters(Query query, TripStatus tripStatus, String country, LocalDate startDate, LocalDate endDate) {
        if (tripStatus != null) {
            query.setParameter("tripStatus", tripStatus.name());
        }
        String normalizedCountry = normalizeOptional(country);
        if (normalizedCountry != null) {
            query.setParameter("country", normalizedCountry);
        }
        if (startDate != null && endDate != null) {
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
        }
    }

    private List<Trip> findTripsByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }

        return em.createQuery("""
                        SELECT t
                        FROM Trip t
                        JOIN FETCH t.groupTrip gt
                        JOIN FETCH gt.owner
                        WHERE t.id IN :ids
                        ORDER BY gt.dateRange.startDate DESC, gt.id DESC
                        """, Trip.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    private List<Long> toLongIds(List<?> rawIds) {
        return rawIds.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

}
