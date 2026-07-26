package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.RoomNotFoundException;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.RoomAvailabilityService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.model.DTO.RoomAvailabilityCalendar;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.service.rooms.RoomAvailabilityCalendarServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RoomAvailabilityCalendarServiceImplTest {
    private static final long ROOM_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);

    @Mock
    private RoomService roomService;
    @Mock
    private RoomAvailabilityService roomAvailabilityService;
    @Mock
    private ContactService contactService;

    @InjectMocks
    private RoomAvailabilityCalendarServiceImpl service;

    private final User owner = new User(1L, "owner@test.com", "Owner", "pass", false, Locale.ENGLISH.toString(), null, null);
    private final Image image = new Image(1L, "image.jpg", "image/jpeg", 3, new byte[]{1, 2, 3});
    private final Room room = new Room(
            ROOM_ID,
            "Room",
            "AR",
            "BA",
            "Desc",
            RoomType.SHARED,
            BedType.TWIN,
            false,
            true,
            "Wifi",
            owner,
            image,
            BigDecimal.valueOf(100)
    );

    @Test
    public void testGetRoomAvailabilityCalendarSubtractsBookedRanges() {
        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, TODAY, TODAY.plusYears(1))).thenReturn(List.of(
                new RoomAvailability(room, new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
        ));
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, TODAY, TODAY.plusYears(1))).thenReturn(List.of(
                new DateRange(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 14))
        ));

        RoomAvailabilityCalendar result = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, null, null);

        assertEquals(ROOM_ID, result.getRoomId());
        assertEquals(TODAY, result.getWindowStartDate());
        assertEquals(TODAY.plusYears(1), result.getWindowEndDate());
        assertEquals(1, result.getAvailabilityRanges().size());
        assertEquals(1, result.getBookedRanges().size());
        assertEquals(2, result.getSelectableRanges().size());
        assertRange(result.getSelectableRanges().get(0), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 9));
        assertRange(result.getSelectableRanges().get(1), LocalDate.of(2026, 2, 15), LocalDate.of(2026, 2, 28));
        assertEquals(LocalDate.of(2026, 2, 1), result.getFirstSelectableDate());
        assertTrue(result.isHasSelectableStay());
    }

    @Test
    public void testGetRoomAvailabilityCalendarFiltersPastDatesAndMergesBookings() {
        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, TODAY, TODAY.plusYears(1))).thenReturn(List.of(
                new RoomAvailability(room, new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20)))
        ));
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, TODAY, TODAY.plusYears(1))).thenReturn(List.of(
                new DateRange(LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 14)),
                new DateRange(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 16))
        ));

        RoomAvailabilityCalendar result = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, null, null);

        assertEquals(1, result.getBookedRanges().size());
        assertRange(result.getBookedRanges().get(0), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 16));
        assertEquals(2, result.getSelectableRanges().size());
        assertRange(result.getSelectableRanges().get(0), LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11));
        assertRange(result.getSelectableRanges().get(1), LocalDate.of(2026, 1, 17), LocalDate.of(2026, 1, 20));
        assertEquals(LocalDate.of(2026, 1, 10), result.getFirstSelectableDate());
    }

    @Test
    public void testGetRoomAvailabilityCalendarFullyBookedHasNoSelectableStay() {
        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, TODAY, TODAY.plusYears(1))).thenReturn(List.of(
                new RoomAvailability(room, new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10)))
        ));
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, TODAY, TODAY.plusYears(1))).thenReturn(List.of(
                new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 10))
        ));

        RoomAvailabilityCalendar result = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, null, null);

        assertTrue(result.getSelectableRanges().isEmpty());
        assertNull(result.getFirstSelectableDate());
        assertFalse(result.isHasSelectableStay());
    }

    @Test
    public void testGetRoomAvailabilityCalendarRoomNotFound() {
        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, null, null));
    }

    @Test
    public void testGetRoomAvailabilityCalendarWithRangeClipsOverlappingRanges() {
        LocalDate startDate = LocalDate.of(2026, 2, 5);
        LocalDate endDate = LocalDate.of(2026, 2, 20);

        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, startDate, endDate)).thenReturn(List.of(
                new RoomAvailability(room, new DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
        ));
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, startDate, endDate)).thenReturn(List.of(
                new DateRange(LocalDate.of(2026, 2, 18), LocalDate.of(2026, 2, 25))
        ));

        RoomAvailabilityCalendar result = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-02-05", "2026-02-20");

        assertEquals(startDate, result.getWindowStartDate());
        assertEquals(endDate, result.getWindowEndDate());
        assertEquals(1, result.getAvailabilityRanges().size());
        assertRange(result.getAvailabilityRanges().get(0), startDate, endDate);
        assertEquals(1, result.getBookedRanges().size());
        assertRange(result.getBookedRanges().get(0), LocalDate.of(2026, 2, 18), endDate);
        assertEquals(1, result.getSelectableRanges().size());
        assertRange(result.getSelectableRanges().get(0), startDate, LocalDate.of(2026, 2, 17));
        verify(roomAvailabilityService).getAvailabilitiesBetween(ROOM_ID, startDate, endDate);
        verify(contactService).contactAcceptedDatesForRoomBetween(ROOM_ID, startDate, endDate);
    }

    @Test
    public void testGetRoomAvailabilityCalendarWithOneDate_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-02-05", null)
        );
    }

    @Test
    public void testGetRoomAvailabilityCalendarWithInvalidDate_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-02-30", "2026-03-05")
        );
    }

    @Test
    public void testGetRoomAvailabilityCalendarWithStartAfterEnd_ThrowsIllegalArgumentException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-03-05", "2026-02-05")
        );
    }

    @Test
    public void testGetRoomAvailabilityCalendarHashChangesWhenWindowChanges() {
        LocalDate februaryStart = LocalDate.of(2026, 2, 1);
        LocalDate februaryEnd = LocalDate.of(2026, 2, 28);
        LocalDate marchStart = LocalDate.of(2026, 3, 1);
        LocalDate marchEnd = LocalDate.of(2026, 3, 31);

        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, februaryStart, februaryEnd)).thenReturn(List.of());
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, marchStart, marchEnd)).thenReturn(List.of());
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, februaryStart, februaryEnd)).thenReturn(List.of());
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, marchStart, marchEnd)).thenReturn(List.of());

        RoomAvailabilityCalendar february = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-02-01", "2026-02-28");
        RoomAvailabilityCalendar march = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-03-01", "2026-03-31");

        assertNotEquals(february.hashCode(), march.hashCode());
    }

    @Test
    public void testGetRoomAvailabilityCalendarHashChangesWhenSelectableRangesChangeWithToday() {
        LocalDate yesterday = LocalDate.of(2026, 1, 9);
        LocalDate windowStart = LocalDate.of(2026, 1, 1);
        LocalDate windowEnd = LocalDate.of(2026, 1, 20);

        when(roomService.findRoomById(ROOM_ID)).thenReturn(Optional.of(room));
        when(roomAvailabilityService.getAvailabilitiesBetween(ROOM_ID, windowStart, windowEnd)).thenReturn(List.of(
                new RoomAvailability(room, new DateRange(windowStart, windowEnd))
        ));
        when(contactService.contactAcceptedDatesForRoomBetween(ROOM_ID, windowStart, windowEnd)).thenReturn(List.of());

        RoomAvailabilityCalendar yesterdayCalendar = service.getRoomAvailabilityCalendar(ROOM_ID, yesterday, "2026-01-01", "2026-01-20");
        RoomAvailabilityCalendar todayCalendar = service.getRoomAvailabilityCalendar(ROOM_ID, TODAY, "2026-01-01", "2026-01-20");

        assertRange(yesterdayCalendar.getSelectableRanges().get(0), yesterday, windowEnd);
        assertRange(todayCalendar.getSelectableRanges().get(0), TODAY, windowEnd);
        assertNotEquals(yesterdayCalendar.hashCode(), todayCalendar.hashCode());
    }

    private void assertRange(DateRange range, LocalDate startDate, LocalDate endDate) {
        assertEquals(startDate, range.getStartDate());
        assertEquals(endDate, range.getEndDate());
    }
}
