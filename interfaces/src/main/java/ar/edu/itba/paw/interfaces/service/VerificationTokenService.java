package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

public interface VerificationTokenService {
    VerificationToken createToken(User user, TokenType type, Duration lifetime);

    Optional<VerificationToken> validateToken(String tokenValue) ;

    void invalidateToken(String tokenValue);

    void deleteToken(String token);

    Optional<VerificationToken> findByToken(String token);
    void deleteExpiredTokens();
}
