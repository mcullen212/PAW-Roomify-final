package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.persistence.TripContactDao;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripContact;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Repository
public class TripContactJpaDao implements TripContactDao {
    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<TripContact> findTripContactById(long id) {
        return Optional.ofNullable(em.find(TripContact.class, id));
    }

    @Override
    public TripContact create(long tripId, long contactId, long roomId){
        Trip trip = em.find(Trip.class, tripId);
        Contact contact = em.find(Contact.class, contactId);
        Room room = em.find(Room.class, roomId);

        if (trip == null || contact == null || room == null) {
            throw new IllegalArgumentException("Trip, Contact or Room not found.");
        }

        TripContact tripContact = new TripContact(trip, contact, room);

        em.persist(tripContact);

        return tripContact;
    }

    @Override
    public List<TripContact> tripContactList(long tripId, int page, int pageSize){
        Query idQuery = em.createNativeQuery("""
                SELECT tc.id
                FROM trip_contact tc
                JOIN contact c ON c.id = tc.contact_id
                WHERE tc.trip_id = :tripId
                ORDER BY c.contact_date DESC, tc.id DESC
                LIMIT :limit OFFSET :offset
                """)
                .setParameter("tripId", tripId)
                .setParameter("limit", pageSize)
                .setParameter("offset", (page - 1) * pageSize);
        List<Long> pageIds = toLongIds(idQuery.getResultList());
        if (pageIds.isEmpty()) return List.of();
        return em.createQuery("""
                        SELECT tc FROM TripContact tc
                        JOIN FETCH tc.trip
                        JOIN FETCH tc.contact c
                        JOIN FETCH c.roomRequested rr
                        JOIN FETCH rr.owner
                        LEFT JOIN FETCH c.roomOffered ro
                        LEFT JOIN FETCH ro.owner
                        LEFT JOIN FETCH c.offerUser
                        WHERE tc.id IN :ids
                        ORDER BY c.contactDate DESC, tc.id DESC
                        """, TripContact.class)
                .setParameter("ids", pageIds)
                .getResultList();
    }

    @Override
    public int countByTripId(long tripId) {
        TypedQuery<Long> query = em.createQuery(
                "SELECT COUNT(tc.id) FROM TripContact tc WHERE tc.trip.id = :tripId",
                Long.class
        );
        query.setParameter("tripId", tripId);
        return query.getSingleResult().intValue();
    }

    private List<Long> toLongIds(List<?> rawIds) {
        return rawIds.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }

}
