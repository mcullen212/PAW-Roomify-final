package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.webapp.validation.ValidEnumList;

import javax.validation.constraints.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigDecimal;
import java.util.List;

@XmlRootElement
public class RoomUpdateDTO {
    @NotBlank
    @Size(max = 50)
    private String title;

    @Size(max = 500)
    @Pattern(
            regexp = "^[\\p{L}\\p{N}\\s.,!?¿¡'\"-]*$")
    private String description;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal dayPrice;

    @ValidEnumList(enumClass = Amenity.class, message = "{validation.amenity.invalid}")
    private List<String> amenities;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }
    public BigDecimal getDayPrice() { return dayPrice; }
    public void setDayPrice(BigDecimal dayPrice) { this.dayPrice = dayPrice; }

}
