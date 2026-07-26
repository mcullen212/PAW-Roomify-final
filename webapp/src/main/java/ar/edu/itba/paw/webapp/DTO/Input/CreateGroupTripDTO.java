package ar.edu.itba.paw.webapp.DTO.Input;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

public class CreateGroupTripDTO {

    @Positive
    private long ownerId;

    @NotBlank
    @Size(max = 255)
    private String title;

    public long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(long ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
