package ar.edu.itba.paw.interfaces.persistence;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import ar.edu.itba.paw.model.User;

public interface UserDao {
    Optional<User> findUserById(long id);
    Optional<User> findUserByEmail(String email);
    User create(String name, String email, String hashPassword, Locale locale);
    boolean changePassword(long userId, String newPassword);
    boolean verifiedEmail(long userId);
    boolean updateLocale(long userId, Locale locale);
    boolean updateProfile(long userId, String bio, String travelPrefs);
}