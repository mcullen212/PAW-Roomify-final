package ar.edu.itba.paw.service.users;

import ar.edu.itba.paw.interfaces.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.interfaces.exceptions.InvalidPasswordException;
import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.UserDao;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.PasswordPolicy;
import ar.edu.itba.paw.model.User;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Primary
@Service
public class UserServiceImpl implements UserService {

    private static final int MAX_BIO_LENGTH = 500;
    private static final int MAX_TRAVEL_PREFS_LENGTH = 100;
    private static final Set<String> SUPPORTED_LOCALES = Set.of("en", "es");

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(
        final UserDao userDao,
        final PasswordEncoder passwordEncoder
    ) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public User create(
        String name,
        String email,
        String password,
        Locale locale
    ) {
        validatePasswordPolicy(password);

        if (userDao.findUserByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(
                "The email is already registered"
            );
        }
        return userDao.create(
            name,
            email,
            passwordEncoder.encode(password),
            locale
        );
    }

    @Override
    public Optional<User> findUserById(long id) {
        return userDao.findUserById(id);
    }

    @Override
    public Optional<User> findUserByEmail(String email) {
        return userDao.findUserByEmail(email);
    }

    @Transactional
    @Override
    public boolean resetPassword(long userId, String password) {
        validatePasswordPolicy(password);
        return userDao.changePassword(userId, passwordEncoder.encode(password));
    }

    @Transactional
    @Override
    public void changePassword(
        long userId,
        String oldPassword,
        String newPassword
    ) {
        User user = findUserById(userId).orElseThrow(() ->
            new UserNotFoundException("User not found")
        );

        validatePasswordPolicy(newPassword);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidPasswordException("Invalid current password");
        }

        userDao.changePassword(
            user.getId(),
            passwordEncoder.encode(newPassword)
        );
    }

    @Transactional
    @Override
    public boolean verifiedEmail(long userId) {
        return userDao.verifiedEmail(userId);
    }

    @Transactional
    @Override
    public boolean updateLocale(long userId, Locale locale) {
        long id = findUserById(userId)
            .orElseThrow(() ->
                new IllegalArgumentException("User does not Exist")
            )
            .getId();
        return userDao.updateLocale(id, locale);
    }

    @Transactional
    @Override
    public boolean updateProfile(long userId, String bio, String travelPrefs) {
        User user = findUserById(userId).orElseThrow(() ->
            new IllegalArgumentException("User does not Exist")
        );

        if (bio == null && travelPrefs == null) {
            return false;
        }

        return userDao.updateProfile(user.getId(), bio, travelPrefs);
    }

    @Transactional
    @Override
    public User updateUser(
            long userId,
            String bio,
            String travelPrefs,
            String locale,
            String oldPassword,
            String newPassword
    ) {
        User user = findUserById(userId).orElseThrow(() ->
                new UserNotFoundException("User not found")
        );

        validateUserUpdate(bio, travelPrefs, locale, oldPassword, newPassword);

        if (bio != null || travelPrefs != null) {
            userDao.updateProfile(user.getId(), bio, travelPrefs);
        }

        if (locale != null) {
            userDao.updateLocale(user.getId(), Locale.forLanguageTag(locale));
        }

        if (oldPassword != null && newPassword != null) {
            if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
                throw new InvalidPasswordException("Invalid current password");
            }

            userDao.changePassword(
                user.getId(),
                passwordEncoder.encode(newPassword)
            );
        }

        return user;
    }

    private void validateUserUpdate(
        String bio,
        String travelPrefs,
        String locale,
        String oldPassword,
        String newPassword
    ) {
        if (
            bio == null &&
            travelPrefs == null &&
            locale == null &&
            oldPassword == null &&
            newPassword == null
        ) {
            throw new IllegalArgumentException("At least one field must be provided");
        }

        if (bio != null && bio.length() > MAX_BIO_LENGTH) {
            throw new IllegalArgumentException("Bio must be at most 500 characters");
        }

        if (travelPrefs != null && travelPrefs.length() > MAX_TRAVEL_PREFS_LENGTH) {
            throw new IllegalArgumentException("Travel preferences must be at most 100 characters");
        }

        if (locale != null && !SUPPORTED_LOCALES.contains(locale)) {
            throw new IllegalArgumentException("Invalid locale");
        }

        if (oldPassword == null && newPassword != null) {
            throw new IllegalArgumentException("Old password is required");
        }

        if (oldPassword != null && newPassword == null) {
            throw new IllegalArgumentException("New password is required");
        }

        if (oldPassword != null && oldPassword.isEmpty()) {
            throw new IllegalArgumentException("Old password is required");
        }

        if (newPassword != null) {
            validatePasswordPolicy(newPassword);
        }
    }

    private void validatePasswordPolicy(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new IllegalArgumentException("Password must be at least 8 characters and contain a number and an uppercase letter");
        }
    }

    @Override
    public User getPrivateProfile(long userId) {
        return findUserById(userId).orElseThrow(() ->
            new UserNotFoundException("User not found")
        );
    }
}
