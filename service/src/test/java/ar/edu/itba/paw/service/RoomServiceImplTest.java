package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.ForbiddenUserOperationException;
import ar.edu.itba.paw.interfaces.exceptions.RoomHasActiveSwapsException;
import ar.edu.itba.paw.interfaces.exceptions.RoomNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.RoomValidationException;
import ar.edu.itba.paw.interfaces.persistence.ContactDao;
import ar.edu.itba.paw.interfaces.persistence.ReviewDao;
import ar.edu.itba.paw.interfaces.persistence.RoomDao;
import ar.edu.itba.paw.interfaces.service.ImageService;
import ar.edu.itba.paw.interfaces.service.RoomAvailabilityService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.interfaces.service.EmailService;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.DTO.RoomCardResult;
import ar.edu.itba.paw.model.DTO.RoomCreateRequest;
import ar.edu.itba.paw.model.DTO.RoomCreationResult;
import ar.edu.itba.paw.model.DTO.RoomReviewStats;
import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.SwapStatus;
import ar.edu.itba.paw.service.rooms.RoomServiceImpl;
import org.mockito.InOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RoomServiceImplTest {
    private static final long ROOM_ID = 100L;
    private static final long OWNER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long IMAGE_ID = 5L;
    private static final String OWNER_EMAIL = "owner@test.com";
    private static final String OTHER_EMAIL = "other@test.com";
    private static final String NON_EXISTENT_EMAIL = "missing@test.com";
    private static final LocalDate START_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate END_DATE = START_DATE.plusDays(5);

    @Mock
    private RoomDao mockRoomDao;
    @Mock
    private UserService mockUserService;
    @Mock
    private RoomAvailabilityService mockRoomAvailabilityService;
    @Mock
    private ImageService mockImageService;
    @Mock
    private ContactDao mockContactDao;
    @Mock
    private ReviewDao mockReviewDao;
    @Mock
    private EmailService mockEmailService;

    @InjectMocks
    private RoomServiceImpl roomService;

    private final Image dummyImage = new Image(IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final User ownerUser = new User(OWNER_ID, OWNER_EMAIL, "Owner", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final User otherUser = new User(OTHER_USER_ID, OTHER_EMAIL, "Other", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final Image ownedImage = new Image(IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3}, ownerUser, null);
    private final Image otherUserImage = new Image(IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3}, otherUser, null);
    private final Room ownedRoom = new Room(ROOM_ID, "Room", "AR", "BA", "Desc", RoomType.SHARED, BedType.TWIN,
            false, true, "Wifi", ownerUser, dummyImage, BigDecimal.valueOf(100));
    private final Room otherRoom = new Room(ROOM_ID + 1, "Other room", "AR", "BA", "Desc", RoomType.SHARED, BedType.TWIN,
            false, true, "Wifi", otherUser, dummyImage, BigDecimal.valueOf(100));
    private final RoomAvailability availability = new RoomAvailability(1L, ownedRoom, new DateRange(START_DATE, END_DATE));

    private RoomCreateRequest validCreateRequest(Long imageId) {
        return new RoomCreateRequest(
                "Room",
                "Argentina",
                "Buenos Aires",
                "Desc",
                "SHARED",
                "TWIN",
                false,
                true,
                List.of("WIFI"),
                List.of(new DateRange(START_DATE, END_DATE)),
                BigDecimal.valueOf(100),
                imageId
        );
    }

    @Test
    public void createRoomWithAvailabilityUsesOwnedImageId() {
        when(mockUserService.findUserById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(mockImageService.findOwnedUnassignedImage(IMAGE_ID, OWNER_ID)).thenReturn(Optional.of(ownedImage));
        when(mockRoomDao.create(eq(OWNER_ID), eq("Room"), eq("Argentina"), eq("Buenos Aires"), eq("Desc"),
                eq(RoomType.SHARED), eq(BedType.TWIN), eq(false), eq(true), eq("[\"WiFi\"]"),
                eq(IMAGE_ID), eq(BigDecimal.valueOf(100)))).thenReturn(ownedRoom);
        when(mockRoomAvailabilityService.addAvailability(eq(ROOM_ID), any(DateRange.class))).thenReturn(availability);

        RoomCreationResult result = roomService.createRoomWithAvailability(OWNER_ID, validCreateRequest(IMAGE_ID));

        assertSame(ownedRoom, result.getRoom());
        assertEquals(1, result.getAvailabilities().size());
        verify(mockImageService, never()).deleteIfUnassigned(IMAGE_ID);
    }

    @Test
    public void createRoomWithAvailabilityFailsWhenImageIdIsNull() {
        RoomValidationException exception = assertThrows(RoomValidationException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, validCreateRequest(null)));

        assertEquals("room.image.notNull", exception.getLocalizedMessage());
        verify(mockRoomDao, never()).create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any());
        verify(mockImageService, never()).deleteIfUnassigned(anyLong());
    }

    @Test
    public void createRoomWithAvailabilityFailsWhenImageDoesNotExist() {
        when(mockUserService.findUserById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(mockImageService.findOwnedUnassignedImage(IMAGE_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(mockImageService.findImageById(IMAGE_ID)).thenReturn(Optional.empty());

        RoomValidationException exception = assertThrows(RoomValidationException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, validCreateRequest(IMAGE_ID)));

        assertEquals("room.image.notFound", exception.getLocalizedMessage());
        verify(mockImageService, never()).deleteIfUnassigned(IMAGE_ID);
        verify(mockRoomDao, never()).create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any());
    }

    @Test
    public void createRoomWithAvailabilityFailsWhenImageBelongsToOtherUser() {
        when(mockUserService.findUserById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(mockImageService.findOwnedUnassignedImage(IMAGE_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(mockImageService.findImageById(IMAGE_ID)).thenReturn(Optional.of(otherUserImage));

        assertThrows(ForbiddenUserOperationException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, validCreateRequest(IMAGE_ID)));

        verify(mockImageService, never()).deleteIfUnassigned(IMAGE_ID);
        verify(mockRoomDao, never()).create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any());
    }

    @Test
    public void createRoomWithAvailabilityFailsWhenImageIsAlreadyAssigned() {
        when(mockUserService.findUserById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(mockImageService.findOwnedUnassignedImage(IMAGE_ID, OWNER_ID)).thenReturn(Optional.empty());
        when(mockImageService.findImageById(IMAGE_ID)).thenReturn(Optional.of(ownedImage));

        RoomValidationException exception = assertThrows(RoomValidationException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, validCreateRequest(IMAGE_ID)));

        assertEquals("room.image.alreadyAssociated", exception.getLocalizedMessage());
        verify(mockImageService).deleteIfUnassigned(IMAGE_ID);
        verify(mockRoomDao, never()).create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any());
    }

    @Test
    public void createRoomWithAvailabilityFailsWhenDayPriceIsInvalid() {
        RoomCreateRequest request = new RoomCreateRequest(
                "Room",
                "Argentina",
                "Buenos Aires",
                "Desc",
                "SHARED",
                "TWIN",
                false,
                true,
                List.of("WIFI"),
                List.of(new DateRange(START_DATE, END_DATE)),
                BigDecimal.ZERO,
                IMAGE_ID
        );

        RoomValidationException exception = assertThrows(RoomValidationException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, request));

        assertEquals("room.dayPrice.min", exception.getLocalizedMessage());
        verify(mockUserService, never()).findUserById(anyLong());
        verify(mockRoomDao, never()).create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any());
    }

    @Test
    public void createRoomWithAvailabilityFailsWhenDateRangesOverlap() {
        RoomCreateRequest request = new RoomCreateRequest(
                "Room",
                "Argentina",
                "Buenos Aires",
                "Desc",
                "SHARED",
                "TWIN",
                false,
                true,
                List.of("WIFI"),
                List.of(
                        new DateRange(START_DATE, END_DATE),
                        new DateRange(END_DATE.minusDays(1), END_DATE.plusDays(3))
                ),
                BigDecimal.valueOf(100),
                IMAGE_ID
        );

        RoomValidationException exception = assertThrows(RoomValidationException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, request));

        assertEquals("room.error.overlappingDates", exception.getLocalizedMessage());
        verify(mockUserService, never()).findUserById(anyLong());
        verify(mockRoomDao, never()).create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any());
    }

    @Test
    public void createRoomWithAvailabilityCleansImageWhenRoomCreationFails() {
        when(mockUserService.findUserById(OWNER_ID)).thenReturn(Optional.of(ownerUser));
        when(mockImageService.findOwnedUnassignedImage(IMAGE_ID, OWNER_ID)).thenReturn(Optional.of(ownedImage));
        when(mockImageService.findImageById(IMAGE_ID)).thenReturn(Optional.of(ownedImage));
        when(mockRoomDao.create(anyLong(), anyString(), anyString(), anyString(), anyString(),
                any(), any(), anyBoolean(), anyBoolean(), anyString(), anyLong(), any()))
                .thenThrow(new IllegalStateException("Room could not be created"));

        assertThrows(IllegalStateException.class,
                () -> roomService.createRoomWithAvailability(OWNER_ID, validCreateRequest(IMAGE_ID)));

        verify(mockImageService).deleteIfUnassigned(IMAGE_ID);
    }

    @Test
    public void deleteRoomDeletesOwnedRoomWithoutActiveContacts() {
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockContactDao.hasActiveContacts(ROOM_ID)).thenReturn(false);
        when(mockContactDao.findPendingContactsForRoom(ROOM_ID)).thenReturn(List.of());
        when(mockRoomDao.deleteRoom(ROOM_ID)).thenReturn(true);

        roomService.deleteRoom(ROOM_ID);

        verify(mockContactDao).deletePendingContactsForRoom(ROOM_ID);
        verify(mockRoomDao).deleteRoom(ROOM_ID);
        verify(mockImageService).deleteImage(IMAGE_ID);
    }

    @Test
    public void deleteRoomNotifiesOfferUserAndDeletesPendingWhenRoomWasRequested() {
        Contact pendingContact = new Contact(
                1L,
                ownedRoom,
                java.time.LocalDateTime.now(),
                SwapStatus.PENDING,
                false,
                BigDecimal.valueOf(100),
                otherUser,
                null,
                new DateRange(START_DATE, END_DATE),
                null
        );

        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockContactDao.hasActiveContacts(ROOM_ID)).thenReturn(false);
        when(mockContactDao.findPendingContactsForRoom(ROOM_ID)).thenReturn(List.of(pendingContact));
        when(mockRoomDao.deleteRoom(ROOM_ID)).thenReturn(true);

        roomService.deleteRoom(ROOM_ID);

        verify(mockEmailService).sendPendingContactRoomDeletedNotification(
                otherUser,
                ownerUser.getName(),
                ownerUser.getEmail(),
                ownedRoom.getTitle(),
                ownedRoom.getCountry()
        );
        InOrder inOrder = inOrder(mockContactDao, mockRoomDao);
        inOrder.verify(mockContactDao).deletePendingContactsForRoom(ROOM_ID);
        inOrder.verify(mockRoomDao).deleteRoom(ROOM_ID);
    }

    @Test
    public void deleteRoomNotifiesRequestedRoomOwnerAndDeletesPendingWhenRoomWasOffered() {
        Contact pendingContact = new Contact(
                2L,
                otherRoom,
                java.time.LocalDateTime.now(),
                SwapStatus.PENDING,
                true,
                null,
                otherUser,
                ownedRoom,
                new DateRange(START_DATE, END_DATE),
                null
        );

        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockContactDao.hasActiveContacts(ROOM_ID)).thenReturn(false);
        when(mockContactDao.findPendingContactsForRoom(ROOM_ID)).thenReturn(List.of(pendingContact));
        when(mockRoomDao.deleteRoom(ROOM_ID)).thenReturn(true);

        roomService.deleteRoom(ROOM_ID);
        verify(mockEmailService).sendPendingContactRoomDeletedNotification(
                otherUser,
                ownerUser.getName(),
                ownerUser.getEmail(),
                ownedRoom.getTitle(),
                ownedRoom.getCountry()
        );
        InOrder inOrder = inOrder(mockContactDao, mockRoomDao);
        inOrder.verify(mockContactDao).deletePendingContactsForRoom(ROOM_ID);
        inOrder.verify(mockRoomDao).deleteRoom(ROOM_ID);
    }

    @Test
    public void deleteRoomFailsWhenRoomDoesNotExist() {
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomService.deleteRoom(ROOM_ID));

        verify(mockRoomDao, never()).deleteRoom(ROOM_ID);
        verify(mockImageService, never()).deleteImage(IMAGE_ID);
    }


    @Test
    public void deleteRoomFailsWhenRoomHasActiveContacts() {
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockContactDao.hasActiveContacts(ROOM_ID)).thenReturn(true);

        assertThrows(RoomHasActiveSwapsException.class, () -> roomService.deleteRoom(ROOM_ID));

        verify(mockContactDao, never()).findPendingContactsForRoom(ROOM_ID);
        verify(mockContactDao, never()).deletePendingContactsForRoom(ROOM_ID);
        verify(mockRoomDao, never()).deleteRoom(ROOM_ID);
        verify(mockImageService, never()).deleteImage(IMAGE_ID);
    }

    @Test
    public void updateRoomUpdatesOwnedRoom() {
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockRoomDao.updateRoom(ROOM_ID, "Updated room", "Updated description", "[\"WiFi\", \"Parking\"]", BigDecimal.valueOf(175.50)))
                .thenReturn(true);

        Room updatedRoom = roomService.updateRoom(
                ROOM_ID,
                "Updated room",
                "Updated description",
                List.of("WIFI", "PARKING"),
                BigDecimal.valueOf(175.50)
        );

        assertSame(ownedRoom, updatedRoom);
        verify(mockRoomDao).updateRoom(ROOM_ID, "Updated room", "Updated description", "[\"WiFi\", \"Parking\"]", BigDecimal.valueOf(175.50));
    }

    @Test
    public void updateRoomFailsWhenRoomDoesNotExist() {
        assertThrows(RoomNotFoundException.class, () -> roomService.updateRoom(
                ROOM_ID,
                "Updated room",
                "Updated description",
                List.of("WIFI"),
                BigDecimal.valueOf(175.50)
        ));

        verify(mockRoomDao, never()).updateRoom(ROOM_ID, "Updated room", "Updated description", "[\"WiFi\"]", BigDecimal.valueOf(175.50));
    }

    @Test
    public void updateRoomFailsWhenAmenityIsInvalid() {
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));

        assertThrows(RoomValidationException.class, () -> roomService.updateRoom(
                ROOM_ID,
                "Updated room",
                "Updated description",
                List.of("NOT_AN_AMENITY"),
                BigDecimal.valueOf(175.50)
        ));

        verify(mockRoomDao, never()).updateRoom(ROOM_ID, "Updated room", "Updated description", "[]", BigDecimal.valueOf(175.50));
    }

    @Test
    public void buildSearchCriteriaAcceptsAmenityNames() {
        RoomSearchCriteria criteria = roomService.buildSearchCriteria(
                null, null, null, null, null, null, null, null, List.of("WIFI", "POOL")
        );

        assertEquals(List.of(Amenity.WIFI, Amenity.POOL), criteria.getAmenities());
    }

    @Test
    public void buildSearchCriteriaAcceptsAmenityDisplayNames() {
        RoomSearchCriteria criteria = roomService.buildSearchCriteria(
                null, null, null, null, null, null, null, null, List.of("WiFi", "Air Conditioning")
        );

        assertEquals(List.of(Amenity.WIFI, Amenity.AC), criteria.getAmenities());
    }

    @Test
    public void buildSearchCriteriaTrimsAmenityWhitespaceAndQuotes() {
        RoomSearchCriteria criteria = roomService.buildSearchCriteria(
                null, null, null, null, null, null, null, null, List.of("\" WiFi \"", " Air Conditioning ")
        );

        assertEquals(List.of(Amenity.WIFI, Amenity.AC), criteria.getAmenities());
    }

    @Test
    public void buildSearchCriteriaFailsWhenAmenityIsInvalid() {
        assertThrows(RoomValidationException.class, () -> roomService.buildSearchCriteria(
                null, null, null, null, null, null, null, null, List.of("NOT_AN_AMENITY")
        ));
    }

    @Test
    public void searchRoomCardsIncludesReviewStatsWhenPresent() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        when(mockRoomDao.searchRooms(criteria, 1, 12)).thenReturn(List.of(ownedRoom, otherRoom));
        when(mockReviewDao.getReviewStatsByRoomIds(List.of(ROOM_ID, ROOM_ID + 1))).thenReturn(Map.of(
                ROOM_ID, new RoomReviewStats(ROOM_ID, 2, 4.5)
        ));

        List<RoomCardResult> results = roomService.searchRoomCards(criteria, 1, 12);

        assertEquals(2, results.size());
        assertSame(ownedRoom, results.get(0).getRoom());
        assertEquals(2, results.get(0).getTotalReviews());
        assertEquals(4.5, results.get(0).getAverageRating(), 0.0001);
        assertSame(otherRoom, results.get(1).getRoom());
        assertEquals(0, results.get(1).getTotalReviews());
        assertEquals(0, results.get(1).getAverageRating(), 0.0001);
        verify(mockReviewDao).getReviewStatsByRoomIds(List.of(ROOM_ID, ROOM_ID + 1));
    }
}
