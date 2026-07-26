package ar.edu.itba.paw.interfaces.exceptions;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(long roomId) {
        super("Room not found with id " + roomId);
    }

    @Override
    public String getLocalizedMessage() {
        return "error.room.notFound";
    }
}
