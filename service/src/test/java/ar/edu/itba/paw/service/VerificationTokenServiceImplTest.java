package ar.edu.itba.paw.service;

import ar.edu.itba.paw.interfaces.persistence.VerificationTokenDao;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;
import ar.edu.itba.paw.service.users.VerificationTokenServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VerificationTokenServiceImplTest {

    private static final long USER_ID = 1L;
    private static final String USER_EMAIL = "test@user.com";
    private static final String EXISTING_TOKEN_VALUE = "ABC12345";
    private static final String NEW_TOKEN_VALUE = "XYZ987";
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private static final TokenType TOKEN_TYPE = TokenType.VERIFY_EMAIL;

    @Mock
    private VerificationTokenDao mockTokenDao;
    @Mock
    private UserService mockUserService;

    @InjectMocks
    private VerificationTokenServiceImpl tokenService;

    private final User testUser = new User(USER_ID, USER_EMAIL, "Test", "pass", false, Locale.ENGLISH.toString(), null, null);

    @Test
    public void testFindByToken_Found() {
        // 1. Setup Mocks
        VerificationToken token = new VerificationToken(testUser, NEW_TOKEN_VALUE, TOKEN_TYPE, Instant.now().plus(Duration.ofDays(1)));
        when(mockTokenDao.findByToken(NEW_TOKEN_VALUE)).thenReturn(Optional.of(token));

        // 2. Exercise
        Optional<VerificationToken> result = tokenService.findByToken(NEW_TOKEN_VALUE);

        // 3. Assertions
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(NEW_TOKEN_VALUE, result.get().getToken());
    }

    @Test
    public void testFindByToken_NotFound() {
        // 1. Setup Mocks
        when(mockTokenDao.findByToken(NEW_TOKEN_VALUE)).thenReturn(Optional.empty());

        // 2. Exercise
        Optional<VerificationToken> result = tokenService.findByToken(NEW_TOKEN_VALUE);

        // 3. Assertions
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testValidateToken_Valid() {
        // 1. Setup Mocks (Token is NOT expired)
        VerificationToken validToken = new VerificationToken(testUser, NEW_TOKEN_VALUE, TOKEN_TYPE, Instant.now().plus(Duration.ofDays(1)));
        when(mockTokenDao.findByToken(NEW_TOKEN_VALUE)).thenReturn(Optional.of(validToken));

        // 2. Exercise
        Optional<VerificationToken> result = tokenService.validateToken(NEW_TOKEN_VALUE);

        // 3. Assertions
        Assert.assertTrue(result.isPresent());
    }

    @Test
    public void testValidateToken_Expired() {
        // 1. Setup Mocks (Token IS expired)
   VerificationToken expiredToken = new VerificationToken(testUser, NEW_TOKEN_VALUE, TOKEN_TYPE, Instant.now().minus(Duration.ofDays(1)));
        when(mockTokenDao.findByToken(NEW_TOKEN_VALUE)).thenReturn(Optional.of(expiredToken));

        // 2. Exercise
        Optional<VerificationToken> result = tokenService.validateToken(NEW_TOKEN_VALUE);

        // 3. Assertions
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testValidateToken_NotFound() {
        // 1. Setup Mocks
        when(mockTokenDao.findByToken(NEW_TOKEN_VALUE)).thenReturn(Optional.empty());

        // 2. Exercise
        Optional<VerificationToken> result = tokenService.validateToken(NEW_TOKEN_VALUE);

        // 3. Assertions
        Assert.assertFalse(result.isPresent());
    }

    @Test
    public void testInvalidateToken_SuccessfulExecution() {
        // 1. Setup Mocks
        when(mockTokenDao.delete(EXISTING_TOKEN_VALUE)).thenReturn(true);

        // 2. Exercise
        tokenService.invalidateToken(EXISTING_TOKEN_VALUE);

        // 3. Assertions (Only asserts that no exception was thrown)
        Assert.assertTrue(true);
    }

    @Test
    public void testDeleteToken_SuccessfulExecution() {
        // 1. Setup Mocks
        when(mockTokenDao.delete(EXISTING_TOKEN_VALUE)).thenReturn(true);

        // 2. Exercise
        tokenService.deleteToken(EXISTING_TOKEN_VALUE);

        // 3. Assertions (Only asserts that no exception was thrown)
        Assert.assertTrue(true);
    }

    @Test
    public void testDeleteExpiredTokens_SuccessfulExecution() {
        // 1. Setup Mocks
        when(mockTokenDao.deleteExpiredTokens()).thenReturn(true);

        // 2. Exercise
        tokenService.deleteExpiredTokens();

        // 3. Assertions (Only asserts that no exception was thrown)
        Assert.assertTrue(true);
    }
}