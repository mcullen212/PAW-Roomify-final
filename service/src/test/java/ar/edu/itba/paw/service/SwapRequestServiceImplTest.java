package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.interfaces.exceptions.TripNotFoundException;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.TripContactService;
import ar.edu.itba.paw.interfaces.service.TripService;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.service.contacts.SwapRequestServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SwapRequestServiceImplTest {

    private static final long CONTACT_ID = 1L;
    private static final long ROOM_REQUESTED_ID = 10L;
    private static final long ROOM_OFFERED_ID = 20L;
    private static final long OWNER_ID = 5L;
    private static final long REQUESTER_ID = 6L;
    private static final long TRIP_ID = 30L;
    private static final String REQUESTER_EMAIL = "requester@test.com";
    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 3);
    private static final LocalDate END_DATE = LocalDate.of(2026, 3, 6);
    private static final BigDecimal MONEY_OFFER = new BigDecimal("150.00");
    private static final Locale LOCALE = Locale.ENGLISH;

    @Mock
    private ContactService mockContactService;
    @Mock
    private TripService mockTripService;
    @Mock
    private TripContactService mockTripContactService;

    @InjectMocks
    private SwapRequestServiceImpl swapRequestService;

    private final User ownerUser = new User(OWNER_ID, "owner@test.com", "Owner", "pass", false, LOCALE.toString(), null, null);
    private final User requesterUser = new User(REQUESTER_ID, REQUESTER_EMAIL, "Requester", "pass", false, LOCALE.toString(), null, null);
    private final Image dummyImage = new Image(1L, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final Room roomRequested = new Room(
            ROOM_REQUESTED_ID,
            "Req Room",
            "AR",
            "BA",
            "Desc",
            RoomType.SHARED,
            BedType.TWIN,
            false,
            true,
            "Wifi",
            ownerUser,
            dummyImage,
            BigDecimal.valueOf(100)
    );
    private final Contact contact = new Contact(
            CONTACT_ID,
            roomRequested,
            LocalDateTime.of(2026, 2, 1, 10, 0),
            SwapStatus.PENDING,
            false,
            MONEY_OFFER,
            requesterUser,
            null,
            new DateRange(START_DATE, END_DATE),
            null
    );

    @Test
    public void testProcessSwapRequest_WithoutTrip_Successful() {
        when(mockContactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null
        )).thenReturn(contact);

        Contact result = swapRequestService.processSwapRequest(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, null, null, REQUESTER_EMAIL
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(CONTACT_ID, result.getId());
        verify(mockTripService, never()).findTripById(TRIP_ID);
        verify(mockTripContactService, never()).addRoomToTrip(TRIP_ID, CONTACT_ID, REQUESTER_EMAIL);
    }

    @Test
    public void testProcessSwapRequest_WithTrip_Successful() {
        Trip trip = buildTrip(new DateRange(START_DATE.minusDays(1), END_DATE.plusDays(1)));
        when(mockContactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, REQUESTER_EMAIL, ROOM_OFFERED_ID
        )).thenReturn(contact);
        when(mockTripService.findTripById(TRIP_ID)).thenReturn(Optional.of(trip));

        Contact result = swapRequestService.processSwapRequest(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, ROOM_OFFERED_ID, TRIP_ID, REQUESTER_EMAIL
        );

        Assert.assertNotNull(result);
        Assert.assertEquals(CONTACT_ID, result.getId());
        verify(mockTripContactService).addRoomToTrip(TRIP_ID, CONTACT_ID, REQUESTER_EMAIL);
    }

    @Test
    public void testProcessSwapRequest_SwapWithMoneyOffer_IgnoresMoneyOffer() {
        when(mockContactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, REQUESTER_EMAIL, ROOM_OFFERED_ID
        )).thenReturn(contact);

        Contact result = swapRequestService.processSwapRequest(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, true, MONEY_OFFER, ROOM_OFFERED_ID, null, REQUESTER_EMAIL
        );

        Assert.assertNotNull(result);
        verify(mockContactService).createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, REQUESTER_EMAIL, ROOM_OFFERED_ID
        );
    }

    @Test
    public void testProcessSwapRequest_MoneyOfferWithOfferedRoom_IgnoresOfferedRoom() {
        when(mockContactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null
        )).thenReturn(contact);

        Contact result = swapRequestService.processSwapRequest(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, ROOM_OFFERED_ID, null, REQUESTER_EMAIL
        );

        Assert.assertNotNull(result);
        verify(mockContactService).createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null
        );
    }

    @Test
    public void testProcessSwapRequest_TripNotFound_ThrowsException() {
        when(mockContactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null
        )).thenReturn(contact);
        when(mockTripService.findTripById(TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> swapRequestService.processSwapRequest(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, null, TRIP_ID, REQUESTER_EMAIL
                )
        );

        verify(mockTripContactService, never()).addRoomToTrip(TRIP_ID, CONTACT_ID, REQUESTER_EMAIL);
    }

    @Test
    public void testProcessSwapRequest_DatesOutsideTrip_ThrowsException() {
        Trip trip = buildTrip(new DateRange(START_DATE.minusDays(10), START_DATE.minusDays(1)));
        when(mockContactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null
        )).thenReturn(contact);
        when(mockTripService.findTripById(TRIP_ID)).thenReturn(Optional.of(trip));

        assertThrows(
                DateRangeException.class,
                () -> swapRequestService.processSwapRequest(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, null, TRIP_ID, REQUESTER_EMAIL
                )
        );

        verify(mockTripContactService, never()).addRoomToTrip(TRIP_ID, CONTACT_ID, REQUESTER_EMAIL);
    }

    private Trip buildTrip(DateRange tripRange) {
        GroupTrip groupTrip = new GroupTrip(requesterUser, "Group trip", tripRange);
        return new Trip(groupTrip, "Argentina", tripRange);
    }
}
