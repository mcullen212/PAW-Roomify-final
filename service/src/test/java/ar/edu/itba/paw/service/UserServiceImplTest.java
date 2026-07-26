package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.interfaces.exceptions.InvalidPasswordException;
import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.persistence.UserDao;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.service.users.UserServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Optional;

import static org.junit.Assert.assertThrows;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    private static final long USER_ID = 1L;
    private static final String NAME = "name";
    private static final String RAW_PASSWORD = "Rawpassword1"; // Password passed to the service
    private static final String ENCODED_PASSWORD = "encodedpasswordhash"; // Password the DAO receives
    private static final String PASSWORD_WITHOUT_UPPERCASE = "password1";
    private static final String PASSWORD_WITHOUT_NUMBER = "Password";
    private static final String EMAIL = "email@example.com";
    private static final String OTHER_EMAIL = "other@example.com";
    private static final Locale LOCALE = Locale.ENGLISH;

    @Mock
    private UserDao mockDao;

    @Mock
    private PasswordEncoder mockPasswordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void testCreate_SuccessfulCreation() {
        // 1. Setup Mock Behavior (WHEN)
        Mockito.when(mockDao.findUserByEmail(EMAIL)).thenReturn(Optional.empty());

        Mockito.when(mockPasswordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        final User expectedUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);

        Mockito.when(mockDao.create(NAME, EMAIL, ENCODED_PASSWORD, LOCALE))
                .thenReturn(expectedUser);

        // 2. Exercise (CALL)
        User resultUser = userService.create(NAME, EMAIL, RAW_PASSWORD, LOCALE);

        // 3. Assertions (THEN)

        Assert.assertNotNull(resultUser);

        Assert.assertEquals(USER_ID, resultUser.getId());
        Assert.assertEquals(NAME, resultUser.getName());
        Assert.assertEquals(EMAIL, resultUser.getEmail());

        Assert.assertEquals(ENCODED_PASSWORD, resultUser.getPassword());
        Assert.assertEquals(LOCALE, resultUser.getLocale());
    }

    @Test
    public void testCreate_EmailAlreadyExists_ThrowsException() {
        // 1. Setup Mock Behavior (WHEN)

        final User existingUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);
        Mockito.when(mockDao.findUserByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        // 2. Exercise (CALL)
        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.create(NAME, EMAIL, RAW_PASSWORD, LOCALE)
        );
    }

    @Test
    public void testCreate_PasswordWithoutUppercase_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.create(NAME, EMAIL, PASSWORD_WITHOUT_UPPERCASE, LOCALE)
        );

        Mockito.verify(mockDao, Mockito.never()).create(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(Locale.class));
        Mockito.verify(mockPasswordEncoder, Mockito.never()).encode(Mockito.anyString());
    }

    @Test
    public void testFindUserById_UserExists() {
        // 1. Setup Mock Behavior
        final User expectedUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);
        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(expectedUser));

        // 2. Exercise
        Optional<User> result = userService.findUserById(USER_ID);

        // 3. Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(USER_ID, result.get().getId());
    }

    @Test
    public void testFindUserById_UserDoesNotExist() {
        // 1. Setup Mock Behavior
        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.empty());

        // 2. Exercise
        Optional<User> result = userService.findUserById(USER_ID);

        // 3. Assertions
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testFindUserByEmail_UserExists() {
        // 1. Setup Mock Behavior
        final User expectedUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);
        Mockito.when(mockDao.findUserByEmail(EMAIL)).thenReturn(Optional.of(expectedUser));

        // 2. Exercise
        Optional<User> result = userService.findUserByEmail(EMAIL);

        // 3. Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(EMAIL, result.get().getEmail());
    }

    @Test
    public void testFindUserByEmail_UserDoesNotExist() {
        // 1. Setup Mock Behavior
        Mockito.when(mockDao.findUserByEmail(EMAIL)).thenReturn(Optional.empty());

        // 2. Exercise
        Optional<User> result = userService.findUserByEmail(EMAIL);

        // 3. Assertions
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testResetPassword_Successful() {
        // 1. Setup Mock Behavior
        final String NEW_ENCODED_PASSWORD = "new_encoded_hash";
        final User userToUpdate = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);

        Mockito.when(mockPasswordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        Mockito.when(mockDao.changePassword(USER_ID, NEW_ENCODED_PASSWORD)).thenReturn(true);

        // 2. Exercise
        boolean result = userService.resetPassword(userToUpdate.getId(), NEW_RAW_PASSWORD);

        // 3. Assertions
        Assert.assertTrue(result);
    }

    private static final String NEW_RAW_PASSWORD = "Newrawpassword1";
    private static final String NEW_ENCODED_PASSWORD = "new_encoded_hash";

    @Test
    public void testChangePassword_Successful() {
        // 1. Setup Mock Behavior
        final User userToUpdate = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);

        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(userToUpdate));
        Mockito.when(mockPasswordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        Mockito.when(mockPasswordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);

        Mockito.when(mockDao.changePassword(USER_ID, NEW_ENCODED_PASSWORD)).thenReturn(true);

        // 2. Exercise
        userService.changePassword(USER_ID, RAW_PASSWORD, NEW_RAW_PASSWORD);

        // 3. Assertions
        Mockito.verify(mockDao).changePassword(USER_ID, NEW_ENCODED_PASSWORD);
    }

    @Test
    public void testChangePassword_OldPasswordMismatch() {
        // 1. Setup Mock Behavior
        final User userToUpdate = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);
        final String WRONG_OLD_PASSWORD = "wrong_password";

        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(userToUpdate));
        Mockito.when(mockPasswordEncoder.matches(WRONG_OLD_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

        // 2. Exercise
        assertThrows(
                InvalidPasswordException.class,
                () -> userService.changePassword(USER_ID, WRONG_OLD_PASSWORD, NEW_RAW_PASSWORD)
        );
    }

    @Test
    public void testChangePassword_NewPasswordWithoutNumber_ThrowsException() {
        final User userToUpdate = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);

        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(userToUpdate));

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.changePassword(USER_ID, RAW_PASSWORD, PASSWORD_WITHOUT_NUMBER)
        );

        Mockito.verify(mockPasswordEncoder, Mockito.never()).matches(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDao, Mockito.never()).changePassword(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testResetPassword_PasswordWithoutUppercase_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.resetPassword(USER_ID, PASSWORD_WITHOUT_UPPERCASE)
        );

        Mockito.verify(mockPasswordEncoder, Mockito.never()).encode(Mockito.anyString());
        Mockito.verify(mockDao, Mockito.never()).changePassword(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testVerifiedEmail_Successful() {
        // 1. Setup Mock Behavior
        Mockito.when(mockDao.verifiedEmail(USER_ID)).thenReturn(true);

        // 2. Exercise
        boolean result = userService.verifiedEmail(USER_ID);

        // 3. Assertions
        Assert.assertTrue(result);
    }

    private static final Locale NEW_LOCALE = Locale.forLanguageTag("es");

    @Test
    public void testUpdateLocale_Successful() {
        // 1. Setup Mock Behavior
        final User existingUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);

        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
        Mockito.when(mockDao.updateLocale(USER_ID, NEW_LOCALE)).thenReturn(true);

        // 2. Exercise
        boolean result = userService.updateLocale(USER_ID, NEW_LOCALE);

        // 3. Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void testUpdateLocale_UserDoesNotExist() {
        // 1. Setup Mock Behavior
        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.empty());

        // 2. Exercise
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateLocale(USER_ID, NEW_LOCALE)
        );
    }

    private static final String BIO = "Test Bio";
    private static final String TRAVEL_PREFS = "Mountains";

    @Test
    public void testUpdateProfile_Successful() {
        // 1. Setup Mock Behavior
        final User existingUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);

        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
        Mockito.when(mockDao.updateProfile(USER_ID, BIO, TRAVEL_PREFS)).thenReturn(true);

        // 2. Exercise
        boolean result = userService.updateProfile(USER_ID, BIO, TRAVEL_PREFS);

        // 3. Assertions
        Assert.assertTrue(result);
    }

    @Test
    public void testUpdateProfile_UserDoesNotExist() {
        // 1. Setup Mock Behavior
        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.empty());

        // 2. Exercise
        assertThrows(
                IllegalArgumentException.class,
                () ->userService.updateProfile(USER_ID, BIO, TRAVEL_PREFS)
        );
    }



    @Test
    public void testUpdateUser_BioOnly() {
        final User existingUser = mockExistingAuthorizedUser();
        Mockito.when(mockDao.updateProfile(USER_ID, BIO, null)).thenReturn(true);

        User result = userService.updateUser(USER_ID, BIO, null, null, null, null);

        Assert.assertEquals(existingUser.getId(), result.getId());
        Mockito.verify(mockDao).updateProfile(USER_ID, BIO, null);
        Mockito.verify(mockDao, Mockito.never()).updateLocale(Mockito.anyLong(), Mockito.any(Locale.class));
        Mockito.verify(mockDao, Mockito.never()).changePassword(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testUpdateUser_TravelPreferencesOnly() {
        mockExistingAuthorizedUser();
        Mockito.when(mockDao.updateProfile(USER_ID, null, TRAVEL_PREFS)).thenReturn(true);

        userService.updateUser(USER_ID, null, TRAVEL_PREFS, null, null, null);

        Mockito.verify(mockDao).updateProfile(USER_ID, null, TRAVEL_PREFS);
    }

    @Test
    public void testUpdateUser_LocaleOnly() {
        mockExistingAuthorizedUser();
        Mockito.when(mockDao.updateLocale(USER_ID, NEW_LOCALE)).thenReturn(true);

        userService.updateUser(USER_ID, null, null, "es", null, null);

        Mockito.verify(mockDao).updateLocale(USER_ID, NEW_LOCALE);
    }

    @Test
    public void testUpdateUser_PasswordOnly() {
        mockExistingAuthorizedUser();
        Mockito.when(mockPasswordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        Mockito.when(mockPasswordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        Mockito.when(mockDao.changePassword(USER_ID, NEW_ENCODED_PASSWORD)).thenReturn(true);

        userService.updateUser(USER_ID, null, null, null, RAW_PASSWORD, NEW_RAW_PASSWORD);

        Mockito.verify(mockDao).changePassword(USER_ID, NEW_ENCODED_PASSWORD);
    }

    @Test
    public void testUpdateUser_ProfileAndPassword() {
        mockExistingAuthorizedUser();
        Mockito.when(mockDao.updateProfile(USER_ID, BIO, TRAVEL_PREFS)).thenReturn(true);
        Mockito.when(mockDao.updateLocale(USER_ID, NEW_LOCALE)).thenReturn(true);
        Mockito.when(mockPasswordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        Mockito.when(mockPasswordEncoder.encode(NEW_RAW_PASSWORD)).thenReturn(NEW_ENCODED_PASSWORD);
        Mockito.when(mockDao.changePassword(USER_ID, NEW_ENCODED_PASSWORD)).thenReturn(true);

        userService.updateUser(USER_ID, BIO, TRAVEL_PREFS, "es", RAW_PASSWORD, NEW_RAW_PASSWORD);

        Mockito.verify(mockDao).updateProfile(USER_ID, BIO, TRAVEL_PREFS);
        Mockito.verify(mockDao).updateLocale(USER_ID, NEW_LOCALE);
        Mockito.verify(mockDao).changePassword(USER_ID, NEW_ENCODED_PASSWORD);
    }

    @Test
    public void testUpdateUser_EmptyBody() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, null, null, null, null)
        );
    }

    @Test
    public void testUpdateUser_OldPasswordWithoutNewPassword() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, null, null, RAW_PASSWORD, null)
        );
    }

    @Test
    public void testUpdateUser_NewPasswordWithoutOldPassword() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, null, null, null, NEW_RAW_PASSWORD)
        );
    }

    @Test
    public void testUpdateUser_OldPasswordMismatch() {
        mockExistingAuthorizedUser();
        final String wrongOldPassword = "wrong_password";
        Mockito.when(mockPasswordEncoder.matches(wrongOldPassword, ENCODED_PASSWORD)).thenReturn(false);

        assertThrows(
                InvalidPasswordException.class,
                () -> userService.updateUser(USER_ID, null, null, null, wrongOldPassword, NEW_RAW_PASSWORD)
        );
    }

    @Test
    public void testUpdateUser_NewPasswordTooShort() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, null, null, RAW_PASSWORD, "short")
        );

        Mockito.verify(mockPasswordEncoder, Mockito.never()).matches(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDao, Mockito.never()).changePassword(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testUpdateUser_NewPasswordWithoutUppercase_ThrowsException() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, null, null, RAW_PASSWORD, PASSWORD_WITHOUT_UPPERCASE)
        );

        Mockito.verify(mockPasswordEncoder, Mockito.never()).matches(Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mockDao, Mockito.never()).changePassword(Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    public void testUpdateUser_InvalidLocale() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, null, "fr", null, null)
        );
    }

    @Test
    public void testUpdateUser_BioTooLong() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, "a".repeat(501), null, null, null, null)
        );
    }

    @Test
    public void testUpdateUser_TravelPreferencesTooLong() {
        mockExistingAuthorizedUser();

        assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(USER_ID, null, "a".repeat(101), null, null, null)
        );
    }


    @Test
    public void testUpdateUser_UserDoesNotExist() {
        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(USER_ID, BIO, null, null, null, null)
        );
    }

    private User mockExistingAuthorizedUser() {
        final User existingUser = new User(USER_ID, EMAIL, NAME, ENCODED_PASSWORD, false, LOCALE.toString(), null, null);
        Mockito.when(mockDao.findUserById(USER_ID)).thenReturn(Optional.of(existingUser));
        return existingUser;
    }
}
