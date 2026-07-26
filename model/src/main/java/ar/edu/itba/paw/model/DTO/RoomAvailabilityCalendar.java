package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.DateRange;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class RoomAvailabilityCalendar {
    private final long roomId;
    private final LocalDate windowStartDate;
    private final LocalDate windowEndDate;
    private final List<DateRange> availabilityRanges;
    private final List<DateRange> bookedRanges;
    private final List<DateRange> selectableRanges;
    private final LocalDate firstSelectableDate;

    public RoomAvailabilityCalendar(long roomId,
                                    LocalDate windowStartDate,
                                    LocalDate windowEndDate,
                                    List<DateRange> availabilityRanges,
                                    List<DateRange> bookedRanges,
                                    List<DateRange> selectableRanges,
                                    LocalDate firstSelectableDate) {
        this.roomId = roomId;
        this.windowStartDate = windowStartDate;
        this.windowEndDate = windowEndDate;
        this.availabilityRanges = availabilityRanges;
        this.bookedRanges = bookedRanges;
        this.selectableRanges = selectableRanges;
        this.firstSelectableDate = firstSelectableDate;
    }

    public long getRoomId() {
        return roomId;
    }

    public LocalDate getWindowStartDate() {
        return windowStartDate;
    }

    public LocalDate getWindowEndDate() {
        return windowEndDate;
    }

    public List<DateRange> getAvailabilityRanges() {
        return availabilityRanges;
    }

    public List<DateRange> getBookedRanges() {
        return bookedRanges;
    }

    public List<DateRange> getSelectableRanges() {
        return selectableRanges;
    }

    public LocalDate getFirstSelectableDate() {
        return firstSelectableDate;
    }

    public boolean isHasSelectableStay() {
        return firstSelectableDate != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                roomId,
                windowStartDate,
                windowEndDate,
                availabilityRanges,
                bookedRanges,
                selectableRanges,
                firstSelectableDate,
                isHasSelectableStay()
        );
    }
}
