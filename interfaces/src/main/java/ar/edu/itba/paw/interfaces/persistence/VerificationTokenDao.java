package ar.edu.itba.paw.interfaces.persistence;

import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenDao {
    VerificationToken save(User user, String token, TokenType type, Instant expiryDate);
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(long userId);
    boolean delete(String token);
    boolean deleteExpiredTokens();
}
