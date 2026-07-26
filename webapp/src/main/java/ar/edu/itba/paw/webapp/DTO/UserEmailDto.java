package ar.edu.itba.paw.webapp.DTO;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UserEmailDto {

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    public UserEmailDto() {
    }

    public UserEmailDto(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}