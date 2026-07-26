package ar.edu.itba.paw.webapp.DTO;

import ar.edu.itba.paw.webapp.validation.ValidPassword;

import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UserUpdateDto {

    @Size(max = 500)
    private String bio;

    @Size(max = 100)
    private String travelPreferences;

    private String locale;

    private String oldPassword;

    @Size(min = 8)
    @ValidPassword
    private String newPassword;

    public UserUpdateDto() {
        // JAX-RS
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getTravelPreferences() {
        return travelPreferences;
    }

    public void setTravelPreferences(String travelPreferences) {
        this.travelPreferences = travelPreferences;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
