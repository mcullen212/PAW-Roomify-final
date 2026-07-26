package ar.edu.itba.paw.webapp.DTO.Input;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class UpdateTripDTO {

    @NotNull
    @Positive
    private Long roomId;

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }
}
