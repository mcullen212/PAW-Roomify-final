package ar.edu.itba.paw.model;

import javax.persistence.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "AppUser")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appuser_id_seq")
    @SequenceGenerator(sequenceName = "appuser_id_seq", name = "appuser_id_seq", allocationSize = 1)
    @Column(name = "id")
    private long id;

    @Column(length = 255, nullable = false, unique = true)
    private String email;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(name = "is_verified", nullable = false)//columnDefinition= "boolean default false" (?)
    private boolean isVerified;

    @Column(length = 10)
    private String locale;
    @Column
    private String bio;

    @Column(name = "travel_prefs")
    private String travelPreferences;

    //@OneToMany(mappedBy = "reportedBy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    //private List<Issue> reportedIssues;

    //@OneToMany(mappedBy = "assignedTo", cascade = CascadeType.ALL, orphanRemoval = false)
    //private List<Issue> assignedIssues;

    /* Default*/ User() {
        //Just for Hibernate
    }


    public User(String email, String name, String password, boolean isVerified, String locale, String bio, String travelPreferences) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.isVerified = isVerified;
        this.locale = locale != null ? locale : Locale.getDefault().toString();

        this.bio = bio;
        this.travelPreferences = travelPreferences;
    }

    public User(Long id, String email, String name, String password, boolean isVerified, String locale, String bio, String travelPreferences) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.password = password;
        this.isVerified = isVerified;

        if (locale != null && !locale.isEmpty()) {
            this.locale = Locale.forLanguageTag(locale.replace('_', '-')).toString();
        } else {
            this.locale = Locale.getDefault().toString();
        }
        this.bio = bio;
        this.travelPreferences = travelPreferences;
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public Locale getLocale() {
        if(locale == null || locale.isEmpty()){
            return Locale.getDefault();
        }
        return Locale.forLanguageTag(locale.replace('_','-'));
    }

    public String getLocaleCode() {
        return locale == null ? "en" : locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale != null ? locale.toString() : Locale.getDefault().toString();
    }

    public String getBio() {
        return bio;
    }

    public void setPassword(String password){
        this.password = password;
    }
    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getTravelPreferences() {
        return travelPreferences;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVerified() {
        this.isVerified = true;
    }
    public void setTravelPreferences(String travelPreferences) {
        this.travelPreferences = travelPreferences;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, name, isVerified, locale, bio, travelPreferences);
    }
}
