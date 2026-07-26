package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.persistence.GroupTripDao;
import ar.edu.itba.paw.interfaces.exceptions.GroupTripNotFoundException;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.DTO.GroupTripPage;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;
import ar.edu.itba.paw.service.trips.GroupTripServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupTripServiceImplTest {
    private static final long USER_ID = 10L;
    private static final long GROUP_TRIP_ID = 1L;
    private static final String USER_EMAIL = "test@example.com";
    private static final String TRIP_NAME = "Summer Vacation";
    private static final LocalDate START_DATE = LocalDate.now().plusDays(30);
    private static final LocalDate END_DATE = START_DATE.plusDays(10);

    @Mock
    private GroupTripDao mockGroupTripDao;
    @Mock
    private UserService mockUserService;

    @InjectMocks
    private GroupTripServiceImpl groupTripService;

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

    private final GroupTrip createdGroupTrip = new GroupTrip(
            testUser,
            TRIP_NAME,
            new DateRange((LocalDate) null, null)
    );
    private final GroupTrip datedGroupTrip = new GroupTrip(
            testUser,
            TRIP_NAME,
            new DateRange(START_DATE, END_DATE)
    );

    private final Trip createdTrip = new Trip(
            createdGroupTrip,
            "France",
            new DateRange(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 3, 16))
    );

    @Test
    public void testCreate_Successful() {
        // 1. Setup Mocks
        when(mockUserService.findUserById(USER_ID))
                .thenReturn(Optional.of(testUser));
        when(mockGroupTripDao.create(
                eq(testUser),
                eq(TRIP_NAME),
                any(DateRange.class)))
                .thenReturn(createdGroupTrip);

        // 2. Exercise
        GroupTrip result = groupTripService.create(
                USER_ID,
                TRIP_NAME
        );

        // 3. Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(TRIP_NAME, result.getTitle());
        Assert.assertEquals(testUser, result.getOwner());
        Assert.assertNull(result.getDateRange().getStartDate());
        Assert.assertNull(result.getDateRange().getEndDate());
    }

    @Test
    public void testCreate_EmptyName_ThrowsException() {
        // Setup user finding to pass
        when(mockUserService.findUserById(USER_ID))
                .thenReturn(Optional.of(testUser));

        assertThrows(
                IllegalArgumentException.class,
                () -> groupTripService.create(USER_ID, "  ")
        );
    }

    @Test
    public void testCreate_UserNotFound_ThrowsException() {
        // Mock user finding to fail
        when(mockUserService.findUserById(USER_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> groupTripService.create(USER_ID, TRIP_NAME)
        );

    }

    @Test
    public void testUpdateGroupTrip_UnsupportedTitle_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> groupTripService.updateGroupTrip(GROUP_TRIP_ID, "New title", TripStatus.UPCOMING)
        );

        Mockito.verify(mockGroupTripDao, Mockito.never()).updateStatus(anyLong(), any());
    }

    @Test
    public void testUpdateGroupTrip_StatusOnly_ReturnsUpdatedGroupTrip() {
        when(mockGroupTripDao.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(datedGroupTrip));
        when(mockGroupTripDao.updateStatus(GROUP_TRIP_ID, TripStatus.UPCOMING)).thenReturn(true);

        GroupTrip result = groupTripService.updateGroupTrip(GROUP_TRIP_ID, null, TripStatus.UPCOMING);

        Assert.assertEquals(datedGroupTrip, result);
        Mockito.verify(mockGroupTripDao, Mockito.times(2)).findGroupTripById(GROUP_TRIP_ID);
        Mockito.verify(mockGroupTripDao).updateStatus(GROUP_TRIP_ID, TripStatus.UPCOMING);
    }

    @Test
    public void testUpdateStatus_UpcomingStatus_FinishesPlanning() {
        when(mockGroupTripDao.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.of(datedGroupTrip));
        when(mockGroupTripDao.updateStatus(GROUP_TRIP_ID, TripStatus.UPCOMING)).thenReturn(true);

        boolean result = groupTripService.updateStatus(GROUP_TRIP_ID, TripStatus.UPCOMING);

        Assert.assertTrue(result);
        Mockito.verify(mockGroupTripDao).findGroupTripById(GROUP_TRIP_ID);
        Mockito.verify(mockGroupTripDao).updateStatus(GROUP_TRIP_ID, TripStatus.UPCOMING);
    }

    @Test
    public void testUpdateStatus_PlanningStatus_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> groupTripService.updateStatus(GROUP_TRIP_ID, TripStatus.PLANNING)
        );

        Mockito.verify(mockGroupTripDao, Mockito.never()).updateStatus(anyLong(), any());
    }

    @Test
    public void testUpdateStatus_CancelledStatus_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> groupTripService.updateStatus(GROUP_TRIP_ID, TripStatus.CANCELLED)
        );

        Mockito.verify(mockGroupTripDao, Mockito.never()).updateStatus(anyLong(), any());
    }

    @Test
    public void testIsOwnerTrip_GroupTripNotFound_ThrowsNotFoundException() {
        when(mockUserService.findUserByEmail(USER_EMAIL)).thenReturn(Optional.of(testUser));
        when(mockGroupTripDao.findGroupTripById(GROUP_TRIP_ID)).thenReturn(Optional.empty());

        assertThrows(
                GroupTripNotFoundException.class,
                () -> groupTripService.isOwnerTrip(USER_EMAIL, GROUP_TRIP_ID)
        );
    }

    @Test
    public void testGetTripsForGroupTripAssociation_InvalidUserId_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> groupTripService.getTripsForGroupTripAssociation(null, "France", null, null, TripStatus.PLANNING, 1, 10)
        );

        Mockito.verify(mockGroupTripDao, Mockito.never()).findTripsForGroupTripAssociation(anyLong(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    public void testGetTripsForGroupTripAssociation_IncompleteDateRange_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> groupTripService.getTripsForGroupTripAssociation(USER_ID, "France", "2026-03-01", null, TripStatus.PLANNING, 1, 10)
        );

        Mockito.verify(mockGroupTripDao, Mockito.never()).findTripsForGroupTripAssociation(anyLong(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    public void testGetTripsForGroupTripAssociation_PassesParsedFiltersToDao() {
        groupTripService.getTripsForGroupTripAssociation(
                USER_ID,
                "France",
                "2026-03-01",
                "2026-03-31",
                TripStatus.PLANNING,
                1,
                10
        );

        Mockito.verify(mockGroupTripDao).findTripsForGroupTripAssociation(
                eq(USER_ID),
                eq(TripStatus.PLANNING),
                eq("France"),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                eq(1),
                eq(10)
        );
    }

    @Test
    public void testSearchGroupTrips_NoTripFilters_UsesGroupTripListing() {
        when(mockGroupTripDao.findGroupTripsByOwnerId(USER_ID, TripStatus.PLANNING, 1, 10))
                .thenReturn(List.of(createdGroupTrip));
        when(mockGroupTripDao.countGroupTripsByOwnerId(USER_ID, TripStatus.PLANNING))
                .thenReturn(1);

        GroupTripPage result = groupTripService.searchGroupTrips(
                USER_ID,
                null,
                null,
                null,
                TripStatus.PLANNING,
                1,
                10
        );

        Assert.assertEquals(1, result.getItems().size());
        Assert.assertEquals(createdGroupTrip, result.getItems().get(0).getGroupTrip());
        Assert.assertFalse(result.getItems().get(0).getMatchedTrip().isPresent());
        Assert.assertEquals(1, result.getTotalItems());
        Mockito.verify(mockGroupTripDao).findGroupTripsByOwnerId(USER_ID, TripStatus.PLANNING, 1, 10);
        Mockito.verify(mockGroupTripDao).countGroupTripsByOwnerId(USER_ID, TripStatus.PLANNING);
        Mockito.verify(mockGroupTripDao, Mockito.never()).findTripsForGroupTripAssociation(anyLong(), any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    public void testSearchGroupTrips_WithTripFilters_UsesAssociationSearch() {
        when(mockGroupTripDao.findTripsForGroupTripAssociation(
                eq(USER_ID),
                eq(TripStatus.PLANNING),
                eq("France"),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                eq(1),
                eq(10)
        )).thenReturn(List.of(createdTrip));
        when(mockGroupTripDao.countTripsForGroupTripAssociation(
                eq(USER_ID),
                eq(TripStatus.PLANNING),
                eq("France"),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31))
        )).thenReturn(1);

        GroupTripPage result = groupTripService.searchGroupTrips(
                USER_ID,
                "France",
                "2026-03-01",
                "2026-03-31",
                TripStatus.PLANNING,
                1,
                10
        );

        Assert.assertEquals(1, result.getItems().size());
        Assert.assertEquals(createdGroupTrip, result.getItems().get(0).getGroupTrip());
        Assert.assertEquals(createdTrip, result.getItems().get(0).getMatchedTrip().orElseThrow(AssertionError::new));
        Assert.assertEquals(1, result.getTotalItems());
        Mockito.verify(mockGroupTripDao).findTripsForGroupTripAssociation(
                eq(USER_ID),
                eq(TripStatus.PLANNING),
                eq("France"),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31)),
                eq(1),
                eq(10)
        );
        Mockito.verify(mockGroupTripDao).countTripsForGroupTripAssociation(
                eq(USER_ID),
                eq(TripStatus.PLANNING),
                eq("France"),
                eq(LocalDate.of(2026, 3, 1)),
                eq(LocalDate.of(2026, 3, 31))
        );
        Mockito.verify(mockGroupTripDao, Mockito.never()).findGroupTripsByOwnerId(anyLong(), any(), anyInt(), anyInt());
    }
}
