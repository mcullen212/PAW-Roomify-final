package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.DTO.GroupTripListItem;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.webapp.utils.CountryUtils;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@XmlRootElement
public class GroupTripDTO {

    private long id;
    private String title;
    private String status;
    private String startDate;
    private String endDate;
    private Long tripId;
    private String country;
    private String countryCode;
    private String tripStartDate;
    private String tripEndDate;

    private Map<String, URI> links;

    public GroupTripDTO() {
        // Required by Jersey/MOXy.
    }

    public GroupTripDTO(final GroupTrip groupTrip, final UriInfo uriInfo) {
        this.id = groupTrip.getId();
        this.title = groupTrip.getTitle();
        this.status = groupTrip.getStatus() == null ? null : groupTrip.getStatus().name();
        this.startDate = groupTrip.getDateRange() == null || groupTrip.getDateRange().getStartDate() == null
                ? null
                : groupTrip.getDateRange().getStartDate().toString();
        this.endDate = groupTrip.getDateRange() == null || groupTrip.getDateRange().getEndDate() == null
                ? null
                : groupTrip.getDateRange().getEndDate().toString();

        this.links = new LinkedHashMap<>();
        this.links.put("self", uriInfo.getBaseUriBuilder()
                .path("group-trips")
                .path(String.valueOf(groupTrip.getId()))
                .build());
        this.links.put("owner", uriInfo.getBaseUriBuilder()
                .path("users")
                .path(String.valueOf(groupTrip.getOwner().getId()))
                .build());
        this.links.put("destinations", uriInfo.getBaseUriBuilder()
                .path("group-trips")
                .path(String.valueOf(groupTrip.getId()))
                .path("trips")
                .build());
    }

    public GroupTripDTO(final Trip trip, final UriInfo uriInfo) {
        this(trip.getGroupTrip(), uriInfo);
        this.tripId = trip.getId();
        this.country = trip.getCountry();
        this.countryCode = CountryUtils.getCountryCode(trip.getCountry()).orElse(null);
        this.tripStartDate = trip.getDateRange().getStartDate().toString();
        this.tripEndDate = trip.getDateRange().getEndDate().toString();
        this.links.put("matchedDestination", uriInfo.getBaseUriBuilder()
                .path("group-trips")
                .path(String.valueOf(trip.getGroupTrip().getId()))
                .path("trips")
                .path(String.valueOf(trip.getId()))
                .build());
    }

    public GroupTripDTO(final GroupTripListItem item, final UriInfo uriInfo) {
        this(item.getGroupTrip(), uriInfo);
        item.getMatchedTrip().ifPresent(trip -> {
            this.tripId = trip.getId();
            this.country = trip.getCountry();
            this.countryCode = CountryUtils.getCountryCode(trip.getCountry()).orElse(null);
            this.tripStartDate = trip.getDateRange().getStartDate().toString();
            this.tripEndDate = trip.getDateRange().getEndDate().toString();
            this.links.put("matchedDestination", uriInfo.getBaseUriBuilder()
                    .path("group-trips")
                    .path(String.valueOf(trip.getGroupTrip().getId()))
                    .path("trips")
                    .path(String.valueOf(trip.getId()))
                    .build());
        });
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getTripStartDate() {
        return tripStartDate;
    }

    public void setTripStartDate(String tripStartDate) {
        this.tripStartDate = tripStartDate;
    }

    public String getTripEndDate() {
        return tripEndDate;
    }

    public void setTripEndDate(String tripEndDate) {
        this.tripEndDate = tripEndDate;
    }

    @XmlElement(name = "_links")
    public Map<String, URI> getLinks() {
        return links;
    }

    public void setLinks(Map<String, URI> links) {
        this.links = links;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                title,
                status,
                startDate,
                endDate,
                tripId,
                country,
                countryCode,
                tripStartDate,
                tripEndDate,
                links
        );
    }
}
