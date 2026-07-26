package ar.edu.itba.paw.webapp.DTO.Input;

import ar.edu.itba.paw.webapp.validation.ValidPassword;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class UserResetPasswordDto {

    @NotBlank
    @Size(min = 8, max = 100)
    @ValidPassword
    private String newPassword;

    public UserResetPasswordDto() {
        // For Jersey
    }

    public UserResetPasswordDto(final String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
