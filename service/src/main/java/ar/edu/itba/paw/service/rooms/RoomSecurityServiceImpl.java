package ar.edu.itba.paw.service.rooms;

import ar.edu.itba.paw.interfaces.persistence.RoomDao; // Re-introducimos el DAO de Room
import ar.edu.itba.paw.interfaces.service.ImageService;
import ar.edu.itba.paw.interfaces.service.RoomSecurityService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.interfaces.service.ContactService; // Mantenemos ContactService

import ar.edu.itba.paw.model.rooms.Amenity;
import ar.edu.itba.paw.model.rooms.DeleteRoomStatus;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.swaps.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoomSecurityServiceImpl implements RoomSecurityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomSecurityServiceImpl.class);

    private final RoomDao roomDao;
    private final UserService userService;
    private final ContactService contactService;
    private final ImageService imageService;

    public RoomSecurityServiceImpl(RoomDao roomDao, UserService userService,
                                   ContactService contactService, ImageService imageService) {
        this.roomDao = roomDao;
        this.userService = userService;
        this.contactService = contactService;
        this.imageService = imageService;
    }

    @Override
    @Transactional
    public DeleteRoomStatus deleteRoom(long roomId, String email) {
        Optional<Room> roomOpt = roomDao.findRoomById(roomId);

        if (roomOpt.isEmpty()) {
            return DeleteRoomStatus.ROOM_NOT_FOUND;
        }
        Room room = roomOpt.get();

        Optional<User> userOpt = userService.findUserByEmail(email);

        if (userOpt.isEmpty() || room.getOwner().getId() != userOpt.get().getId()) {
            return DeleteRoomStatus.NOT_OWNER;
        }

        if (contactService.hasActiveContacts(roomId)) {
            return DeleteRoomStatus.HAS_ACTIVE_SWAPS;
        }

        boolean deleted = roomDao.deleteRoom(roomId);

        if (deleted) {
            imageService.deleteImage(room.getImageId());
            return DeleteRoomStatus.SUCCESS;
        } else {
            return DeleteRoomStatus.PERSISTENCE_ERROR;
        }
    }

    @Override
    @Transactional
    public boolean updateRoomDetailsWithNotification(long roomId, String email, String title, String description, List<Amenity> amenities, BigDecimal dayPrice) {
        Room oldRoom = roomDao.findRoomById(roomId)
                .orElseThrow(()-> new IllegalArgumentException("No room found"));

        User user = userService.findUserByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException("No user found"));

        if (oldRoom.getOwner().getId() != user.getId()) {
            return false;
        }

        String oldTitle = oldRoom.getTitle();
        String oldDescription = oldRoom.getDescription();
        List<Amenity> oldAmenities = new ArrayList<>(oldRoom.getAmenitiesEnums());

        String attrsJson = amenitiesJson(amenities);
        boolean updated = roomDao.updateRoom(roomId, title, description, attrsJson,dayPrice);

        if (updated) {
            List<Contact> contacts = contactService.findContactsForRoom(roomId);
            contactService.notifyRoomUpdate(roomId, oldTitle, oldDescription, oldAmenities, title, description, amenities, contacts);
            return true;
        } else {
            return false;
        }
    }

    private String amenitiesJson(List<Amenity> amenities) {
        return amenities != null
                ? amenities.stream()
                .map(Amenity::toString)
                .map(name -> "\"" + name + "\"")
                .collect(Collectors.joining(", ", "[", "]"))
                : "[]";
    }
}