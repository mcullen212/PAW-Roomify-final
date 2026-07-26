package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.interfaces.exceptions.GroupTripNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.RoomNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.TripNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.TripDao;
import ar.edu.itba.paw.interfaces.service.GroupTripService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.service.trips.TripServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TripServiceImplTest {
    private static final long USER_ID = 10L;
    private static final long GROUP_TRIP_ID = 1L;
    private static final long OTHER_GROUP_TRIP_ID = 2L;
    private static final long TRIP_ID = 10L;
    private static final long ROOM_ID = 20L;
    private static final String COUNTRY = "Germany";
    private static final String USER_EMAIL = "test@example.com";

    // Valid dates
    private static final LocalDate START_DATE = LocalDate.now().plusDays(10);
    private static final LocalDate END_DATE = START_DATE.plusDays(5);
    private static final String START_DATE_STRING = START_DATE.toString();
    private static final String END_DATE_STRING = END_DATE.toString();
    private static final DateRange VALID_RANGE = new DateRange(START_DATE, END_DATE);

    // Dates for an overlapping trip
    private static final LocalDate OVERLAP_START = START_DATE.plusDays(2);
    private static final LocalDate OVERLAP_END = END_DATE.minusDays(2);
    private static final DateRange OVERLAP_RANGE = new DateRange(OVERLAP_START, OVERLAP_END);

    @Mock
    private TripDao mockTripDao;
    @Mock
    private GroupTripService mockGroupTripService;
    @Mock
    private UserService mockUserService;
    @Mock
    private RoomService mockRoomService;

    @InjectMocks
    private TripServiceImpl tripService;

    private GroupTrip mockGroupTrip;
    private Trip existingTrip;
    private Trip createdTrip;
    private Room matchingRoom;
    private Room mismatchingRoom;
    private final User testUser = new User(
            USER_ID,
            USER_EMAIL,
            "Test User",
            "pass",
            true,
            Locale.ENGLISH.toString(),
            null,
            null
    );

    @Before
    public void setup() {
        mockGroupTrip = new GroupTrip( testUser,"EuroTrip", VALID_RANGE);
        ReflectionTestUtils.setField(mockGroupTrip, "id", GROUP_TRIP_ID);

        existingTrip = new Trip(mockGroupTrip, "Italy", VALID_RANGE);

        createdTrip = new Trip(mockGroupTrip, COUNTRY, OVERLAP_RANGE);

        matchingRoom = new Room(ROOM_ID, "Rome room", "Italy", "Rome", "Nice room", null, null, false, false, null, testUser, null, null);
        mismatchingRoom = new Room(ROOM_ID, "Berlin room", "Germany", "Berlin", "Nice room", null, null, false, false, null, testUser, null, null);
    }

    @Test
    public void testCreate_Successful() throws DateRangeException {
        when(mockGroupTripService.findGroupTripById(eq(GROUP_TRIP_ID)))
                .thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.existsOverlappingTrip(eq(GROUP_TRIP_ID), any(DateRange.class)))
                .thenReturn(false);
        when(mockTripDao.create(
                eq(GROUP_TRIP_ID),
                eq(COUNTRY),
                any(DateRange.class)))
                .thenReturn(createdTrip);

        doNothing().when(mockGroupTripService).recalculateDates(eq(GROUP_TRIP_ID));

        Trip result = tripService.create(GROUP_TRIP_ID, COUNTRY, START_DATE, END_DATE);

        Assert.assertNotNull(result);
        Assert.assertEquals(COUNTRY, result.getCountry());

        Mockito.verify(mockTripDao, Mockito.times(1)).create(eq(GROUP_TRIP_ID), eq(COUNTRY), any(DateRange.class));
        Mockito.verify(mockTripDao, Mockito.times(1)).existsOverlappingTrip(eq(GROUP_TRIP_ID), any(DateRange.class));
        Mockito.verify(mockGroupTripService, Mockito.times(1)).recalculateDates(eq(GROUP_TRIP_ID));
    }

    @Test
    public void testCreate_DateOverlap_ThrowsException() throws DateRangeException {
        when(mockGroupTripService.findGroupTripById(eq(GROUP_TRIP_ID)))
                .thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.existsOverlappingTrip(eq(GROUP_TRIP_ID), any(DateRange.class)))
                .thenReturn(true);

        assertThrows(
                DateRangeException.class,
                () -> tripService.create(GROUP_TRIP_ID, COUNTRY, START_DATE, END_DATE));

        Mockito.verify(mockTripDao, Mockito.never()).create(anyLong(), anyString(), any(DateRange.class));
        Mockito.verify(mockGroupTripService, Mockito.never()).recalculateDates(anyLong());
    }

    @Test
    public void testFindTripsByGroupTripId_ExistingGroupTrip_DelegatesToDao() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripsByGroupTripId(GROUP_TRIP_ID, 2, 5)).thenReturn(List.of(existingTrip));

        List<Trip> result = tripService.findTripsByGroupTripId(GROUP_TRIP_ID, 2, 5);

        Assert.assertEquals(List.of(existingTrip), result);
        Mockito.verify(mockGroupTripService).findGroupTripById(GROUP_TRIP_ID);
        Mockito.verify(mockTripDao).findTripsByGroupTripId(GROUP_TRIP_ID, 2, 5);
    }

    @Test
    public void testFindTripsByGroupTripId_MissingGroupTrip_ThrowsNotFoundException() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(
                GroupTripNotFoundException.class,
                () -> tripService.findTripsByGroupTripId(GROUP_TRIP_ID, 1, 12));

        Mockito.verify(mockTripDao, Mockito.never()).findTripsByGroupTripId(anyLong(), anyInt(), anyInt());
    }

    @Test
    public void testCountTripsByGroupTripId_ExistingGroupTrip_DelegatesToDao() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.countTripsByGroupTripId(GROUP_TRIP_ID)).thenReturn(3);

        int result = tripService.countTripsByGroupTripId(GROUP_TRIP_ID);

        Assert.assertEquals(3, result);
        Mockito.verify(mockGroupTripService).findGroupTripById(GROUP_TRIP_ID);
        Mockito.verify(mockTripDao).countTripsByGroupTripId(GROUP_TRIP_ID);
    }

    @Test
    public void testCountTripsByGroupTripId_MissingGroupTrip_ThrowsNotFoundException() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(
                GroupTripNotFoundException.class,
                () -> tripService.countTripsByGroupTripId(GROUP_TRIP_ID));

        Mockito.verify(mockTripDao, Mockito.never()).countTripsByGroupTripId(anyLong());
    }

    @Test
    public void testFindTripByGroupTripId_MatchingGroupTrip_ReturnsTrip() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.of(existingTrip));

        Trip result = tripService.findTripByGroupTripId(GROUP_TRIP_ID, TRIP_ID);

        Assert.assertEquals(existingTrip, result);
        Mockito.verify(mockGroupTripService).findGroupTripById(GROUP_TRIP_ID);
        Mockito.verify(mockTripDao).findTripById(TRIP_ID);
    }

    @Test
    public void testFindTripByGroupTripId_MissingTrip_ThrowsNotFoundException() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(
                TripNotFoundException.class,
                () -> tripService.findTripByGroupTripId(GROUP_TRIP_ID, TRIP_ID));
    }

    @Test
    public void testFindTripByGroupTripId_DifferentGroupTrip_ThrowsNotFoundException() {
        GroupTrip otherGroupTrip = new GroupTrip(testUser, "Other trip", VALID_RANGE);
        ReflectionTestUtils.setField(otherGroupTrip, "id", OTHER_GROUP_TRIP_ID);
        Trip tripFromOtherGroup = new Trip(otherGroupTrip, COUNTRY, VALID_RANGE);

        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.of(tripFromOtherGroup));

        assertThrows(
                TripNotFoundException.class,
                () -> tripService.findTripByGroupTripId(GROUP_TRIP_ID, TRIP_ID));
    }

    @Test
    public void testMatchRoomToTrip_ReturnsTripWhenRoomCountryMatches() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.of(existingTrip));
        when(mockRoomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(matchingRoom));

        Trip result = tripService.matchRoomToTrip(ROOM_ID, GROUP_TRIP_ID, TRIP_ID);

        Assert.assertEquals(existingTrip, result);
        Mockito.verify(mockRoomService).findRoomById(ROOM_ID);
    }

    @Test
    public void testMatchRoomToTrip_NullRoomId_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tripService.matchRoomToTrip(null, GROUP_TRIP_ID, TRIP_ID));

        Mockito.verify(mockTripDao, Mockito.never()).findTripById(anyLong());
        Mockito.verify(mockRoomService, Mockito.never()).findRoomById(anyLong());
    }

    @Test
    public void testMatchRoomToTrip_MissingRoom_ThrowsRoomNotFoundException() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.of(existingTrip));
        when(mockRoomService.findRoomById(ROOM_ID)).thenReturn(Optional.empty());

        assertThrows(
                RoomNotFoundException.class,
                () -> tripService.matchRoomToTrip(ROOM_ID, GROUP_TRIP_ID, TRIP_ID));
    }

    @Test
    public void testMatchRoomToTrip_DifferentGroupTrip_ThrowsNotFoundException() {
        GroupTrip otherGroupTrip = new GroupTrip(testUser, "Other trip", VALID_RANGE);
        ReflectionTestUtils.setField(otherGroupTrip, "id", OTHER_GROUP_TRIP_ID);
        Trip tripFromOtherGroup = new Trip(otherGroupTrip, COUNTRY, VALID_RANGE);

        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.of(tripFromOtherGroup));

        assertThrows(
                TripNotFoundException.class,
                () -> tripService.matchRoomToTrip(ROOM_ID, GROUP_TRIP_ID, TRIP_ID));

        Mockito.verify(mockRoomService, Mockito.never()).findRoomById(anyLong());
    }

    @Test
    public void testMatchRoomToTrip_CountryMismatch_ThrowsIllegalArgumentException() {
        when(mockGroupTripService.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(mockGroupTrip));
        when(mockTripDao.findTripById(TRIP_ID)).thenReturn(Optional.of(existingTrip));
        when(mockRoomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(mismatchingRoom));

        assertThrows(
                IllegalArgumentException.class,
                () -> tripService.matchRoomToTrip(ROOM_ID, GROUP_TRIP_ID, TRIP_ID));
    }

    @Test
    public void testBringMyTrips_UsesUserIdAndDelegatesToDao() {
        when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.of(testUser));
        when(mockTripDao.bringMytrips(COUNTRY, START_DATE, END_DATE, testUser, 2, 5))
                .thenReturn(List.of(existingTrip));

        List<Trip> result = tripService.bringMyTrips(COUNTRY, START_DATE_STRING, END_DATE_STRING, USER_ID, 2, 5);

        Assert.assertEquals(List.of(existingTrip), result);
        Mockito.verify(mockUserService).findUserById(USER_ID);
        Mockito.verify(mockTripDao).bringMytrips(COUNTRY, START_DATE, END_DATE, testUser, 2, 5);
    }

    @Test
    public void testBringMyTrips_NoOptionalFilters_DelegatesOnlyWithUser() {
        when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.of(testUser));
        when(mockTripDao.bringMytrips(null, null, null, testUser, 1, 12))
                .thenReturn(List.of(existingTrip));

        List<Trip> result = tripService.bringMyTrips(null, null, null, USER_ID, 1, 12);

        Assert.assertEquals(List.of(existingTrip), result);
        Mockito.verify(mockTripDao).bringMytrips(null, null, null, testUser, 1, 12);
    }

    @Test
    public void testBringMyTrips_DateFilterWithoutCountry_DelegatesWithDateRange() {
        when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.of(testUser));
        when(mockTripDao.bringMytrips(null, START_DATE, END_DATE, testUser, 1, 12))
                .thenReturn(List.of(existingTrip));

        List<Trip> result = tripService.bringMyTrips(null, START_DATE_STRING, END_DATE_STRING, USER_ID, 1, 12);

        Assert.assertEquals(List.of(existingTrip), result);
        Mockito.verify(mockTripDao).bringMytrips(null, START_DATE, END_DATE, testUser, 1, 12);
    }

    @Test
    public void testBringMyTrips_MissingUser_ThrowsUserNotFoundException() {
        when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> tripService.bringMyTrips(COUNTRY, START_DATE_STRING, END_DATE_STRING, USER_ID, 1, 12));
    }

    @Test
    public void testBringMyTrips_NullUserId_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tripService.bringMyTrips(COUNTRY, START_DATE_STRING, END_DATE_STRING, null, 1, 12));
    }

    @Test
    public void testBringMyTrips_InvalidUserId_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tripService.bringMyTrips(COUNTRY, START_DATE_STRING, END_DATE_STRING, 0L, 1, 12));
    }

    @Test
    public void testBringMyTrips_OnlyCheckIn_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tripService.bringMyTrips(COUNTRY, START_DATE_STRING, null, USER_ID, 1, 12));
    }

    @Test
    public void testBringMyTrips_OnlyCheckOut_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tripService.bringMyTrips(COUNTRY, null, END_DATE_STRING, USER_ID, 1, 12));
    }

    @Test
    public void testCountMyTrips_UsesUserIdAndDelegatesToDao() {
        when(mockUserService.findUserById(USER_ID)).thenReturn(Optional.of(testUser));
        when(mockTripDao.countMyTrips(COUNTRY, START_DATE, END_DATE, testUser)).thenReturn(3);

        int result = tripService.countMyTrips(COUNTRY, START_DATE_STRING, END_DATE_STRING, USER_ID);

        Assert.assertEquals(3, result);
        Mockito.verify(mockUserService).findUserById(USER_ID);
        Mockito.verify(mockTripDao).countMyTrips(COUNTRY, START_DATE, END_DATE, testUser);
    }
}
