package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.DTO.RoomCardResult;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Objects;

@XmlRootElement
public class RoomCardDTO {
    private long id;
    private String title;
    private String country;
    private String city;
    private URI imageUrl;
    private BigDecimal dayPrice;
    private String roomType;
    private int totalReviews;
    private double averageRating;
    private URI self;

    public RoomCardDTO() {}

    public RoomCardDTO(final Room room, final UriInfo uriInfo) {
        this(room, uriInfo, 0, 0);
    }

    public RoomCardDTO(final RoomCardResult result, final UriInfo uriInfo) {
        this(result.getRoom(), uriInfo, result.getTotalReviews(), result.getAverageRating());
    }

    public RoomCardDTO(final Room room, final UriInfo uriInfo, final int totalReviews, final double averageRating) {
        this.id = room.getId();
        this.title = room.getTitle();
        this.dayPrice = room.getDayPrice();
        this.roomType = room.getRoomType().name();
        this.country = room.getCountry();
        this.city = room.getCity();

        // Image handling
        this.imageUrl = uriInfo.getBaseUriBuilder()
                .path("images")
                .path(String.valueOf(room.getImageId()))
                .build();
        this.self = uriInfo.getBaseUriBuilder()
                .path("rooms")
                .path(String.valueOf(room.getId()))
                .build();
        this.totalReviews = totalReviews;
        this.averageRating = averageRating;
    }

    public void setDayPrice(BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }
    public void setRoomType(final String roomType) {
        this.roomType = roomType;
    }
    public void setImageUrl(final URI imageUrl) {
        this.imageUrl = imageUrl;
    }
    public void setTitle(final String title) {
        this.title = title;
    }
    public void setId(long id) {
        this.id = id;
    }
    public void setCountry(final String country) {
        this.country = country;
    }
    public void setCity(final String city) {
        this.city = city;
    }
    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
    public void setSelf(URI self) {
        this.self = self;
    }
    public long getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public URI getImageUrl() {
        return imageUrl;
    }
    public String getCountry() {
        return country;
    }
    public String getCity() {
        return city;
    }
    public BigDecimal getDayPrice() {
        return dayPrice;
    }
    public String getRoomType() {
        return roomType;
    }
    public int getTotalReviews() {
        return totalReviews;
    }
    public double getAverageRating() {
        return averageRating;
    }
    public URI getSelf() {
        return self;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                title,
                country,
                city,
                imageUrl,
                dayPrice,
                roomType,
                totalReviews,
                averageRating,
                self
        );
    }
}
