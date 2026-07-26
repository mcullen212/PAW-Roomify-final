package ar.edu.itba.paw.webapp.DTO.Output;

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
public class TripDTO {

    private long id;
    private String country;
    private String countryCode;
    private String startDate;
    private String endDate;

    private Map<String, URI> links;

    public TripDTO() {
        // Required by Jersey/MOXy.
    }

    public TripDTO(final Trip trip, final UriInfo uriInfo) {
        this.id = trip.getId();
        this.country = trip.getCountry();
        this.countryCode = CountryUtils.getCountryCode(trip.getCountry()).orElse(null);
        this.startDate = trip.getDateRange().getStartDate().toString();
        this.endDate = trip.getDateRange().getEndDate().toString();

        this.links = new LinkedHashMap<>();
        this.links.put("self", uriInfo.getBaseUriBuilder()
                .path("group-trips")
                .path(String.valueOf(trip.getGroupTrip().getId()))
                .path("trips")
                .path(String.valueOf(trip.getId()))
                .build());
        this.links.put("groupTrip", uriInfo.getBaseUriBuilder()
                .path("group-trips")
                .path(String.valueOf(trip.getGroupTrip().getId()))
                .build());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    @Override
    public int hashCode() {
        return Objects.hash(id, country, countryCode, startDate, endDate, links);
    }
}
