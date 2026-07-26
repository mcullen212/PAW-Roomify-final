package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.DeleteRoomStatus;

import java.math.BigDecimal;
import java.util.List;

public interface RoomSecurityService {
    DeleteRoomStatus deleteRoom(long roomId, String email);
    boolean updateRoomDetailsWithNotification(long roomId, String email, String title, String description, List<Amenity> amenities, BigDecimal dayPrice);
}