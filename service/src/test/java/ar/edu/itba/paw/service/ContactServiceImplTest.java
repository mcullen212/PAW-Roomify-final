package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.*;
import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.interfaces.service.RoomAvailabilityService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.interfaces.service.EmailService;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ContactPage;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.ContactView;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.service.contacts.ContactServiceImpl;
import ar.edu.itba.paw.service.reviews.ReviewEligibilityService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ContactServiceImplTest {
    private static final long ROOM_REQUESTED_ID = 10L;
    private static final long ROOM_OFFERED_ID = 20L;
    private static final long CONTACT_ID = 1L;
    private static final long OWNER_ID = 5L;
    private static final long REQUESTER_ID = 6L;
    private static final String REQUESTER_EMAIL = "requester@test.com";
    private static final String OWNER_EMAIL = "owner@test.com";
    private static final SwapStatus PENDING_STATUS  = SwapStatus.PENDING;
    private static final SwapStatus ACCEPTED_STATUS = SwapStatus.ACCEPTED;
    private static final BigDecimal MONEY_OFFER = new BigDecimal("100.00");
    private static final LocalDate START_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate END_DATE = START_DATE.plusDays(5);
    private static final DateRange REQUESTED_RANGE = new DateRange(START_DATE, END_DATE);
    private static final DateRange OFFERED_RANGE = new DateRange(START_DATE.plusDays(2), END_DATE.minusDays(2));
    private static final Locale LOCALE = Locale.ENGLISH;
    private static final long DUMMY_IMAGE_ID = 1L;
    private static final int PAGE_SIZE = 12;
    private static final int DEFAULT_PAGE = 1;
    @Mock
    private ContactDao mockContactDao;
    @Mock
    private UserService mockUserService;
    @Mock
    private RoomService mockRoomService;
    @Mock
    private EmailService mockEmailService;
    @Mock
    private RoomAvailabilityService mockAvailabilityService;
    @Mock
    private ReviewEligibilityService mockReviewEligibilityService;

    @InjectMocks
    private ContactServiceImpl contactService;

    private final User ownerUser = new User(OWNER_ID, "owner@test.com", "Owner", "pass", false, LOCALE.toString(), null, null);
    private final User requesterUser = new User(REQUESTER_ID, REQUESTER_EMAIL, "Requester", "pass", false, LOCALE.toString(), null, null);

    private final Image dummyImage = new Image(DUMMY_IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});

    private final Room roomRequested = new Room(
            ROOM_REQUESTED_ID,
            "Req Room",
            "AR",
            "BA",
            "Desc",
            RoomType.SHARED,      // el enum que quieras
            BedType.TWIN,         // el enum que quieras
            false,                // privateBathroom
            true,                 // privateKitchen
            "Wifi",
            ownerUser,            // el User dueño (ya definido arriba)
            dummyImage,
            BigDecimal.valueOf(100)  // dayPrice dummy
    );
    private final RoomAvailability ra = new RoomAvailability(roomRequested, new DateRange(START_DATE.minusDays(5), END_DATE.plusDays(5)) );

    private final Room roomOffered = new Room(
            ROOM_OFFERED_ID,
            "Off Room",
            "AR",
            "BA",
            "Desc",
            RoomType.SHARED,
            BedType.TWIN,
            false,
            true,
            "Wifi",
            requesterUser,
            dummyImage,
            BigDecimal.valueOf(100)
    );

    private final Contact pendingSwapContact = new Contact(
            CONTACT_ID,
            roomRequested,
            LocalDateTime.now(),
            PENDING_STATUS,
            true,
            null,
            requesterUser,
            roomOffered,
            REQUESTED_RANGE,
            OFFERED_RANGE
    );

    private final Contact pendingMoneyContact = new Contact(
            CONTACT_ID,
            roomRequested,
            LocalDateTime.now(),
            PENDING_STATUS,
            false,
            MONEY_OFFER,
            requesterUser,
            null,
            REQUESTED_RANGE,
            null
    );

    private final Contact acceptedContact = new Contact(
            CONTACT_ID,
            roomRequested,
            LocalDateTime.now(),
            ACCEPTED_STATUS,
            true,
            null,
            requesterUser,
            roomOffered,
            REQUESTED_RANGE,
            OFFERED_RANGE
    );

    private Contact newPendingMoneyContact() {
        return new Contact(
                CONTACT_ID,
                roomRequested,
                LocalDateTime.now(),
                PENDING_STATUS,
                false,
                MONEY_OFFER,
                requesterUser,
                null,
                REQUESTED_RANGE,
                null
        );
    }

    private Contact newPendingSwapContact() {
        return new Contact(
                CONTACT_ID,
                roomRequested,
                LocalDateTime.now(),
                PENDING_STATUS,
                true,
                null,
                requesterUser,
                roomOffered,
                REQUESTED_RANGE,
                null
        );
    }

    private Contact newAcceptedSwapContact(DateRange requestedRange, DateRange offeredRange) {
        return new Contact(
                CONTACT_ID,
                roomRequested,
                LocalDateTime.now(),
                ACCEPTED_STATUS,
                true,
                null,
                requesterUser,
                roomOffered,
                requestedRange,
                offeredRange
        );
    }

    private Contact newExpiredPendingMoneyContact() {
        return new Contact(
                CONTACT_ID,
                roomRequested,
                LocalDateTime.now(),
                PENDING_STATUS,
                false,
                MONEY_OFFER,
                requesterUser,
                null,
                new DateRange(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)),
                null
        );
    }

    @Test
    public void testFindContactsPagePastIncludesPendingReviewFlags() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockContactDao.findPastTripsByEmail(REQUESTER_ID, LocalDate.now(), DEFAULT_PAGE, PAGE_SIZE))
                .thenReturn(List.of(acceptedContact));
        when(mockContactDao.countPastTripsByEmail(REQUESTER_ID, LocalDate.now())).thenReturn(1);
        when(mockReviewEligibilityService.isReviewPending(acceptedContact, requesterUser, LocalDate.now())).thenReturn(true);

        ContactPage page = contactService.findContactsPage(REQUESTER_EMAIL, ContactView.PAST, DEFAULT_PAGE, PAGE_SIZE);

        Assert.assertEquals(1, page.getTotalItems());
        Assert.assertTrue(page.isReviewPending(CONTACT_ID));
    }

    @Test
    public void testFindContactsPageNonPastDoesNotComputePendingReviews() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockContactDao.findRequestedSwapsByUserPage(REQUESTER_ID, DEFAULT_PAGE, PAGE_SIZE))
                .thenReturn(List.of(pendingSwapContact));
        when(mockContactDao.countRequestedSwapsByUser(REQUESTER_ID)).thenReturn(1L);

        ContactPage page = contactService.findContactsPage(REQUESTER_EMAIL, ContactView.SENT, DEFAULT_PAGE, PAGE_SIZE);

        Assert.assertFalse(page.isReviewPending(CONTACT_ID));
        verify(mockReviewEligibilityService, never()).isReviewPending(any(Contact.class), any(User.class), any(LocalDate.class));
    }

    @Test
    public void testCreateContact_MoneyOffer_Successful() {
        // 1. Setup Mocks
        when(mockAvailabilityService.inAvailabilityRanges(
                eq(ROOM_REQUESTED_ID), eq(START_DATE), eq(END_DATE)))
                .thenReturn(true);
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL))
                .thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID))
                .thenReturn(Optional.of(roomRequested));

        when(mockContactDao.create(
                eq(REQUESTER_ID), eq(roomRequested), isNull(), any(LocalDateTime.class), eq(PENDING_STATUS),
                eq(false), eq(MONEY_OFFER),any(DateRange.class), isNull()))
                .thenReturn(pendingMoneyContact);
        // 2. Exercise
        Contact result = contactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null);

        // 3. Assertions
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isSwap());
        Assert.assertEquals(MONEY_OFFER, result.getMoneyOffer());
        Assert.assertEquals(Long.valueOf(ROOM_REQUESTED_ID), result.getRoomRequested().getId());
    }

    @Test
    public void testCreateContact_SwapOffer_Successful() {
        // 1. Setup Mocks
        LocalDate availableStart = LocalDate.now().minusDays(5);
        LocalDate availableEnd = LocalDate.now().plusDays(20);
        DateRange offeredAvailability = new DateRange(availableStart, availableEnd);

        when(mockAvailabilityService.inAvailabilityRanges(
                eq(ROOM_REQUESTED_ID), eq(START_DATE), eq(END_DATE)))
                .thenReturn(true);
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL))
                .thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID))
                .thenReturn(Optional.of(roomRequested));
        when(mockRoomService.findRoomById(ROOM_OFFERED_ID))
                .thenReturn(Optional.of(roomOffered));

        when(mockContactDao.contactAcceptedDatesForRoom(ROOM_OFFERED_ID, DEFAULT_PAGE, PAGE_SIZE)).thenReturn(Collections.emptyList());
        when(mockContactDao.contactAcceptedDatesForRoomOffered(ROOM_OFFERED_ID, DEFAULT_PAGE, PAGE_SIZE)).thenReturn(Collections.emptyList());
        when(mockAvailabilityService.getAvailabilityDatesForRoom(ROOM_OFFERED_ID)).thenReturn(offeredAvailability);
        when(mockContactDao.create(
                eq(REQUESTER_ID), eq(roomRequested), eq(roomOffered), any(LocalDateTime.class), eq(PENDING_STATUS),
                eq(true), isNull(), any(DateRange.class), isNull()))
                .thenReturn(pendingSwapContact);

        // 2. Exercise
        Contact result = contactService.createContact(
                ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, REQUESTER_EMAIL, ROOM_OFFERED_ID);

        // 3. Assertions
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isSwap());
        Assert.assertEquals(Long.valueOf(ROOM_OFFERED_ID), result.getRoomOffered().getId());
    }

    @Test
    public void testCreateContact_BookedDate() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));

        assertThrows(
                BookedDateException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null)
        );
    }

    @Test
    public void testCreateContact_RoomNotFound_ThrowsException() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.empty());

        assertThrows(
                RoomNotFoundException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_MoneyOfferNull_ThrowsException() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));

        assertThrows(
                SwapException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, null, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_SameCheckInAndCheckOut_ThrowsException() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));

        assertThrows(
                BookedDateException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, START_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).replySameSwapMoney(anyLong(), any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class));
        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_CheckOutBeforeCheckIn_ThrowsException() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));

        assertThrows(
                BookedDateException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, END_DATE, START_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).replySameSwapMoney(anyLong(), any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class));
        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_MoneyOfferDuplicate_ThrowsException() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));
        when(mockContactDao.replySameSwapMoney(ROOM_REQUESTED_ID, START_DATE, END_DATE, MONEY_OFFER)).thenReturn(true);

        assertThrows(
                BookedDateException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_SwapMissingOfferedRoom_ThrowsException() {
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));
        when(mockAvailabilityService.inAvailabilityRanges(ROOM_REQUESTED_ID, START_DATE, END_DATE)).thenReturn(true);

        assertThrows(
                SwapException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_SwapOfferedRoomNotOwned_ThrowsException() {
        Room otherUserRoom = new Room(
                ROOM_OFFERED_ID,
                "Other Room",
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

        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(roomRequested));
        when(mockAvailabilityService.inAvailabilityRanges(ROOM_REQUESTED_ID, START_DATE, END_DATE)).thenReturn(true);
        when(mockRoomService.findRoomById(ROOM_OFFERED_ID)).thenReturn(Optional.of(otherUserRoom));

        assertThrows(
                SwapException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, true, null, REQUESTER_EMAIL, ROOM_OFFERED_ID)
        );

        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }

    @Test
    public void testCreateContact_RequestedRoomOwnedByUser_ThrowsException() {
        Room ownRequestedRoom = new Room(
                ROOM_REQUESTED_ID,
                "Own Room",
                "AR",
                "BA",
                "Desc",
                RoomType.SHARED,
                BedType.TWIN,
                false,
                true,
                "Wifi",
                requesterUser,
                dummyImage,
                BigDecimal.valueOf(100)
        );

        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockRoomService.findRoomById(ROOM_REQUESTED_ID)).thenReturn(Optional.of(ownRequestedRoom));
        when(mockAvailabilityService.inAvailabilityRanges(ROOM_REQUESTED_ID, START_DATE, END_DATE)).thenReturn(true);

        assertThrows(
                BookedDateException.class,
                () -> contactService.createContact(
                        ROOM_REQUESTED_ID, START_DATE, END_DATE, false, MONEY_OFFER, REQUESTER_EMAIL, null)
        );

        verify(mockContactDao, never()).create(
                anyLong(), any(Room.class), any(Room.class), any(LocalDateTime.class), any(SwapStatus.class),
                anyBoolean(), any(BigDecimal.class), any(DateRange.class), any(DateRange.class));
    }


    @Test
    public void testAcceptedContact_Money_Successful() {
        Contact contact = newPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockContactDao.acceptMoneyOffer(CONTACT_ID)).thenReturn(contact);
        when(mockContactDao.findPendingRequestedOverlap(CONTACT_ID)).thenReturn(Collections.emptyList());

        Contact result = contactService.acceptedContact(CONTACT_ID, null, null);

        Assert.assertNotNull(result);
        verify(mockContactDao).acceptMoneyOffer(CONTACT_ID);
    }

    @Test
    public void testAcceptedContact_Money_NotifiesDeletedOverlappingPendingRequester() {
        long rejectedRequesterId = 99L;
        User rejectedRequester = new User(rejectedRequesterId, "rejected@test.com", "Rejected", "pass", false, LOCALE.toString(), null, null);
        Contact contact = newPendingMoneyContact();
        DateRange deletedContactRange = new DateRange(START_DATE.plusDays(1), END_DATE.plusDays(1));
        Contact deletedContact = new Contact(
                77L,
                roomRequested,
                LocalDateTime.now(),
                PENDING_STATUS,
                false,
                MONEY_OFFER,
                rejectedRequester,
                null,
                deletedContactRange,
                null
        );
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockContactDao.acceptMoneyOffer(CONTACT_ID)).thenReturn(contact);
        when(mockContactDao.findPendingRequestedOverlap(CONTACT_ID)).thenReturn(List.of(deletedContact));

        contactService.acceptedContact(CONTACT_ID, null, null);

        verify(mockEmailService).sendSwapOwnerAcceptedOtherOffer(
                rejectedRequester,
                ownerUser,
                roomRequested,
                deletedContactRange
        );
    }

    @Test
    public void testAcceptedContact_Swap_MaxDaysExceededException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingSwapContact()));

        assertThrows(
                MaxDaysExceededException.class,
                () -> contactService.acceptedContact(CONTACT_ID, START_DATE, END_DATE)
        );
    }

    @Test
    public void testRejectedContact_Successful() {
        Contact contact = newPendingSwapContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));

        contactService.rejectedContact(CONTACT_ID);

        verify(mockContactDao).updateStatus(CONTACT_ID, SwapStatus.REJECTED);
        Assert.assertEquals(SwapStatus.REJECTED, contact.getStatus());
    }

    @Test
    public void testUpdateContactStatus_AcceptMoneyOffer_Successful() {
        Contact contact = newPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactDao.acceptMoneyOffer(CONTACT_ID)).thenReturn(contact);
        when(mockContactDao.findPendingRequestedOverlap(CONTACT_ID)).thenReturn(Collections.emptyList());

        Contact result = contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, null, null, OWNER_EMAIL);

        Assert.assertSame(contact, result);
        verify(mockContactDao).acceptMoneyOffer(CONTACT_ID);
        verify(mockContactDao, never()).confirmedDateRange(anyLong(), any(DateRange.class));
    }

    @Test
    public void testUpdateContactStatus_AcceptSwapWithDates_Successful() {
        Contact contact = newPendingSwapContact();
        DateRange offeredRange = new DateRange(START_DATE, END_DATE);
        Contact updated = newPendingSwapContact();
        updated.setStatus(SwapStatus.ACCEPTED);
        updated.setOfferedRange(offeredRange);

        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactDao.getStayDurationDays(CONTACT_ID)).thenReturn(10);
        when(mockContactDao.confirmedDateRange(eq(CONTACT_ID), any(DateRange.class))).thenReturn(updated);
        when(mockContactDao.findPendingRequestedOverlap(CONTACT_ID)).thenReturn(Collections.emptyList());

        Contact result = contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, START_DATE, END_DATE, OWNER_EMAIL);

        Assert.assertEquals(SwapStatus.ACCEPTED, result.getStatus());
        Assert.assertEquals(START_DATE, result.getOfferedRange().getStartDate());
        Assert.assertEquals(END_DATE, result.getOfferedRange().getEndDate());
        verify(mockContactDao).confirmedDateRange(eq(CONTACT_ID), any(DateRange.class));
    }

    @Test
    public void testUpdateContactStatus_RejectPendingContact_Successful() {
        Contact contact = newPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        Contact result = contactService.updateContactStatus(CONTACT_ID, SwapStatus.REJECTED, null, null, OWNER_EMAIL);

        Assert.assertEquals(SwapStatus.REJECTED, result.getStatus());
        verify(mockContactDao).updateStatus(CONTACT_ID, SwapStatus.REJECTED);
    }

    @Test
    public void testUpdateContactStatus_AcceptExpiredPendingContact_MarksExpiredAndThrowsInvalidContactStateException() {
        Contact contact = newExpiredPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactStateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, null, null, OWNER_EMAIL)
        );

        Assert.assertEquals(SwapStatus.EXPIRED, contact.getStatus());
        verify(mockContactDao).updateStatus(CONTACT_ID, SwapStatus.EXPIRED);
        verify(mockContactDao, never()).acceptMoneyOffer(anyLong());
    }

    @Test
    public void testUpdateContactStatus_RejectExpiredPendingContact_MarksExpiredAndThrowsInvalidContactStateException() {
        Contact contact = newExpiredPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactStateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.REJECTED, null, null, OWNER_EMAIL)
        );

        Assert.assertEquals(SwapStatus.EXPIRED, contact.getStatus());
        verify(mockContactDao).updateStatus(CONTACT_ID, SwapStatus.EXPIRED);
        verify(mockContactDao, never()).updateStatus(CONTACT_ID, SwapStatus.REJECTED);
    }

    @Test
    public void testUpdateContactStatus_CancelExpiredPendingContact_MarksExpiredAndThrowsInvalidContactStateException() {
        Contact contact = newExpiredPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));

        assertThrows(
                InvalidContactStateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, REQUESTER_EMAIL)
        );

        Assert.assertEquals(SwapStatus.EXPIRED, contact.getStatus());
        verify(mockContactDao).updateStatus(CONTACT_ID, SwapStatus.EXPIRED);
        verify(mockContactDao, never()).cancelSwap(anyLong());
    }

    @Test
    public void testUpdateContactStatus_CancelPendingContactByOwner_Successful() {
        Contact contact = newPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        Contact result = contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, OWNER_EMAIL);

        Assert.assertEquals(SwapStatus.CANCELED, result.getStatus());
        verify(mockContactDao).cancelSwap(CONTACT_ID);
        verify(mockEmailService).sendSwapCancellationNotification(requesterUser, ownerUser, roomRequested, PENDING_STATUS);
    }

    @Test
    public void testUpdateContactStatus_CancelPendingContactByRequester_Successful() {
        Contact contact = newPendingMoneyContact();
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));

        Contact result = contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, REQUESTER_EMAIL);

        Assert.assertEquals(SwapStatus.CANCELED, result.getStatus());
        verify(mockContactDao).cancelSwap(CONTACT_ID);
        verify(mockEmailService).sendSwapCancellationNotification(ownerUser, requesterUser, roomRequested, PENDING_STATUS);
    }

    @Test
    public void testUpdateContactStatus_CancelAcceptedContactMoreThanSevenDaysBefore_Successful() {
        Contact contact = newAcceptedSwapContact(
                new DateRange(LocalDate.now().plusDays(12), LocalDate.now().plusDays(15)),
                new DateRange(LocalDate.now().plusDays(13), LocalDate.now().plusDays(16))
        );
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        Contact result = contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, OWNER_EMAIL);

        Assert.assertEquals(SwapStatus.CANCELED, result.getStatus());
        verify(mockContactDao).cancelSwap(CONTACT_ID);
        verify(mockEmailService).sendSwapCancellationNotification(requesterUser, ownerUser, roomRequested, ACCEPTED_STATUS);
    }

    @Test
    public void testUpdateContactStatus_ContactNotFound_ThrowsException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.empty());

        assertThrows(
                ContactNotFoundException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, null, null, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_NonOwner_ThrowsForbiddenUserOperationException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingMoneyContact()));
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));

        assertThrows(
                ForbiddenUserOperationException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, null, null, REQUESTER_EMAIL)
        );

        verify(mockContactDao, never()).acceptMoneyOffer(anyLong());
        verify(mockContactDao, never()).updateStatus(anyLong(), any(SwapStatus.class));
    }

    @Test
    public void testUpdateContactStatus_NonPendingContact_ThrowsInvalidContactStateException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(acceptedContact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactStateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.REJECTED, null, null, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_UnsupportedStatus_ThrowsInvalidContactUpdateException() {
        assertThrows(
                InvalidContactUpdateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.EXPIRED, null, null, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_CancelNonParticipant_ThrowsForbiddenUserOperationException() {
        User strangerUser = new User(999L, "stranger@test.com", "Stranger", "pass", false, LOCALE.toString(), null, null);
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingMoneyContact()));
        when(mockUserService.findUserByEmail("stranger@test.com")).thenReturn(Optional.of(strangerUser));

        assertThrows(
                ForbiddenUserOperationException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, "stranger@test.com")
        );

        verify(mockContactDao, never()).cancelSwap(anyLong());
    }

    @Test
    public void testUpdateContactStatus_CancelRejectedContact_ThrowsInvalidContactStateException() {
        Contact contact = newPendingMoneyContact();
        contact.setStatus(SwapStatus.REJECTED);
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactStateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, OWNER_EMAIL)
        );

        verify(mockContactDao, never()).cancelSwap(anyLong());
    }

    @Test
    public void testUpdateContactStatus_CancelAcceptedContactTooLate_ThrowsCancelException() {
        Contact contact = newAcceptedSwapContact(
                new DateRange(LocalDate.now().plusDays(3), LocalDate.now().plusDays(6)),
                new DateRange(LocalDate.now().plusDays(4), LocalDate.now().plusDays(7))
        );
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(contact));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                CancelException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, null, null, OWNER_EMAIL)
        );

        verify(mockContactDao, never()).cancelSwap(anyLong());
    }

    @Test
    public void testUpdateContactStatus_CancelWithDates_ThrowsInvalidContactUpdateException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingMoneyContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactUpdateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.CANCELED, START_DATE, null, OWNER_EMAIL)
        );

        verify(mockContactDao, never()).cancelSwap(anyLong());
    }

    @Test
    public void testUpdateContactStatus_AcceptSwapMissingDates_ThrowsInvalidContactUpdateException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingSwapContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactUpdateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, START_DATE, null, OWNER_EMAIL)
        );

        verify(mockContactDao, never()).confirmedDateRange(anyLong(), any(DateRange.class));
    }

    @Test
    public void testUpdateContactStatus_AcceptSwapInvalidRange_ThrowsInvalidContactUpdateException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingSwapContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactUpdateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, END_DATE, START_DATE, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_AcceptSwapPastStart_ThrowsInvalidContactUpdateException() {
        LocalDate pastStart = LocalDate.now().minusDays(1);
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingSwapContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactUpdateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, pastStart, END_DATE, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_AcceptSwapMaxDaysExceeded_ThrowsMaxDaysExceededException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingSwapContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactDao.getStayDurationDays(CONTACT_ID)).thenReturn(1);

        assertThrows(
                MaxDaysExceededException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, START_DATE, END_DATE, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_AcceptSwapBookedDates_ThrowsBookedDateException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingSwapContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactDao.getStayDurationDays(CONTACT_ID)).thenReturn(10);
        when(mockContactDao.hasAcceptedContactInRangeRequestedSide(eq(ROOM_OFFERED_ID), any(DateRange.class))).thenReturn(true);

        assertThrows(
                BookedDateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, START_DATE, END_DATE, OWNER_EMAIL)
        );
    }

    @Test
    public void testUpdateContactStatus_AcceptMoneyWithDates_ThrowsInvalidContactUpdateException() {
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(newPendingMoneyContact()));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));

        assertThrows(
                InvalidContactUpdateException.class,
                () -> contactService.updateContactStatus(CONTACT_ID, SwapStatus.ACCEPTED, START_DATE, END_DATE, OWNER_EMAIL)
        );

        verify(mockContactDao, never()).acceptMoneyOffer(anyLong());
    }

    @Test
    public void testUserIsGoingTo_MoneyOffer_Requester() {
        // 1. Setup Mocks
        when(mockUserService.findUserByEmail(REQUESTER_EMAIL)).thenReturn(Optional.of(requesterUser));
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(pendingMoneyContact));
        when(mockContactDao.isSwap(CONTACT_ID)).thenReturn(false);

        // 2. Exercise
        Room result = contactService.userIsGoingTo(CONTACT_ID, REQUESTER_EMAIL);

        // 3. Assertions (Requester goes to the requested room)
        Assert.assertEquals(Long.valueOf(ROOM_REQUESTED_ID), result.getId());
    }


    @Test
    public void testUserIsGoingTo_NotGuestOrHost() {
        // 1. Setup Mocks
        User thirdUser = new User(99L, "third@test.com", "Third", "pass", false, LOCALE.toString(), null, null);
        when(mockUserService.findUserByEmail(anyString())).thenReturn(Optional.of(thirdUser));
        when(mockContactDao.findContactById(CONTACT_ID)).thenReturn(Optional.of(pendingSwapContact));

        assertThrows(
                ForbiddenUserOperationException.class,
                () -> contactService.userIsGoingTo(CONTACT_ID, "third@test.com")
        );
    }
}
