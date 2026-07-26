package ar.edu.itba.paw.webapp.security.handlers;

import ar.edu.itba.paw.webapp.mediaType.VndType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class VerificationAccessDeniedHandler implements AccessDeniedHandler {
    private static final String VERIFIED_USER_AUTHORITY = "ROLE_VERIFIED_USER";
    private static final String VERIFICATION_REQUIRED_CODE = "EMAIL_VERIFICATION_REQUIRED";

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        response.setContentType(VndType.APPLICATION_API);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isMissingEmailVerification(authentication)) {
            response.getWriter().write(
                    String.format("{\n \"code\": \"%s\",\n \"message\": \"Email verification required.\"\n}", VERIFICATION_REQUIRED_CODE)
            );
            return;
        }

        response.getWriter().write(
                String.format("{\n \"message\": \"%s\"\n}", accessDeniedException.getMessage())
        );
    }

    private boolean isMissingEmailVerification(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !hasVerifiedUserAuthority(authentication);
    }

    private boolean hasVerifiedUserAuthority(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> VERIFIED_USER_AUTHORITY.equals(authority.getAuthority()));
    }
}
