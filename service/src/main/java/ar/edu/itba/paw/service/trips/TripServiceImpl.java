package ar.edu.itba.paw.service.trips;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.interfaces.exceptions.GroupTripNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.RoomNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.TripNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.TripDao;
import ar.edu.itba.paw.interfaces.service.GroupTripService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.interfaces.service.TripService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.TripMatch;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TripServiceImpl implements TripService {
    private final GroupTripService groupTripService;
    private final TripDao tripDao;
    private final UserService userService;
    private final RoomService roomService;

    public TripServiceImpl(final TripDao tripDao, final GroupTripService groupTripService, final UserService userService, final RoomService roomService) {
        this.tripDao = tripDao;
        this.groupTripService = groupTripService;
        this.userService = userService;
        this.roomService = roomService;
    }

    @Transactional
    @Override
    public Trip create(long groupTripId, String country, LocalDate startDate, LocalDate endDate) throws DateRangeException {
        GroupTrip groupTrip = groupTripService.findGroupTripById(groupTripId).orElseThrow(() -> new GroupTripNotFoundException(groupTripId));

        DateRange dateRange = new DateRange(startDate, endDate);

        if(checkDates(startDate, endDate)){
            throw new DateRangeException("Start date must be before end date.", "specific.errors.swap.dates.outside.trip", groupTripId);
        }

        if (tripDao.existsOverlappingTrip(groupTripId, dateRange)) {
            throw new DateRangeException("Date range overlap.", "plan.trip.error.date.overlap", groupTripId);
        }

        Trip newTrip = tripDao.create(groupTripId, country, dateRange);

        groupTripService.recalculateDates(groupTripId);

        return newTrip;
    }

    private boolean checkDates(LocalDate startDate, LocalDate endDate) {
        return startDate.isAfter(endDate);
    }

    @Transactional
    @Override
    public Optional<Trip> findTripById(long tripId) {
        return tripDao.findTripById(tripId);
    }

    @Transactional
    @Override
    public Trip findTripByGroupTripId(long groupTripId, long tripId) {
        validateGroupTripExists(groupTripId);
        Trip trip = tripDao.findTripById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId));

        if (trip.getGroupTrip().getId() != groupTripId) {
            throw new TripNotFoundException(tripId);
        }

        return trip;
    }

    @Transactional
    @Override
    public List<Trip> findTripsByGroupTripId(long groupTripId, int page, int pageSize) {
        validateGroupTripExists(groupTripId);
        return tripDao.findTripsByGroupTripId(groupTripId, page, pageSize);
    }

    @Transactional
    @Override
    public List<Trip> bringMyTrips(String country, String checkIn, String checkOut, Long userId, int page, int pageSize) {
        validateUserId(userId);
        DateRange dateRange = parseOptionalTripFilterDateRange(checkIn, checkOut);
        User user = findUserByIdOrThrow(userId);

        return tripDao.bringMytrips(
                country,
                dateRange.getStartDate(),
                dateRange.getEndDate(),
                user,
                page,
                pageSize
        );
    }

    @Transactional
    @Override
    public int countMyTrips(String country, String checkIn, String checkOut, Long userId) {
        validateUserId(userId);
        DateRange dateRange = parseOptionalTripFilterDateRange(checkIn, checkOut);
        User user = findUserByIdOrThrow(userId);

        return tripDao.countMyTrips(
                country,
                dateRange.getStartDate(),
                dateRange.getEndDate(),
                user
        );
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Query param 'userId' must be a positive number.");
        }
    }

    private User findUserByIdOrThrow(Long userId) {
        return userService.findUserById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    private DateRange parseOptionalTripFilterDateRange(String checkIn, String checkOut) {
        final boolean hasCheckIn = hasText(checkIn);
        final boolean hasCheckOut = hasText(checkOut);

        if (!hasCheckIn && !hasCheckOut) {
            return new DateRange((LocalDate) null, null);
        }

        if (!hasCheckIn || !hasCheckOut) {
            throw new IllegalArgumentException("Query params 'checkIn' and 'checkOut' must be provided together.");
        }

        return new DateRange(checkIn, checkOut, "checkIn", "checkOut");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Transactional
    @Override
    public int countTripsByGroupTripId(long groupTripId) {
        validateGroupTripExists(groupTripId);
        return tripDao.countTripsByGroupTripId(groupTripId);
    }

    private void validateGroupTripExists(long groupTripId) {
        groupTripService.findGroupTripById(groupTripId)
                .orElseThrow(() -> new GroupTripNotFoundException(groupTripId));
    }

    @Transactional
    @Override
    public Trip matchRoomToTrip(Long roomId, long groupTripId, long tripId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("Field 'roomId' must be a positive number.");
        }

        Trip trip = findTripByGroupTripId(groupTripId, tripId);
        Room room = roomService.findRoomById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        if (!trip.getCountry().equalsIgnoreCase(room.getCountry())) {
            throw new IllegalArgumentException("Room country does not match trip country.");
        }

        return trip;
    }

    @Transactional
    @Override
    public TripMatch matchRoomToTrip(long roomId, LocalDate checkIn, LocalDate checkOut, String email) {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        Room room = roomService.findRoomById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
        String country = room.getCountry();

        Optional<Trip> containing = tripDao.findContainingActiveTrip(country, checkIn, checkOut, user);
        return containing.map(TripMatch::contained).orElseGet(() -> tripDao.findNearestActiveTripInCountry(country, user)
                .map(TripMatch::datesOutside)
                .orElseGet(TripMatch::none));

    }
}
