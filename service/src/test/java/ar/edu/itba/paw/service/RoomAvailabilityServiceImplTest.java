package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.persistence.RoomAvailabilityDao;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.*;
import ar.edu.itba.paw.service.rooms.RoomAvailabilityServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RoomAvailabilityServiceImplTest {

    // --- Constants ---
    private static final long ROOM_ID = 10L;
    private static final long AVAILABILITY_ID = 1L;
    private static final LocalDate START_DATE = LocalDate.now().plusDays(5);
    private static final LocalDate END_DATE = START_DATE.plusDays(10);
    private static final LocalDate PAST_DATE = LocalDate.now().minusDays(5);
    private static final long DUMMY_IMAGE_ID = 1L;
    private static final Locale LOCALE = Locale.ENGLISH;


    // Corrected Constants using DateRange
    private static final DateRange VALID_DATE_RANGE = new DateRange(START_DATE, END_DATE);

    @Mock
    private RoomAvailabilityDao mockAvailabilityDao;

    @InjectMocks
    private RoomAvailabilityServiceImpl availabilityService;


    private static final long OWNER_ID = 5L;

    private final User ownerUser = new User(OWNER_ID, "owner@test.com", "Owner", "pass", false, LOCALE.toString(), null, null);
    private final Image dummyImage = new Image(DUMMY_IMAGE_ID, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});

    private final Room room = new Room( ROOM_ID, "Req Room", "AR", "BA", "Desc", RoomType.SHARED,BedType.TWIN,false, true, "Wifi", ownerUser, dummyImage, BigDecimal.valueOf(100));
    private final RoomAvailability availability = new RoomAvailability(AVAILABILITY_ID, room, VALID_DATE_RANGE);


    @Test
    public void testAddAvailability_Successful() {
        // Mock updated to use DateRange (matching the new DAO interface)
        when(mockAvailabilityDao.create(eq(ROOM_ID), any(DateRange.class))).thenReturn(availability);
        // Exercise (Assuming service now accepts DateRange)
        RoomAvailability result = availabilityService.addAvailability(ROOM_ID, VALID_DATE_RANGE);
        // Assertions
        Assert.assertNotNull(result);
        Assert.assertEquals(Long.valueOf(AVAILABILITY_ID), result.getId());
        Assert.assertEquals(START_DATE, result.getRange().getStartDate());
    }

    @Test
    public void testGetAvailabilities_Found() {
        List<RoomAvailability> expectedList = Collections.singletonList(availability);
        when(mockAvailabilityDao.findByRoom(ROOM_ID)).thenReturn(expectedList);

        List<RoomAvailability> result = availabilityService.getAvailabilities(ROOM_ID);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testGetAvailabilitiesBetween_Found() {
        List<RoomAvailability> expectedList = Collections.singletonList(availability);
        when(mockAvailabilityDao.findByRoomBetween(ROOM_ID, START_DATE, END_DATE)).thenReturn(expectedList);

        List<RoomAvailability> result = availabilityService.getAvailabilitiesBetween(ROOM_ID, START_DATE, END_DATE);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(Long.valueOf(AVAILABILITY_ID), result.get(0).getId());
    }

    @Test
    public void testFindAvailableIdByRoom_NotFound() {
        when(mockAvailabilityDao.findAvailableIdByRoom(ROOM_ID)).thenReturn(null);
        Long result = availabilityService.findAvailableIdByRoom(ROOM_ID);
        Assert.assertNull(result);
    }

    @Test
    public void testGetAvailabilityDatesForRoom_Successful() {
        List<RoomAvailability> mockList = Collections.singletonList(availability);
        when(mockAvailabilityDao.findByRoom(ROOM_ID)).thenReturn(mockList);

        DateRange result = availabilityService.getAvailabilityDatesForRoom(ROOM_ID);

        Assert.assertNotNull(result);
        Assert.assertEquals(START_DATE, result.getStartDate());
    }
//
    @Test
    public void testGetAvailabilityDatesForRoom_NotFound() {
        when(mockAvailabilityDao.findByRoom(ROOM_ID)).thenReturn(Collections.emptyList());

        assertThrows(
                NoSuchElementException.class,
                () -> availabilityService.getAvailabilityDatesForRoom(ROOM_ID)
        );
    }
    @Test
    public void testInAvailabilityRanges_True() {
        when(mockAvailabilityDao.inAvailabilityRanges(ROOM_ID, START_DATE, END_DATE)).thenReturn(true);

        boolean result = availabilityService.inAvailabilityRanges(ROOM_ID, START_DATE, END_DATE);

        Assert.assertTrue(result);
    }
}
