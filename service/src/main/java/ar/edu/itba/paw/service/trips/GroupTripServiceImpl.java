package ar.edu.itba.paw.service.trips;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.interfaces.exceptions.GroupTripNotFoundException;
import ar.edu.itba.paw.interfaces.exceptions.InvalidTripStateException;
import ar.edu.itba.paw.interfaces.persistence.GroupTripDao;
import ar.edu.itba.paw.interfaces.service.GroupTripService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.DateRange;
import ar.edu.itba.paw.model.DTO.GroupTripListItem;
import ar.edu.itba.paw.model.DTO.GroupTripPage;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class GroupTripServiceImpl implements GroupTripService {
    private final GroupTripDao groupTripDao;
    private final UserService userService;


    public GroupTripServiceImpl(GroupTripDao groupTripDao, UserService userService) {
        this.groupTripDao = groupTripDao;
        this.userService = userService;
    }

    @Transactional
    @Override
    public GroupTrip create(long ownerId, String name){
        if (ownerId <= 0) {
            throw new IllegalArgumentException("User id must be positive when creating a GroupTrip.");
        }
        User user = userService.findUserById(ownerId).orElseThrow(()->  new UsernameNotFoundException("User not found"));

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("GroupTrip name cannot be empty.");
        }

        DateRange dateRange = new DateRange((LocalDate) null, null);

        return groupTripDao.create(user, name, dateRange);
    }

    @Override
    public Optional<GroupTrip> findGroupTripById(Long groupTripId) {
        return groupTripDao.findGroupTripById(groupTripId);
    }

    @Transactional
    @Override
    public void recalculateDates(long groupTripId) {
        groupTripDao.findGroupTripById(groupTripId)
                .orElseThrow(() -> new GroupTripNotFoundException(groupTripId));

        Optional<LocalDate> earliestStartDate = groupTripDao.getEarliestTripStartDate(groupTripId);
        Optional<LocalDate> latestEndDate = groupTripDao.getLatestTripEndDate(groupTripId);

        LocalDate newStartDate = earliestStartDate.orElse(null);
        LocalDate newEndDate = latestEndDate.orElse(null);

        DateRange newDateRange = new DateRange(newStartDate, newEndDate);

        groupTripDao.updateDates(groupTripId, newDateRange);
    }

    @Transactional
    @Override
    public List<GroupTrip> getGroupTripForUser(String email, TripStatus tripStatus, int page, int pageSize) {
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }

        long ownerId = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email")).getId();

        return groupTripDao.findGroupTripsByOwnerId(ownerId, tripStatus, page, pageSize);
    }

    @Transactional
    @Override
    public List<GroupTrip> getGroupTripForUser(Long userId, TripStatus tripStatus, int page, int pageSize) {
        validateUserId(userId);
        return groupTripDao.findGroupTripsByOwnerId(userId, tripStatus, page, pageSize);
    }

    @Override
    public int getGroupTripCountForUser(String email,  TripStatus tripStatus) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty.");
        }
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email"));

        return groupTripDao.countGroupTripsByOwnerId(user.getId(), tripStatus);
    }

    @Override
    public int getGroupTripCountForUser(Long userId, TripStatus tripStatus) {
        validateUserId(userId);
        return groupTripDao.countGroupTripsByOwnerId(userId, tripStatus);
    }

    @Transactional
    @Override
    public GroupTripPage searchGroupTrips(Long userId, String country, String checkIn, String checkOut, TripStatus tripStatus, int page, int pageSize) {
        validateUserId(userId);

        if (hasTripFilters(country, checkIn, checkOut)) {
            DateRange dateRange = parseOptionalTripFilterDateRange(checkIn, checkOut);
            List<GroupTripListItem> items = groupTripDao.findTripsForGroupTripAssociation(
                            userId,
                            TripStatus.PLANNING,
                            country,
                            dateRange.getStartDate(),
                            dateRange.getEndDate(),
                            page,
                            pageSize
                    )
                    .stream()
                    .map(GroupTripListItem::new)
                    .toList();
            int totalItems = groupTripDao.countTripsForGroupTripAssociation(
                    userId,
                    TripStatus.PLANNING,
                    country,
                    dateRange.getStartDate(),
                    dateRange.getEndDate()
            );
            return new GroupTripPage(items, totalItems, pageSize);
        }

        List<GroupTripListItem> items = groupTripDao.findGroupTripsByOwnerId(userId, tripStatus, page, pageSize)
                .stream()
                .map(GroupTripListItem::new)
                .toList();
        int totalItems = groupTripDao.countGroupTripsByOwnerId(userId, tripStatus);
        return new GroupTripPage(items, totalItems, pageSize);
    }

    @Transactional
    @Override
    public List<Trip> getTripsForGroupTripAssociation(Long userId, String country, String checkIn, String checkOut, TripStatus tripStatus, int page, int pageSize) {
        validateUserId(userId);
        DateRange dateRange = parseOptionalTripFilterDateRange(checkIn, checkOut);

        return groupTripDao.findTripsForGroupTripAssociation(
                userId,
                tripStatus,
                country,
                dateRange.getStartDate(),
                dateRange.getEndDate(),
                page,
                pageSize
        );
    }

    @Transactional
    @Override
    public int getTripsForGroupTripAssociationCount(Long userId, String country, String checkIn, String checkOut, TripStatus tripStatus) {
        validateUserId(userId);
        DateRange dateRange = parseOptionalTripFilterDateRange(checkIn, checkOut);

        return groupTripDao.countTripsForGroupTripAssociation(
                userId,
                tripStatus,
                country,
                dateRange.getStartDate(),
                dateRange.getEndDate()
        );
    }

    @Override
    public boolean isOwnerTrip(String email, long groupTripId){
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found for email"));
        GroupTrip groupTrip = groupTripDao.findGroupTripById(groupTripId)
                .orElseThrow(() -> new GroupTripNotFoundException(groupTripId));
        return groupTrip.getOwner().getId() == user.getId();
    }

    @Override
    @Transactional
    public GroupTrip updateGroupTrip(long groupId, String title, TripStatus targetStatus) {
        if (title != null && !title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title updates are not supported yet.");
        }

        updateStatus(groupId, targetStatus);

        return findGroupTripById(groupId)
                .orElseThrow(() -> new GroupTripNotFoundException(groupId));
    }

    @Override
    @Transactional
    public boolean updateStatus(long groupId, TripStatus targetStatus) {
        if (targetStatus == TripStatus.PLANNING) {
            throw new IllegalArgumentException("Cannot move a group trip back to PLANNING.");
        }
        if (targetStatus == TripStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancellation is not supported yet.");
        }

        return finishedPlanning(groupId);
    }

    @Override
    @Transactional
    public boolean finishedPlanning(long groupId) {
        GroupTrip groupTrip = findGroupTripById(groupId)
                .orElseThrow(() -> new GroupTripNotFoundException(groupId));

        if (groupTrip.getStatus() != TripStatus.PLANNING) {
            throw new InvalidTripStateException("Trip status must be PLANNING to be completed.", "plan.trips.status.not.planning", groupId);
        }

        if (
                groupTrip.getDateRange() == null
                        || groupTrip.getDateRange().getStartDate() == null
                        || groupTrip.getDateRange().getEndDate() == null
        ) {
            throw new DateRangeException("Date range cannot be empty.","plan.trips.date.range.empty",groupId);
        }

        if (groupTrip.getDateRange().isBeforeToday()) {
            return groupTripDao.updateStatus(groupId, TripStatus.DONE);
        }

        return groupTripDao.updateStatus(groupId, TripStatus.UPCOMING);
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Query param 'userId' must be a positive number.");
        }
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

    private boolean hasTripFilters(String country, String checkIn, String checkOut) {
        return hasText(country) || hasText(checkIn) || hasText(checkOut);
    }
}
