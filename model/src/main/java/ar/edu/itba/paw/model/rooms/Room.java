package ar.edu.itba.paw.model.rooms;

import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.model.User;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Entity
@Table(name= "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "room_id_seq")
    @SequenceGenerator(name = "room_id_seq", sequenceName = "room_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "description")
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(name= "room_type")
    private RoomType roomType;

    // Mantenemos EnumType.STRING para BedType si es un enum simple sin Converter personalizado.
    @Enumerated(EnumType.STRING)
    @Column(name= "bed_type")
    private BedType bedType;

    @Column(name = "private_bathroom", nullable = false)
    private boolean privateBathroom;

    @Column(name = "private_kitchen", nullable = false)
    private boolean privateKitchen;

    @Column(name = "amenities")
    private String amenities;

    @Column(name = "city", length = 100, nullable = false)
    private String city;

    @Column(name = "country", length = 100, nullable = false)
    private String country;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;   // host

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    //NEW UPDATE --> PUT A price for day
    @Column(name = "day_price", precision = 10, scale = 2, nullable = true)
    private BigDecimal dayPrice;

    /* Default */ Room() {
        // Just for Hibernate
    }

    // Constructor con RoomType como Enum
    public Room(String title, String country, String city, String description,
                RoomType roomType, BedType bedType, boolean privateBathroom, boolean privateKitchen, String amenities, User owner, Image image,BigDecimal dayPrice) {
        this.title = title;
        this.country = country;
        this.city = city;
        this.description = description;
        this.roomType = roomType;
        this.bedType = bedType;
        this.privateBathroom = privateBathroom;
        this.privateKitchen = privateKitchen;
        this.amenities = amenities;
        this.owner = owner;
        this.image = image;
        this.dayPrice = dayPrice;
    }

    // Constructor con RoomType como Enum
    public Room(long id, String title, String country, String city, String description,
                RoomType roomType, BedType bedType, boolean privateBathroom, boolean privateKitchen, String amenities, User owner, Image image, BigDecimal dayPrice) {
        this.id = id;
        this.title = title;
        this.country = country;
        this.city = city;
        this.description = description;
        this.roomType = roomType;
        this.bedType = bedType;
        this.privateBathroom = privateBathroom;
        this.privateKitchen = privateKitchen;
        this.amenities = amenities;
        this.owner= owner;
        this.image = image;
        this.dayPrice = dayPrice;
    }

    public Long getId() {
        return id;
    }
    @Transient
    public Long getImageId() {
        return image != null ? image.getId() : null;
    }
    public User getOwner() {
        return owner;
    }
    public BigDecimal getDayPrice(){
        if(dayPrice == null){
            return BigDecimal.valueOf(1000);
        }
        else
            return dayPrice;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // MANTENIDO: Devuelve el objeto RoomType (el enum)
    public RoomType getRoomType() {
        return roomType;
    }

    // MANTENIDO: Devuelve el objeto BedType (el enum)
    public BedType getBedType() {
        return bedType;
    }


    public boolean isPrivateBathroom() {
        return privateBathroom;
    }

    public boolean isPrivateKitchen() {
        return privateKitchen;
    }

    public List<String> getAmenities() {
        if (amenities == null || amenities.isEmpty()) return List.of();
        String cleaned = amenities.replace("[", "").replace("]", "").replace("\"", "");
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public void setDayPrice(BigDecimal dayPrice) {
        this.dayPrice = dayPrice;
    }
    public List<Amenity> getAmenitiesEnums() {
        if (amenities == null || amenities.isEmpty()) return List.of();

        String cleaned = amenities.replace("[", "").replace("]", "").replace("\"", "");

        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .map(Amenity::fromDisplayName) // Asumiendo que Amenity tiene este método
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                title,
                country,
                city,
                description,
                roomType,
                bedType,
                privateBathroom,
                privateKitchen,
                amenities,
                owner != null ? owner.getId() : null,
                image != null ? image.getId() : null,
                getDayPrice()
        );
    }
}
