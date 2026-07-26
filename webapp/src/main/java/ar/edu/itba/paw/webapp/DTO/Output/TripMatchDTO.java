package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.DTO.TripMatch;
import ar.edu.itba.paw.model.trip.Trip;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@XmlRootElement
public class TripMatchDTO {

    private String decision;
    private Long tripId;
    private Long groupTripId;
    private String groupTripTitle;
    private String startDate;
    private String endDate;

    private Map<String, URI> links;

    public TripMatchDTO() {
        // Required by Jersey/MOXy.
    }

    public TripMatchDTO(final TripMatch match, final UriInfo uriInfo) {
        this.decision = match.getDecision().name();
        this.links = new LinkedHashMap<>();

        match.getTrip().ifPresent(trip -> {
            this.tripId = trip.getId();
            this.groupTripId = trip.getGroupTrip().getId();
            this.groupTripTitle = trip.getGroupTrip().getTitle();
            this.startDate = trip.getDateRange().getStartDate().toString();
            this.endDate = trip.getDateRange().getEndDate().toString();

            this.links.put("trip", uriInfo.getBaseUriBuilder()
                    .path("group-trips")
                    .path(String.valueOf(trip.getGroupTrip().getId()))
                    .path("trips")
                    .path(String.valueOf(trip.getId()))
                    .build());
            this.links.put("groupTrip", uriInfo.getBaseUriBuilder()
                    .path("group-trips")
                    .path(String.valueOf(trip.getGroupTrip().getId()))
                    .build());
        });
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getGroupTripId() {
        return groupTripId;
    }

    public void setGroupTripId(Long groupTripId) {
        this.groupTripId = groupTripId;
    }

    public String getGroupTripTitle() {
        return groupTripTitle;
    }

    public void setGroupTripTitle(String groupTripTitle) {
        this.groupTripTitle = groupTripTitle;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @XmlElement(name = "_links")
    public Map<String, URI> getLinks() {
        return links;
    }

    public void setLinks(Map<String, URI> links) {
        this.links = links;
    }
}
