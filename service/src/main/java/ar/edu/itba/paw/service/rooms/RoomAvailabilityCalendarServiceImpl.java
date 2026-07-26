package ar.edu.itba.paw.service.rooms;

import ar.edu.itba.paw.interfaces.exceptions.RoomNotFoundException;
import ar.edu.itba.paw.interfaces.service.ContactService;
import ar.edu.itba.paw.interfaces.service.RoomAvailabilityCalendarService;
import ar.edu.itba.paw.interfaces.service.RoomAvailabilityService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.model.DTO.RoomAvailabilityCalendar;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RoomAvailabilityCalendarServiceImpl implements RoomAvailabilityCalendarService {
    private final RoomService roomService;
    private final RoomAvailabilityService roomAvailabilityService;
    private final ContactService contactService;

    public RoomAvailabilityCalendarServiceImpl(RoomService roomService,
                                               RoomAvailabilityService roomAvailabilityService,
                                               ContactService contactService) {
        this.roomService = roomService;
        this.roomAvailabilityService = roomAvailabilityService;
        this.contactService = contactService;
    }

    @Override
    public RoomAvailabilityCalendar getRoomAvailabilityCalendar(long roomId,
                                                                LocalDate today,
                                                                String startDate,
                                                                String endDate) {
        if (today == null) {
            throw new IllegalArgumentException("Today is required.");
        }

        DateRange window = resolveWindow(startDate, endDate, today);
        return buildRoomAvailabilityCalendar(roomId, today, window.getStartDate(), window.getEndDate());
    }

    private RoomAvailabilityCalendar buildRoomAvailabilityCalendar(long roomId,
                                                                   LocalDate today,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate.");
        }

        roomService.findRoomById(roomId).orElseThrow(() -> new RoomNotFoundException(roomId));

        List<DateRange> availabilityRanges = roomAvailabilityService.getAvailabilitiesBetween(roomId, startDate, endDate).stream()
                .map(RoomAvailability::getRange)
                .map(range -> clipRange(range, startDate, endDate))
                .filter(range -> range != null)
                .sorted(Comparator.comparing(DateRange::getStartDate))
                .toList();

        List<DateRange> bookedRanges = contactService.contactAcceptedDatesForRoomBetween(roomId, startDate, endDate).stream()
                .map(range -> clipRange(range, startDate, endDate))
                .filter(range -> range != null)
                .toList();
        List<DateRange> normalizedBookedRanges = mergeRanges(bookedRanges);
        List<DateRange> selectableRanges = subtractBookedRanges(availabilityRanges, normalizedBookedRanges, today);
        LocalDate firstSelectableDate = selectableRanges.stream()
                .filter(range -> range.getEndDate().isAfter(range.getStartDate()))
                .map(DateRange::getStartDate)
                .min(LocalDate::compareTo)
                .orElse(null);

        return new RoomAvailabilityCalendar(
                roomId,
                startDate,
                endDate,
                availabilityRanges,
                normalizedBookedRanges,
                selectableRanges,
                firstSelectableDate
        );
    }

    private DateRange resolveWindow(String startDate, String endDate, LocalDate today) {
        if (!hasText(startDate) && !hasText(endDate)) {
            return new DateRange(today, today.plusYears(1));
        }

        if (!hasText(startDate) || !hasText(endDate)) {
            throw new IllegalArgumentException("startDate and endDate must be provided together.");
        }

        LocalDate parsedStartDate = parseDate(startDate, "startDate");
        LocalDate parsedEndDate = parseDate(endDate, "endDate");

        if (parsedStartDate.isAfter(parsedEndDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate.");
        }

        return new DateRange(parsedStartDate, parsedEndDate);
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid ISO-8601 date for field '" + fieldName + "'. Expected yyyy-MM-dd.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private DateRange clipRange(DateRange range, LocalDate startDate, LocalDate endDate) {
        if (range.getEndDate().isBefore(startDate) || range.getStartDate().isAfter(endDate)) {
            return null;
        }

        LocalDate clippedStart = range.getStartDate().isBefore(startDate)
                ? startDate
                : range.getStartDate();
        LocalDate clippedEnd = range.getEndDate().isAfter(endDate)
                ? endDate
                : range.getEndDate();

        return new DateRange(clippedStart, clippedEnd);
    }

    private List<DateRange> mergeRanges(List<DateRange> ranges) {
        if (ranges.isEmpty()) {
            return List.of();
        }

        List<DateRange> sortedRanges = ranges.stream()
                .filter(range -> range.getStartDate() != null && range.getEndDate() != null)
                .sorted(Comparator.comparing(DateRange::getStartDate))
                .toList();

        List<DateRange> mergedRanges = new ArrayList<>();
        for (DateRange range : sortedRanges) {
            if (mergedRanges.isEmpty()) {
                mergedRanges.add(new DateRange(range.getStartDate(), range.getEndDate()));
                continue;
            }

            DateRange last = mergedRanges.getLast();
            if (!range.getStartDate().isAfter(last.getEndDate().plusDays(1))) {
                LocalDate mergedEnd = range.getEndDate().isAfter(last.getEndDate())
                        ? range.getEndDate()
                        : last.getEndDate();
                mergedRanges.set(mergedRanges.size() - 1, new DateRange(last.getStartDate(), mergedEnd));
            } else {
                mergedRanges.add(new DateRange(range.getStartDate(), range.getEndDate()));
            }
        }

        return mergedRanges;
    }

    private List<DateRange> subtractBookedRanges(List<DateRange> availabilityRanges,
                                                 List<DateRange> bookedRanges,
                                                 LocalDate today) {
        List<DateRange> selectableRanges = new ArrayList<>();

        for (DateRange availability : availabilityRanges) {
            LocalDate cursor = availability.getStartDate().isBefore(today)
                    ? today
                    : availability.getStartDate();
            LocalDate availabilityEnd = availability.getEndDate();

            if (cursor.isAfter(availabilityEnd)) {
                continue;
            }

            for (DateRange booked : bookedRanges) {
                if (booked.getEndDate().isBefore(cursor) || booked.getStartDate().isAfter(availabilityEnd)) {
                    continue;
                }

                if (booked.getStartDate().isAfter(cursor)) {
                    selectableRanges.add(new DateRange(cursor, booked.getStartDate().minusDays(1)));
                }

                if (!booked.getEndDate().isBefore(cursor)) {
                    cursor = booked.getEndDate().plusDays(1);
                }

                if (cursor.isAfter(availabilityEnd)) {
                    break;
                }
            }

            if (!cursor.isAfter(availabilityEnd)) {
                selectableRanges.add(new DateRange(cursor, availabilityEnd));
            }
        }

        return selectableRanges;
    }
}
