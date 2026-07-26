package ar.edu.itba.paw.interfaces.service;

import java.time.LocalDate;
import java.util.List;

import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.RoomAvailability;

public interface RoomAvailabilityService {
    RoomAvailability addAvailability(long roomId, DateRange dateRange);
    List<RoomAvailability> getAvailabilities(long roomId);
    List<RoomAvailability> getAvailabilitiesBetween(long roomId, LocalDate startDate, LocalDate endDate);
    Long findAvailableIdByRoom(long roomId);
    DateRange getAvailabilityDatesForRoom(long roomId);
    boolean inAvailabilityRanges(long roomRequestedId, LocalDate startDate, LocalDate endDate);
    }
