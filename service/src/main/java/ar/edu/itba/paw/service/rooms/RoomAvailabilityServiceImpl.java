package ar.edu.itba.paw.service.rooms;

import ar.edu.itba.paw.interfaces.persistence.RoomAvailabilityDao;
import ar.edu.itba.paw.interfaces.service.RoomAvailabilityService;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.rooms.RoomAvailability;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Service
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {

    RoomAvailabilityDao availabilityDao;

    public RoomAvailabilityServiceImpl(RoomAvailabilityDao availabilityDao) {
        this.availabilityDao = availabilityDao;
    }

    @Transactional
    @Override
    public RoomAvailability addAvailability(long roomId, DateRange dateRange) {
        if (dateRange.getStartDate() == null || dateRange.getEndDate() == null) {
            throw new IllegalArgumentException("You must insert starting and ending dates");
        }

        if (dateRange.getStartDate().isAfter(dateRange.getEndDate()) || dateRange.getStartDate().isEqual(dateRange.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after or equal to end date");
        }

        if (dateRange.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        RoomAvailability ra = availabilityDao.create(roomId, dateRange);
        return ra;
    }

    @Override
    public List<RoomAvailability> getAvailabilities(long roomId) {
        return availabilityDao.findByRoom(roomId);
    }

    @Override
    public List<RoomAvailability> getAvailabilitiesBetween(long roomId, LocalDate startDate, LocalDate endDate) {
        return availabilityDao.findByRoomBetween(roomId, startDate, endDate);
    }

    @Override
    public Long findAvailableIdByRoom(long roomId) {
        return availabilityDao.findAvailableIdByRoom(roomId);
    }

    @Override
    public DateRange getAvailabilityDatesForRoom(long roomId){
        RoomAvailability ra =  availabilityDao.findByRoom(roomId).getFirst();
        return ra.getRange();
    }

    @Override
    public boolean inAvailabilityRanges(long roomRequestedId, LocalDate startDate, LocalDate endDate){
        return availabilityDao.inAvailabilityRanges(roomRequestedId,startDate,endDate);
    }

}
