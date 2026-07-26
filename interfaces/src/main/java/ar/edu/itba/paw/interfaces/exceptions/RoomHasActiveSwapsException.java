package ar.edu.itba.paw.interfaces.exceptions;

public class RoomHasActiveSwapsException extends SwapException {
    public RoomHasActiveSwapsException(final long roomId) {
        super("Room has active swaps", "Room has active swaps", roomId);
    }
}
