package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.RoomAvailabilityDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import org.springframework.stereotype.Repository;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;

@Repository
public class RoomAvailabilityJpaDao implements RoomAvailabilityDao {

    @PersistenceContext
    private EntityManager em;

    @Override
    public RoomAvailability create(long roomId, DateRange range) {
        Room roomRef = em.getReference(Room.class, roomId);
        final RoomAvailability ra = new RoomAvailability(roomRef, range);
        em.persist(ra);
        return ra;
    }

    @Override
    public List<RoomAvailability> findByRoom(long roomId) {
        final TypedQuery<RoomAvailability> query = em.createQuery("FROM RoomAvailability as ra WHERE ra.room.id = :roomId ORDER BY ra.range.startDate", RoomAvailability.class);
        query.setParameter("roomId", roomId);
        return query.getResultList();
    }

    @Override
    public List<RoomAvailability> findByRoomBetween(long roomId, LocalDate startDate, LocalDate endDate) {
        return em.createQuery("""
                FROM RoomAvailability as ra
                WHERE ra.room.id = :roomId
                  AND ra.range.startDate <= :endDate
                  AND ra.range.endDate >= :startDate
                ORDER BY ra.range.startDate, ra.id
                """, RoomAvailability.class)
                .setParameter("roomId", roomId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getResultList();
    }

    @Override
    public Long findAvailableIdByRoom(long roomId) {
        final TypedQuery<Long> query = em.createQuery("SELECT ra.id FROM RoomAvailability as ra WHERE ra.room.id = :roomId ORDER BY ra.range.startDate", Long.class);
        query.setParameter("roomId", roomId);
        return query.getResultList().stream().findAny().orElse(null);
    }

    @Override
    public boolean inAvailabilityRanges(long roomRequestedId, LocalDate startDate, LocalDate endDate){
        Long count = em.createQuery(
                        "SELECT COUNT(ra) " +
                                "FROM RoomAvailability ra " +
                                "WHERE ra.room.id = :roomId " +
                                "AND ra.range.startDate <= :startDate " +
                                "AND ra.range.endDate >= :endDate", Long.class)
                .setParameter("roomId", roomRequestedId)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

        return count > 0;
    }

}
