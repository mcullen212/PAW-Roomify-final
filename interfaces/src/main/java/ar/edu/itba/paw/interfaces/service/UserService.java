package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.User;

import java.util.Locale;
import java.util.Optional;

public interface UserService {
    User create(String name, String email, String password, Locale locale);
    Optional <User> findUserById(long id);
    Optional <User> findUserByEmail(String email);
    boolean resetPassword(long userId, String password);
    void changePassword(long userId, String oldPassword, String newPassword);
    boolean verifiedEmail(long userId);
    boolean updateLocale(long userId,Locale locale);
    boolean updateProfile(long userId, String bio, String travelPrefs);
    User updateUser(
            long userId,
            String bio,
            String travelPrefs,
            String locale,
            String oldPassword,
            String newPassword
    );
    User getPrivateProfile(long userId);
}
