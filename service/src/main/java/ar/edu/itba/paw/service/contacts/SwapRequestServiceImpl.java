package ar.edu.itba.paw.service.contacts;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.interfaces.exceptions.TripNotFoundException;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.SwapRequestService;
import ar.edu.itba.paw.interfaces.service.TripContactService;
import ar.edu.itba.paw.interfaces.service.TripService;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.Trip;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class SwapRequestServiceImpl implements SwapRequestService {

    private final ContactService contactService;
    private final TripContactService tripContactService;
    private final TripService tripService;

    public SwapRequestServiceImpl(ContactService contactService, TripService tripService, TripContactService tripContactService) {
        this.contactService = contactService;
        this.tripService = tripService;
        this.tripContactService = tripContactService;
    }

    @Override
    @Transactional
    public Contact processSwapRequest(long roomRequestedId, LocalDate startDate, LocalDate endDate, Boolean isSwap, BigDecimal moneyOffer, Long roomOfferedId, Long tripId, String username) {

        boolean swapRequest = Boolean.TRUE.equals(isSwap);

        Contact newContact = contactService.createContact(
                roomRequestedId,
                startDate,
                endDate,
                swapRequest,
                swapRequest ? null : moneyOffer,
                username,
                swapRequest ? roomOfferedId : null
        );

        if (tripId != null) {

            Trip trip = tripService.findTripById(tripId)
                    .orElseThrow(() -> new TripNotFoundException(tripId));

            DateRange tripDates = trip.getDateRange();

            boolean startsAfterTrip = !startDate.isBefore(tripDates.getStartDate());

            boolean endsBeforeTrip = !endDate.isAfter(tripDates.getEndDate());

            if (!startsAfterTrip || !endsBeforeTrip) {
                throw new DateRangeException("Swap dates are outside of range", "specific.errors.swap.dates.outside.trip", trip.getGroupTrip().getId());
            }

            tripContactService.addRoomToTrip(
                    tripId,
                    newContact.getId(),
                    username
            );
        }

        return newContact;
    }
    }
