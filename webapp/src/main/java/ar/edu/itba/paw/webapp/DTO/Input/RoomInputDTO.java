package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.webapp.validation.*;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoOverlappingDateRanges
public class RoomInputDTO {
    @NotNull
    @Positive
    private Long userId;

    @NotBlank
    @Size(max = 50)
    private String title;

    @NotBlank(message = "{room.country.notBlank}")
    @ValidCountry
    private String country;

    @NotBlank(message = "{room.city.notBlank}")
    @Size(max = 50, message = "{room.city.size}")
    @ValidCity(message = "{room.city.invalid}")
    private String city;

    @Size(max = 500, message = "{room.description.size}")
    @Pattern(
            regexp = "^[\\p{L}\\p{N}\\s.,!?¿¡'\"-]*$",
            message = "{room.description.pattern}"
    )
    private String description;


    @NotNull(message = "{room.bedType.notNull}")
    @ValidEnum(enumClass = RoomType.class)
    private String roomType;

    @NotNull(message = "{room.bedType.notNull}")
    @ValidEnum(enumClass = BedType.class)
    private String bedType;

    //optional
    @NotNull
    private Boolean privateBathroom;
    @NotNull
    private Boolean privateKitchen;

    @ValidEnumList(enumClass = Amenity.class, message = "{validation.amenity.invalid}")
    private List<String> amenities; // ["WiFi", "Air Conditioning", "Heating", "Parking", "Pool", "Gym"]

    @Valid
    @NotNull
    @Size(min = 1)
    private List<RoomAvailabilityDTO> dateRange = new ArrayList<>();

    @NotNull(message = "{room.dayPrice.notNull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{room.dayPrice.min}")
    @Digits(integer = 8, fraction = 2, message = "{room.dayPrice.digits}")
    private BigDecimal dayPrice;

    @NotNull(message = "{room.image.notNull}")
    private Long imageId;


    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public Boolean getPrivateBathroom() { return privateBathroom; }
    public void setPrivateBathroom(Boolean privateBathroom) { this.privateBathroom = privateBathroom; }
    public Boolean getPrivateKitchen() { return privateKitchen; }
    public void setPrivateKitchen(Boolean privateKitchen) { this.privateKitchen = privateKitchen; }
    public void setAmenities(List<String> attributes) { this.amenities = attributes; }
    public void setDateRange(List<RoomAvailabilityDTO> dateRanges) { this.dateRange = dateRanges; }
    public List<RoomAvailabilityDTO> getDateRange() { return dateRange; }
    public List<String> getAmenities() {
        return amenities;
    }
    public BigDecimal getDayPrice() {
        return dayPrice;
    }

    public void setDayPrice(BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public RoomType getRoomTypeEnum() {
        if (roomType == null || roomType.isBlank()) return null;
        return RoomType.valueOf(roomType.toUpperCase());
    }

    public BedType getBedTypeEnum() {
        if (bedType == null || bedType.isBlank()) return null;
        return BedType.valueOf(bedType.toUpperCase());
    }

    public List<Amenity> getAmenitiesEnums() {
        if (amenities == null) return new ArrayList<>();
        return amenities.stream()
                .map(Amenity::valueOf)
                .collect(Collectors.toList());
    }

    public List<DateRange> getDateRanges() {
        if (dateRange == null) {
            return List.of();
        }

        return dateRange.stream()
                .map(dateRangeForm -> new DateRange(dateRangeForm.getStartDate(), dateRangeForm.getEndDate()))
                .collect(Collectors.toList());
    }
}
