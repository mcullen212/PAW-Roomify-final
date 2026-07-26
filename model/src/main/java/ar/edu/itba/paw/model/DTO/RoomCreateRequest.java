package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.DateRange;

import java.math.BigDecimal;
import java.util.List;

public class RoomCreateRequest {

    private final Long userId;
    private final String title;
    private final String country;
    private final String city;
    private final String description;
    private final String roomType;
    private final String bedType;
    private final Boolean privateBathroom;
    private final Boolean privateKitchen;
    private final List<String> amenities;
    private final List<DateRange> dateRanges;
    private final BigDecimal dayPrice;
    private final Long imageId;

    public RoomCreateRequest(Long userId, String title, String country, String city, String description,
                             String roomType, String bedType, Boolean privateBathroom, Boolean privateKitchen,
                             List<String> amenities, List<DateRange> dateRanges,
                             BigDecimal dayPrice, Long imageId) {
        this.userId = userId;
        this.title = title;
        this.country = country;
        this.city = city;
        this.description = description;
        this.roomType = roomType;
        this.bedType = bedType;
        this.privateBathroom = privateBathroom;
        this.privateKitchen = privateKitchen;
        this.amenities = amenities;
        this.dateRanges = dateRanges;
        this.dayPrice = dayPrice;
        this.imageId = imageId;
    }

    public RoomCreateRequest(String title, String country, String city, String description,
                             String roomType, String bedType, Boolean privateBathroom, Boolean privateKitchen,
                             List<String> amenities, List<DateRange> dateRanges,
                             BigDecimal dayPrice, Long imageId) {
        this(null, title, country, city, description, roomType, bedType, privateBathroom, privateKitchen,
                amenities, dateRanges, dayPrice, imageId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getCountry() {
        return country;
    }

    public String getCity() {
        return city;
    }

    public String getDescription() {
        return description;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getBedType() {
        return bedType;
    }

    public Boolean getPrivateBathroom() {
        return privateBathroom;
    }

    public Boolean getPrivateKitchen() {
        return privateKitchen;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public List<DateRange> getDateRanges() {
        return dateRanges;
    }

    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public Long getImageId() {
        return imageId;
    }
}
