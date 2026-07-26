package ar.edu.itba.paw.model.DTO;

import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.model.rooms.RoomAvailability;

import java.util.List;

public class RoomCreationResult {

    private final Room room;
    private final List<RoomAvailability> availabilities;

    public RoomCreationResult(Room room, List<RoomAvailability> availabilities) {
        this.room = room;
        this.availabilities = availabilities;
    }

    public Room getRoom() {
        return room;
    }

    public List<RoomAvailability> getAvailabilities() {
        return availabilities;
    }
}
