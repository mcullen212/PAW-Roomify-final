package ar.edu.itba.paw.service.trips;

import ar.edu.itba.paw.interfaces.persistence.TripContactDao;
import ar.edu.itba.paw.interfaces.service.*;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripContact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TripContactServiceImpl implements TripContactService {
    private final static Logger LOGGER = LoggerFactory.getLogger(TripContactServiceImpl.class);
    private final TripContactDao tripContactDao;
    private final ContactService contactService;
    private final UserService userService;
    private final TripService tripService;


    public TripContactServiceImpl(final TripContactDao tripContactDao, final UserService userService,
                                final ContactService contactService, final TripService tripService) {
        this.tripContactDao = tripContactDao;
        this.contactService = contactService;
        this.userService = userService;
        this.tripService = tripService;
    }

    @Transactional
    @Override
    public TripContact addRoomToTrip(long tripId, long contactId, String email) {
        Contact contact = contactService.getContactById(contactId).orElseThrow(()-> new IllegalArgumentException("Contact not found"));

        if(contact.getStatus() != SwapStatus.ACCEPTED && contact.getStatus() != SwapStatus.PENDING ){
            throw new IllegalArgumentException("Contact has not been accepted and cannot be added to a trip plan.");
        }

        User user = userService.findUserByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Trip trip = tripService.findTripById(tripId).orElseThrow(()-> new IllegalArgumentException("Trip not found"));

        long roomLink = 0;

        /*
         * 1. Requester: The user who is the owner of 'roomRequested'. This user is planning to receive 'roomOffered'.
         * 2. Offerer: The user who is the 'offerUser'. This user is planning to receive 'roomRequested'.
         */

        // If the user owns the requested room, they are receiving the offered room (roomOffered) in the swap/deal.
        if (contact.isSwap() && contact.getRoomRequested().getOwner().getId() == user.getId() && contact.getRoomOffered() != null) {
            roomLink = contact.getRoomOffered().getId();
        }

        // The offer user is receiving the requested room (roomRequested) in the swap/deal.
        if (contact.getOfferUser() != null && contact.getOfferUser().getId() == user.getId()) {
            roomLink = contact.getRoomRequested().getId();
        }

        if(roomLink == 0){
            throw new IllegalStateException("Room link could not be determined.");
        }

        return tripContactDao.create(tripId, contactId, roomLink);
    }

    @Override
    public List<Contact> getContactsForTrip(long tripId, int page, int pageSize) {
        tripService.findTripById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + tripId));

        return tripContactDao.tripContactList(tripId, page, pageSize)
                .stream()
                .map(TripContact::getContact)
                .toList();
    }

    @Override
    public int countContactsForTrip(long tripId){
        tripService.findTripById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found with ID: " + tripId));

        return tripContactDao.countByTripId(tripId);
    }
}
