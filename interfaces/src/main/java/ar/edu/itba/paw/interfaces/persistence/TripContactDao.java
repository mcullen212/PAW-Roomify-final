package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.trip.TripContact;

import java.util.List;
import java.util.Optional;

public interface TripContactDao {
    Optional<TripContact> findTripContactById(long id);
    TripContact create(long tripId, long contactId, long roomId);
    List<TripContact> tripContactList(long tripId, int page, int pageSize);
    int countByTripId(long tripId);
}
