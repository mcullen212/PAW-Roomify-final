package ar.edu.itba.paw.interfaces.service;

import ar.edu.itba.paw.model.token.VerificationToken;

import java.util.Optional;

public interface AuthService {

    void sendVerificationEmail(String email);

    void requestPasswordReset(String email);

    Optional<VerificationToken> consumeOtp(String email, String otp);
}
