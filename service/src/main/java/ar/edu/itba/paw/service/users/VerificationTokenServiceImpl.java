package ar.edu.itba.paw.service.users;

import ar.edu.itba.paw.interfaces.persistence.VerificationTokenDao;
import ar.edu.itba.paw.interfaces.service.VerificationTokenService;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.model.token.VerificationToken;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class VerificationTokenServiceImpl implements VerificationTokenService {
    private final VerificationTokenDao tokenDao;
    private static final int TOKEN_LENGTH = 6;

    public VerificationTokenServiceImpl(VerificationTokenDao tokenDao) {
        this.tokenDao = tokenDao;
    }

    @Override
    @Transactional
    public VerificationToken createToken(User user, TokenType type, Duration lifetime) {
        tokenDao.findByUser(user.getId()).ifPresent(existingToken -> {
            invalidateToken(existingToken.getToken());
        });

        String tokenValue = generateAlphaNumericToken(TOKEN_LENGTH);
        Instant expiry = Instant.now().plus(lifetime);

        return tokenDao.save(user, tokenValue, type, expiry);
    }

    private String generateAlphaNumericToken(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Override
    public Optional<VerificationToken> findByToken(String token){
        return tokenDao.findByToken(token);
    }

    @Override
    public Optional<VerificationToken> validateToken(String tokenValue) {
        return tokenDao.findByToken(tokenValue)
                .filter(t -> !t.isExpired());
    }

    @Override
    public void invalidateToken(String tokenValue) {
        tokenDao.delete(tokenValue);
    }

    @Override
    public void deleteToken(String token){
        tokenDao.delete(token);
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void deleteExpiredTokens() {
        tokenDao.deleteExpiredTokens();
    }
}
