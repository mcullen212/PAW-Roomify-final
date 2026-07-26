package ar.edu.itba.paw.service.auth;

import ar.edu.itba.paw.interfaces.exceptions.UserNotFoundException;
import ar.edu.itba.paw.interfaces.service.AuthService;
import ar.edu.itba.paw.interfaces.service.EmailService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.interfaces.service.VerificationTokenService;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AuthServiceImpl.class
    );

    private final UserService userService;
    private final EmailService emailService;
    private final VerificationTokenService tokenService;

    @Value("${app.url}")
    private String appUrl;

    private static final String VERIFY_LINK_TEMPLATE = "/verify-token?type=verify";
    private static final String RESET_LINK_TEMPLATE = "/verify-token?type=reset";

    @Autowired
    public AuthServiceImpl(
            UserService userService,
            EmailService emailService,
            VerificationTokenService tokenService
    ) {
        this.userService = userService;
        this.emailService = emailService;
        this.tokenService = tokenService;
    }

    @Override
    @Transactional
    public Optional<VerificationToken> consumeOtp(String email, String otp) {
        Optional<VerificationToken> tokenOpt = tokenService.validateToken(otp);

        if (tokenOpt.isEmpty()) {
            return Optional.empty();
        }

        VerificationToken token = tokenOpt.get();
        if (!token.getUser().getEmail().equalsIgnoreCase(email)) {
            return Optional.empty();
        }

        tokenService.invalidateToken(token.getToken());

        return tokenOpt;
    }

    @Override
    public void sendVerificationEmail(String email) {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        createAndSendVerificationToken(user);
    }

    @Override
    public void requestPasswordReset(String email) {
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        createAndSendResetPasswordToken(user);
    }

    private void createAndSendVerificationToken(final User user) {
        VerificationToken token = createToken(user, TokenType.VERIFY_EMAIL, Duration.ofHours(24));
        emailService.sendVerifyEmail(token.getUser(), token, buildTokenLink(VERIFY_LINK_TEMPLATE, user.getEmail()));
    }

    private void createAndSendResetPasswordToken(final User user) {
        VerificationToken token = createToken(user, TokenType.RESET_PASSWORD, Duration.ofHours(1));
        emailService.sendResetLink(token.getUser(), token.getToken(), buildTokenLink(RESET_LINK_TEMPLATE, user.getEmail()));
    }

    private String buildTokenLink(String template, String email) {
        return appUrl + template + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8);
    }

    private VerificationToken createToken(final User user, final TokenType type, final Duration duration) {
        return tokenService.createToken(user, type, duration);
    }
}
