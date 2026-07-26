package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.model.trip.TripStatus;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class UpdateGroupTripDTO {

    @Size(max = 255)
    private String title;

    @NotNull
    private TripStatus status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TripStatus getStatus() {
        return status;
    }

    public void setStatus(TripStatus status) {
        this.status = status;
    }
}
