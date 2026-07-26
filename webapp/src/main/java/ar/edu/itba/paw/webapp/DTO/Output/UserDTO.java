package ar.edu.itba.paw.webapp.DTO.Output;

import ar.edu.itba.paw.model.User;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
public class UserDTO {

    private long id;
    private String name;
    private String email;
    private String bio;
    private String travelPreferences;
    private String locale;

    public UserDTO() {
        // JAX-RS
    }

    public UserDTO(final User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.bio = user.getBio();
        this.travelPreferences = user.getTravelPreferences();
        this.locale = user.getLocale().toLanguageTag();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    @Override
    public int hashCode() {
        return Objects.hash(id, name, email, bio, travelPreferences, locale);
    }
}
