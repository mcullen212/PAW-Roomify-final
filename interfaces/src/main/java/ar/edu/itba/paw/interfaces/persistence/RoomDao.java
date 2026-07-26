package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.rooms.BedType;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomDao {

    Room create(long ownerId, String title, String country, String city, String description,
                RoomType roomType, BedType bedType, boolean privateBathroom, boolean privateKitchen,
                String attrsJson, long imageId, BigDecimal dayPrice);

    Optional<Room> findRoomById(long id);


    List<Room> getRoomsPaginated(int page, int pageSize);
    int countRooms();

    List<Room> findByRequesterIdRoom(long userId);

    List<Room> searchRooms(RoomSearchCriteria criteria, int page, int pageSize);
    int countSearchRooms(RoomSearchCriteria criteria);

    boolean updateRoom(long roomId, String title, String description, String amenities, BigDecimal dayPrice);
    List<Room> findRoomsByOwnerIdPaging(long ownerId, int page, int pageSize);
    int countRoomsByOwnerId(long ownerId);
    boolean deleteRoom(long roomId);
    List<Room> findRoomsByOwnerIdPagingForSwap(long ownerId, int page, int pageSize);
    int countRoomsByOwnerIdForSwap(long ownerId);
    boolean existsByImageId(long imageId);
    }
