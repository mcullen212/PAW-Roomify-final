package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.DTO.RoomAvailabilityCalendar;
import ar.edu.itba.paw.model.DateRange;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.List;

@XmlRootElement
public class RoomAvailabilityCalendarDTO {
    private long roomId;
    private List<DateRangeOutputDTO> availabilityRanges;
    private List<DateRangeOutputDTO> bookedRanges;
    private List<DateRangeOutputDTO> selectableRanges;
    private String firstSelectableDate;
    private boolean hasSelectableStay;
    private URI self;
    private URI room;

    public RoomAvailabilityCalendarDTO() {
        // Required by Jersey/MOXy.
    }

    public RoomAvailabilityCalendarDTO(RoomAvailabilityCalendar calendar, UriInfo uriInfo) {
        this.roomId = calendar.getRoomId();
        this.availabilityRanges = toDateRangeDTOs(calendar.getAvailabilityRanges());
        this.bookedRanges = toDateRangeDTOs(calendar.getBookedRanges());
        this.selectableRanges = toDateRangeDTOs(calendar.getSelectableRanges());
        this.firstSelectableDate = calendar.getFirstSelectableDate() != null
                ? calendar.getFirstSelectableDate().toString()
                : null;
        this.hasSelectableStay = calendar.isHasSelectableStay();
        this.self = uriInfo.getRequestUri();
        this.room = uriInfo.getBaseUriBuilder()
                .path("rooms")
                .path(String.valueOf(calendar.getRoomId()))
                .build();
    }

    private List<DateRangeOutputDTO> toDateRangeDTOs(List<DateRange> ranges) {
        return ranges.stream()
                .map(DateRangeOutputDTO::new)
                .toList();
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public List<DateRangeOutputDTO> getAvailabilityRanges() {
        return availabilityRanges;
    }

    public void setAvailabilityRanges(List<DateRangeOutputDTO> availabilityRanges) {
        this.availabilityRanges = availabilityRanges;
    }

    public List<DateRangeOutputDTO> getBookedRanges() {
        return bookedRanges;
    }

    public void setBookedRanges(List<DateRangeOutputDTO> bookedRanges) {
        this.bookedRanges = bookedRanges;
    }

    public List<DateRangeOutputDTO> getSelectableRanges() {
        return selectableRanges;
    }

    public void setSelectableRanges(List<DateRangeOutputDTO> selectableRanges) {
        this.selectableRanges = selectableRanges;
    }

    public String getFirstSelectableDate() {
        return firstSelectableDate;
    }

    public void setFirstSelectableDate(String firstSelectableDate) {
        this.firstSelectableDate = firstSelectableDate;
    }

    public boolean isHasSelectableStay() {
        return hasSelectableStay;
    }

    public void setHasSelectableStay(boolean hasSelectableStay) {
        this.hasSelectableStay = hasSelectableStay;
    }

    public URI getSelf() {
        return self;
    }

    public void setSelf(URI self) {
        this.self = self;
    }

    public URI getRoom() {
        return room;
    }

    public void setRoom(URI room) {
        this.room = room;
    }
}
