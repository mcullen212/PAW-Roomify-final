package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.TripDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.GroupTrip; // Necesario para em.find()
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;
import ar.edu.itba.paw.model.User;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class TripJpaDao implements TripDao {

    private static final List<TripStatus> ACTIVE_TRIP_STATUSES = List.of(TripStatus.PLANNING, TripStatus.UPCOMING);

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Trip> findTripById(long id) {
        return Optional.ofNullable(em.find(Trip.class, id));
    }

    @Override
    public Trip create(long groupTripId, String country, DateRange dateRange) {
        GroupTrip groupTrip = em.find(GroupTrip.class, groupTripId);

        if (groupTrip == null) {
            throw new IllegalArgumentException("GroupTrip with ID " + groupTripId + " not found.");
        }

        Trip trip = new Trip(groupTrip, country, dateRange);
        em.persist(trip);
        return trip;
    }

    @Override
    public List<Trip> findTripsByGroupTripId(long groupTripId, int page, int pageSize) {
        Query idQuery = em.createNativeQuery("""
                        SELECT t.id
                        FROM trip t
                        WHERE t.id_group_trip = :groupTripId
                        ORDER BY t.start_date, t.id
                        LIMIT :limit OFFSET :offset
                        """)
                .setParameter("groupTripId", groupTripId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> ids = toLongIds(idQuery.getResultList());
        return findTripsByIds(ids, "t.dateRange.startDate, t.id");
    }

    @Override
    public boolean existsOverlappingTrip(long groupTripId, DateRange dateRange) {
        Long count = em.createQuery("""
                        SELECT COUNT(t.id)
                        FROM Trip t
                        WHERE t.groupTrip.id = :groupTripId
                        AND t.dateRange.startDate < :endDate
                        AND t.dateRange.endDate > :startDate
                        """, Long.class)
                .setParameter("groupTripId", groupTripId)
                .setParameter("startDate", dateRange.getStartDate())
                .setParameter("endDate", dateRange.getEndDate())
                .getSingleResult();

        return count > 0;
    }

    @Override
    public int countTripsByGroupTripId(long groupTripId){
        Long count = em.createQuery(
                "SELECT COUNT(t.id) FROM Trip t WHERE t.groupTrip.id = :groupTripId",
                Long.class
        ).setParameter("groupTripId", groupTripId).getSingleResult();

        return count.intValue();
    }

    @Override
    public List<Trip> bringMytrips(String country, LocalDate startDate, LocalDate endDate, User user, int page, int pageSize) {
        int offset = (page - 1) * pageSize;

        StringBuilder sql = new StringBuilder("""
                SELECT t.id
                FROM trip t
                JOIN group_trip gt ON gt.id = t.id_group_trip
                WHERE gt.id_user = :userId
                """);
        appendOptionalFilters(sql, country, startDate, endDate);
        sql.append(" ORDER BY t.start_date, t.id LIMIT :limit OFFSET :offset");

        Query idQuery = em.createNativeQuery(sql.toString());
        idQuery.setParameter("userId", user.getId());
        idQuery.setParameter("limit", pageSize);
        idQuery.setParameter("offset", offset);
        setOptionalFilterParameters(idQuery, country, startDate, endDate);

        List<Long> tripIds = toLongIds(idQuery.getResultList());
        if (tripIds.isEmpty()) return Collections.emptyList();

        return findTripsByIds(tripIds, "t.dateRange.startDate, t.id");
    }

    @Override
    public int countMyTrips(String country, LocalDate startDate, LocalDate endDate, User user) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT t.id)
                FROM trip t
                JOIN group_trip gt ON gt.id = t.id_group_trip
                WHERE gt.id_user = :userId
                """);
        appendOptionalFilters(sql, country, startDate, endDate);

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("userId", user.getId());
        setOptionalFilterParameters(query, country, startDate, endDate);

        return ((Number) query.getSingleResult()).intValue();
    }

    @Override
    public Optional<Trip> findContainingActiveTrip(String country, LocalDate startDate, LocalDate endDate, User user) { // @TODO mal hecho no esta paginado ni usando nativequery
        // Trip ACTIVO (group trip en PLANNING/UPCOMING) del usuario, en ese pais, cuyo rango CONTIENE
        // por completo [startDate, endDate]. Trae el group trip (JOIN FETCH) para poder armar el DTO
        // fuera de la transaccion sin LazyInitializationException.
        List<Trip> results = em.createQuery("""
                        SELECT t FROM Trip t JOIN FETCH t.groupTrip gt
                        WHERE gt.owner.id = :userId
                        AND LOWER(t.country) = LOWER(:country)
                        AND gt.status IN :statuses
                        AND t.dateRange.startDate <= :startDate AND t.dateRange.endDate >= :endDate
                        ORDER BY t.dateRange.startDate, t.id
                        """, Trip.class)
                .setParameter("userId", user.getId())
                .setParameter("country", country)
                .setParameter("statuses", ACTIVE_TRIP_STATUSES)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<Trip> findNearestActiveTripInCountry(String country, User user) {
        // Cualquier trip ACTIVO del usuario en ese pais (sin importar fechas), el mas proximo primero.
        // Se usa para el mensaje del popup cuando el pais coincide pero las fechas no caen dentro.
        List<Trip> results = em.createQuery("""
                        SELECT t FROM Trip t JOIN FETCH t.groupTrip gt
                        WHERE gt.owner.id = :userId
                        AND LOWER(t.country) = LOWER(:country)
                        AND gt.status IN :statuses
                        ORDER BY t.dateRange.startDate, t.id
                        """, Trip.class)
                .setParameter("userId", user.getId())
                .setParameter("country", country)
                .setParameter("statuses", ACTIVE_TRIP_STATUSES)
                .setMaxResults(1)
                .getResultList();

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    private List<Trip> findTripsByIds(List<Long> ids, String orderBy) {
        if (ids.isEmpty()) return Collections.emptyList();

        return em.createQuery("SELECT t FROM Trip t JOIN FETCH t.groupTrip " +
                        "WHERE t.id IN :ids ORDER BY " + orderBy, Trip.class)
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

    private void appendOptionalFilters(StringBuilder sql, String country, LocalDate startDate, LocalDate endDate) {
        if (normalizeOptional(country) != null) {
            sql.append(" AND LOWER(t.country) = LOWER(:country)");
        }
        if (startDate != null && endDate != null) {
            sql.append("""
                     AND t.start_date <= :endDate
                     AND t.end_date >= :startDate
                    """);
        }
    }

    private void setOptionalFilterParameters(Query query, String country, LocalDate startDate, LocalDate endDate) {
        String normalizedCountry = normalizeOptional(country);
        if (normalizedCountry != null) {
            query.setParameter("country", normalizedCountry);
        }
        if (startDate != null && endDate != null) {
            query.setParameter("startDate", startDate);
            query.setParameter("endDate", endDate);
        }
    }

}
