package ar.edu.itba.paw.model.rooms;

public enum DeleteRoomStatus {
    SUCCESS,
    NOT_OWNER,
    HAS_ACTIVE_SWAPS,
    ROOM_NOT_FOUND,
    PERSISTENCE_ERROR
}