package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.RoomType;

import java.time.LocalDate;
import java.util.List;

public class RoomSearchCriteria {
    private String destination;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private RoomType roomType;
    private BedType bedType;
    private Boolean privateBathroom;
    private Boolean privateKitchen;
    private Long ownerId;
    private Long tripId;
    private List<Amenity> amenities;

    public RoomSearchCriteria() {}

    public String getDestination() {
        return destination;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public BedType getBedType() {
        return bedType;
    }

    public Boolean getPrivateBathroom() {
        return privateBathroom;
    }

    public Boolean getPrivateKitchen() {
        return privateKitchen;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getTripId() {return tripId;} //CAN BE NULL

    public List<Amenity> getAmenities() {
        return amenities;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public void setBedType(BedType bedType) {
        this.bedType = bedType;
    }

    public void setPrivateBathroom(Boolean privateBathroom) {
        this.privateBathroom = privateBathroom;
    }

    public void setPrivateKitchen(Boolean privateKitchen) {
        this.privateKitchen = privateKitchen;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setTripId(Long tripId) {this.tripId = tripId;}

    public void setAmenities(List<Amenity> amenities) {
        this.amenities = amenities;
    }
}
