package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.DTO.RoomAvailabilityCalendar;

import java.time.LocalDate;

public interface RoomAvailabilityCalendarService {
    RoomAvailabilityCalendar getRoomAvailabilityCalendar(long roomId, LocalDate today, String startDate, String endDate);
}
