package ar.edu.itba.paw.service.auth;

import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.service.EmailService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.interfaces.service.VerificationTokenService;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class AuthServiceImplTest {
    private static final long USER_ID = 1L;
    private static final String EMAIL = "test@user.com";
    private static final String MISSING_EMAIL = "missing@user.com";
    private static final String NAME = "Test User";
    private static final String PASSWORD = "encodedPassword";
    private static final String APP_URL = "https://roomify.example";
    private static final String OTP = "ABC123";
    private static final String VERIFY_LINK = APP_URL + "/verify-token?type=verify&email=test%40user.com";
    private static final String RESET_LINK = APP_URL + "/verify-token?type=reset&email=test%40user.com";

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenService tokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;

    @Before
    public void setUp() {
        user = new User(USER_ID, EMAIL, NAME, PASSWORD, false, Locale.ENGLISH.toString(), null, null);
        ReflectionTestUtils.setField(authService, "appUrl", APP_URL);
    }

    @Test
    public void testConsumeOtpValidToken() {
        final String[] invalidatedToken = {null};
        VerificationToken token = new VerificationToken(
                user,
                OTP,
                TokenType.VERIFY_EMAIL,
                Instant.now().plus(Duration.ofHours(1))
        );

        when(tokenService.validateToken(OTP)).thenReturn(Optional.of(token));
        doAnswer(invocation -> {
            invalidatedToken[0] = invocation.getArgument(0);
            return null;
        }).when(tokenService).invalidateToken(OTP);

        Optional<VerificationToken> result = authService.consumeOtp(EMAIL, OTP);

        assertTrue(result.isPresent());
        assertEquals(token, result.get());
        assertEquals(OTP, invalidatedToken[0]);
    }

    @Test
    public void testConsumeOtpInvalidToken() {
        when(tokenService.validateToken(OTP)).thenReturn(Optional.empty());

        Optional<VerificationToken> result = authService.consumeOtp(EMAIL, OTP);

        assertFalse(result.isPresent());
    }

    @Test
    public void testConsumeOtpDoesNotConsumeTokenBelongingToAnotherEmail() {
        VerificationToken token = new VerificationToken(
                user,
                OTP,
                TokenType.VERIFY_EMAIL,
                Instant.now().plus(Duration.ofHours(1))
        );
        when(tokenService.validateToken(OTP)).thenReturn(Optional.of(token));

        Optional<VerificationToken> result = authService.consumeOtp(MISSING_EMAIL, OTP);

        assertFalse(result.isPresent());
        verify(tokenService, never()).invalidateToken(anyString());
    }

    @Test
    public void testSendVerificationEmail() {
        final User[] sentUser = {null};
        final VerificationToken[] sentToken = {null};
        final String[] sentLink = {null};
        VerificationToken token = new VerificationToken(
                user,
                OTP,
                TokenType.VERIFY_EMAIL,
                Instant.now().plus(Duration.ofHours(24))
        );

        when(userService.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.createToken(user, TokenType.VERIFY_EMAIL, Duration.ofHours(24))).thenReturn(token);
        doAnswer(invocation -> {
            sentUser[0] = invocation.getArgument(0);
            sentToken[0] = invocation.getArgument(1);
            sentLink[0] = invocation.getArgument(2);
            return null;
        }).when(emailService).sendVerifyEmail(user, token, VERIFY_LINK);

        authService.sendVerificationEmail(EMAIL);

        assertEquals(user, sentUser[0]);
        assertEquals(token, sentToken[0]);
        assertEquals(VERIFY_LINK, sentLink[0]);
    }

    @Test
    public void testSendVerificationEmailNonExistingUser() {
        when(userService.findUserByEmail(MISSING_EMAIL)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> authService.sendVerificationEmail(MISSING_EMAIL));
    }

    @Test
    public void testRequestPasswordReset() {
        final User[] sentUser = {null};
        final String[] sentToken = {null};
        final String[] sentLink = {null};
        VerificationToken token = new VerificationToken(
                user,
                OTP,
                TokenType.RESET_PASSWORD,
                Instant.now().plus(Duration.ofHours(1))
        );

        when(userService.findUserByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(tokenService.createToken(user, TokenType.RESET_PASSWORD, Duration.ofHours(1))).thenReturn(token);
        doAnswer(invocation -> {
            sentUser[0] = invocation.getArgument(0);
            sentToken[0] = invocation.getArgument(1);
            sentLink[0] = invocation.getArgument(2);
            return null;
        }).when(emailService).sendResetLink(user, OTP, RESET_LINK);

        authService.requestPasswordReset(EMAIL);

        assertEquals(user, sentUser[0]);
        assertEquals(OTP, sentToken[0]);
        assertEquals(RESET_LINK, sentLink[0]);
    }

    @Test
    public void testRequestPasswordResetNonExistingUser() {
        when(userService.findUserByEmail(MISSING_EMAIL)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> authService.requestPasswordReset(MISSING_EMAIL));
    }
}
