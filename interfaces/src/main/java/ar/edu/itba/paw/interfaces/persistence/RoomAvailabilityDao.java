package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.RoomAvailability;

import java.time.LocalDate;
import java.util.List;

public interface RoomAvailabilityDao {
    RoomAvailability create(long roomId, DateRange dateRange);
    List<RoomAvailability> findByRoom(long roomId);
    List<RoomAvailability> findByRoomBetween(long roomId, LocalDate startDate, LocalDate endDate);
    Long findAvailableIdByRoom(long roomId);
    boolean inAvailabilityRanges(long roomRequestedId, LocalDate startDate, LocalDate endDate);

}
