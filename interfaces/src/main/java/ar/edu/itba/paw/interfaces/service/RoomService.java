package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.DTO.ImageDTO;
import ar.edu.itba.paw.model.DTO.RoomCardResult;
import ar.edu.itba.paw.model.DTO.RoomCreateRequest;
import ar.edu.itba.paw.model.DTO.RoomCreationResult;
import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripContact;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomService {
    List<String> validateRoom(long ownerId, RoomType roomType, BedType bedType,
                              boolean privateBathroom, boolean privateKitchen);

    Room createRoomAndAvailability(String email, String title, String country, String city, String description,
                                   RoomType roomType, BedType bedType, boolean privateBathroom, boolean privateKitchen,
                                   List<Amenity> amenities, ImageDTO[] roomImage, List<DateRange> dateRanges, BigDecimal dayPrice);

    Room createRoomAndAvailability(String email, String title, String country, String city, String description,
                                   String roomType, String bedType, Boolean privateBathroom, Boolean privateKitchen,
                                   List<String> amenities, ImageDTO[] roomImage, List<DateRange> dateRanges, BigDecimal dayPrice);

    RoomCreationResult createRoomWithAvailability(String email, RoomCreateRequest request);
    RoomCreationResult createRoomWithAvailability(long ownerId, RoomCreateRequest request);
    RoomCreationResult createRoomWithAvailability(RoomCreateRequest request);

    void deleteRoom(long roomId);
    Room updateRoom(long roomId, String title, String description, List<String> amenities, BigDecimal dayPrice);

    Optional<Room> findRoomById(long id);
    long getOwnerId(long id);
    boolean isOwner(String email, long id);

    int getTotalPages(int pageSize);
    List<Room> getRoomsPaginated(int page, int pageSize);
    RoomSearchCriteria buildSearchCriteria(String destination, String checkIn, String checkOut,
                                           String roomType, String bedType, Boolean privateBathroom,
                                           Boolean privateKitchen, Long ownerId, List<String> amenities);
    List<Room> searchRooms(RoomSearchCriteria criteria, int page, int pageSize);
    List<RoomCardResult> searchRoomCards(RoomSearchCriteria criteria, int page, int pageSize);
    int countSearchRooms(RoomSearchCriteria criteria);
    //boolean updateRoomDetails(long roomId, String email, String title, String description, List<Amenity> amenities);
    boolean checkBothDates(LocalDate checkIn, LocalDate checkOut);
    List<Room> findRoomsByOwnerEmailPaging(String email, int page, int pageSize);
    int countRoomsByOwnerEmail(String email);
    List<Room> findRoomsByOwnerId(long id, int page, int pageSize);
    int countRoomsByOwnerId(long ownerId);
    List<Room> filterSuggestedRoomsForTrip(List<Room> suggestedRooms, List<TripContact> currentTripContacts, Trip trip, User loggedUser);
    int countRoomsByOwnerEmailSwap(String email);
    List<Room> findRoomsByOwnerEmailPagingSwap(String email, int page, int pageSize);
    }
