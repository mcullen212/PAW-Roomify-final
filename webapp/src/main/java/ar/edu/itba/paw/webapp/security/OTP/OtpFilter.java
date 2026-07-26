package ar.edu.itba.paw.webapp.security.OTP;

import ar.edu.itba.paw.interfaces.service.AuthService;
import ar.edu.itba.paw.interfaces.service.UserService;
import ar.edu.itba.paw.model.token.TokenType;
import ar.edu.itba.paw.webapp.security.jwt.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class OtpFilter extends OncePerRequestFilter {

    private static final int EMAIL_IDX = 0;
    private static final int OTP_IDX = 1;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Basic ") ||
                SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        final String[] credentials;
        try {
            credentials = extractAndDecodeHeader(header);
        } catch (BadCredentialsException failed) {
            chain.doFilter(request, response);
            return;
        }

        authService.consumeOtp(credentials[EMAIL_IDX], credentials[OTP_IDX]).ifPresent(token -> {
            String email = token.getUser().getEmail();

            if (token.getType() == TokenType.RESET_PASSWORD) {
                List<GrantedAuthority> restrictedAuthorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_RESET_PASSWORD_PRIVILEGE"));
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, restrictedAuthorities);

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                String accessToken = jwtTokenUtil.createShortLivedToken(email, restrictedAuthorities);

                response.addHeader("access-token", accessToken);
            } else if (token.getType() == TokenType.VERIFY_EMAIL) {
                if (!userService.verifiedEmail(token.getUser().getId())) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                response.addHeader("access-token", jwtTokenUtil.createAccessToken(email));
                response.setHeader("refresh-token", jwtTokenUtil.createRefreshToken(email));
            } else {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        });

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"HEAD".equalsIgnoreCase(request.getMethod()) ||
                !(path.endsWith("/api/") || path.endsWith("/api"));
    }

    private String[] extractAndDecodeHeader(String header) {
        byte[] encoded = header.substring("Basic ".length()).trim().getBytes(StandardCharsets.UTF_8);
        final byte[] decoded;
        try {
            decoded = Base64.decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }

        String token = new String(decoded, StandardCharsets.UTF_8);
        int delimiter = token.indexOf(':');
        if (delimiter < 1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }

        return new String[]{token.substring(0, delimiter), token.substring(delimiter + 1)};
    }
}
