package ar.edu.itba.paw.service.trips;

import ar.edu.itba.paw.interfaces.persistence.GroupTripDao;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.TripStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupTripMaintenanceService {
    private GroupTripDao groupTripDao;

    public GroupTripMaintenanceService(GroupTripDao groupTripDao) {
        this.groupTripDao = groupTripDao;
    }
    @Scheduled(cron = "0 0 2 * * *") // Runs every night at 2:00 AM
    @Transactional
    public void updateExpiredTripStatuses() {
        List<GroupTrip> expiredTrips = groupTripDao.findExpiredUpcomingTrips();

        if (expiredTrips.isEmpty()) {
            return;
        }

        for (GroupTrip trip : expiredTrips) {
            groupTripDao.updateStatus(trip.getId(), TripStatus.DONE);
        }
    }
}
