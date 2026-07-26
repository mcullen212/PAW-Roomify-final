package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.trip.TripContact;

import java.util.List;

public interface TripContactService {
    TripContact addRoomToTrip(long tripId, long contactId, String email);
    List<Contact> getContactsForTrip(long tripId, int page, int pageSize);
    int countContactsForTrip(long tripId);
}
