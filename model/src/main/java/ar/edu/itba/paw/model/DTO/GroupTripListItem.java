package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;

import java.util.Optional;

public class GroupTripListItem {
    private final GroupTrip groupTrip;
    private final Trip matchedTrip;

    public GroupTripListItem(GroupTrip groupTrip) {
        this(groupTrip, null);
    }

    public GroupTripListItem(Trip matchedTrip) {
        this(matchedTrip.getGroupTrip(), matchedTrip);
    }

    public GroupTripListItem(GroupTrip groupTrip, Trip matchedTrip) {
        this.groupTrip = groupTrip;
        this.matchedTrip = matchedTrip;
    }

    public GroupTrip getGroupTrip() {
        return groupTrip;
    }

    public Optional<Trip> getMatchedTrip() {
        return Optional.ofNullable(matchedTrip);
    }
}
