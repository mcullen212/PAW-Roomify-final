package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.persistence.RoomDao;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.ImageService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.DeleteRoomStatus;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.service.rooms.RoomSecurityServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RoomSecurityServiceImplTest {
    private static final long ROOM_ID = 100L;
    private static final long OWNER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long IMAGE_ID = 5L;
    private static final String OWNER_EMAIL = "owner@test.com";
    private static final String OTHER_EMAIL = "other@test.com";
    private static final String NON_EXISTENT_EMAIL = "fake@test.com";

    @Mock
    private RoomDao mockRoomDao;
    @Mock
    private UserService mockUserService;
    @Mock
    private ContactService mockContactService;
    @Mock
    private ImageService mockImageService;

    @InjectMocks
    private RoomSecurityServiceImpl roomSecurityService;

    private final Image dummyImage = new Image(IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final User ownerUser = new User(OWNER_ID, OWNER_EMAIL, "Owner", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final User otherUser = new User(OTHER_USER_ID, OTHER_EMAIL, "Other", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final Room ownedRoom = new Room(ROOM_ID, "Req Room", "AR","BA","Desc",RoomType.SHARED, BedType.TWIN,false,true,"Wifi", ownerUser,dummyImage, BigDecimal.valueOf(100));

    @Test
    public void testDeleteRoom_Success() {
        // 1. Setup Mocks
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactService.hasActiveContacts(ROOM_ID)).thenReturn(false);
        when(mockRoomDao.deleteRoom(ROOM_ID)).thenReturn(true);

        // 2. Exercise
        DeleteRoomStatus status = roomSecurityService.deleteRoom(ROOM_ID, OWNER_EMAIL);

        // 3. Assertions
        Assert.assertEquals(DeleteRoomStatus.SUCCESS, status);
    }

    @Test
    public void testDeleteRoom_RoomNotFound() {
        // 1. Setup Mocks
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.empty()); // Room not found

        // 2. Exercise
        DeleteRoomStatus status = roomSecurityService.deleteRoom(ROOM_ID, OWNER_EMAIL);

        // 3. Assertions
        Assert.assertEquals(DeleteRoomStatus.ROOM_NOT_FOUND, status);
    }

    @Test
    public void testDeleteRoom_NotOwner_UserNotFound() {
        // 1. Setup Mocks
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockUserService.findUserByEmail(NON_EXISTENT_EMAIL)).thenReturn(Optional.empty()); // User not found

        // 2. Exercise
        DeleteRoomStatus status = roomSecurityService.deleteRoom(ROOM_ID, NON_EXISTENT_EMAIL);

        // 3. Assertions
        Assert.assertEquals(DeleteRoomStatus.NOT_OWNER, status);
    }

    @Test
    public void testDeleteRoom_NotOwner_MismatchedId() {
        // 1. Setup Mocks
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockUserService.findUserByEmail(OTHER_EMAIL)).thenReturn(Optional.of(otherUser)); // User is not the owner

        // 2. Exercise
        DeleteRoomStatus status = roomSecurityService.deleteRoom(ROOM_ID, OTHER_EMAIL);

        // 3. Assertions
        Assert.assertEquals(DeleteRoomStatus.NOT_OWNER, status);
    }

    @Test
    public void testDeleteRoom_HasActiveSwaps() {
        // 1. Setup Mocks
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactService.hasActiveContacts(ROOM_ID)).thenReturn(true); // Has active swaps

        // 2. Exercise
        DeleteRoomStatus status = roomSecurityService.deleteRoom(ROOM_ID, OWNER_EMAIL);

        // 3. Assertions
        Assert.assertEquals(DeleteRoomStatus.HAS_ACTIVE_SWAPS, status);
    }

    @Test
    public void testDeleteRoom_PersistenceError() {
        // 1. Setup Mocks
        when(mockRoomDao.findRoomById(ROOM_ID)).thenReturn(Optional.of(ownedRoom));
        when(mockUserService.findUserByEmail(OWNER_EMAIL)).thenReturn(Optional.of(ownerUser));
        when(mockContactService.hasActiveContacts(ROOM_ID)).thenReturn(false);
        when(mockRoomDao.deleteRoom(ROOM_ID)).thenReturn(false); // DAO returns false (persistence error)

        // 2. Exercise
        DeleteRoomStatus status = roomSecurityService.deleteRoom(ROOM_ID, OWNER_EMAIL);

        // 3. Assertions
        Assert.assertEquals(DeleteRoomStatus.PERSISTENCE_ERROR, status);
    }
}
