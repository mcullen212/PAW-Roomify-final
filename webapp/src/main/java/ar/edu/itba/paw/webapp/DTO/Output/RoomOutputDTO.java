package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.Room;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@XmlRootElement
public class RoomOutputDTO {
    private long id;
    private String title;
    private String country;
    private String city;
    private String description;
    private String bedType;
    private String roomType;
    private boolean privateBathroom;
    private boolean privateKitchen;
    private List<String> amenities;
    private URI imageUrl;
    private URI self;
    private URI owner;
    private URI images;
    private URI reviews;
    private BigDecimal dayPrice;

    public RoomOutputDTO() {
        // Required by Jersey/MOXy.
    }

    public RoomOutputDTO(final Room room, final UriInfo uriInfo) {
        this.id = room.getId();
        this.title = room.getTitle();
        this.dayPrice = room.getDayPrice();
        this.country = room.getCountry();
        this.city = room.getCity();
        this.description = room.getDescription();
        this.bedType = room.getBedType().name();
        this.roomType = room.getRoomType().name();
        this.privateBathroom = room.isPrivateBathroom();
        this.privateKitchen = room.isPrivateKitchen();
        this.amenities = room.getAmenitiesEnums().stream()
                .map(Amenity::name)
                .collect(Collectors.toList());

        this.imageUrl = uriInfo.getBaseUriBuilder()
                .path("images")
                .path(String.valueOf(room.getImageId()))
                .build();

        this.self = uriInfo.getBaseUriBuilder()
                .path("rooms")
                .path(String.valueOf(room.getId()))
                .build();
        this.owner = uriInfo.getBaseUriBuilder()
                .path("users")
                .path(String.valueOf(room.getOwner().getId()))
                .build();
        this.images = this.imageUrl;
        this.reviews = uriInfo.getBaseUriBuilder()
                .path("reviews")
                .queryParam("roomId", room.getId())
                .build();
    }

    // --- GETTERS ---
    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getDescription() { return description; }
    public String getBedType() { return bedType; }
    public String getRoomType() { return roomType; }
    public boolean isPrivateBathroom() { return privateBathroom; }
    public boolean isPrivateKitchen() { return privateKitchen; }
    public List<String> getAmenities() { return amenities; }
    public URI getImageUrl() { return imageUrl; }
    public URI getSelf() { return self; }
    public URI getOwner() { return owner; }
    public URI getImages() { return images; }
    public URI getReviews() { return reviews; }
    public BigDecimal getDayPrice() { return dayPrice; }

    // --- SETTERS ---
    public void setId(long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCountry(String country) { this.country = country; }
    public void setCity(String city) { this.city = city; }
    public void setDescription(String description) { this.description = description; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setPrivateBathroom(boolean privateBathroom) { this.privateBathroom = privateBathroom; }
    public void setPrivateKitchen(boolean privateKitchen) { this.privateKitchen = privateKitchen; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public void setImageUrl(URI imageUrl) { this.imageUrl = imageUrl; }
    public void setSelf(URI self) { this.self = self; }
    public void setOwner(URI owner) { this.owner = owner; }
    public void setImages(URI images) { this.images = images; }
    public void setReviews(URI reviews) { this.reviews = reviews; }
    public void setDayPrice(BigDecimal dayPrice) { this.dayPrice = dayPrice; }
}
