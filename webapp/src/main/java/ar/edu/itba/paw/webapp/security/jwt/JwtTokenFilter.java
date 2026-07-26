package ar.edu.itba.paw.webapp.security.jwt;

import ar.edu.itba.paw.webapp.security.PawUserDetailsService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private PawUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            TokenValidation validation = validateToken(token);
            if (validation == null || validation.claims == null) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            tryAuthenticate(validation, request, response);
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.split(" ")[1].trim();
        }
        return null;
    }

    private void tryAuthenticate(TokenValidation validation, HttpServletRequest request, HttpServletResponse response) {
        if (validation == null || validation.claims == null) return;

        String scope = null;
        Object scopeObj = validation.claims.get("scope");
        if (scopeObj != null) scope = scopeObj.toString();

        if (validation.isRefresh) {
            UserDetails user = userDetailsService.loadUserByUsername(validation.claims.getSubject());
            if (user != null && user.isEnabled() && user.isAccountNonLocked()) {
                renewTokens(user.getUsername(), response);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    setAuthenticationContext(user, request);
                }
            }
            return;
        }

        if (JwtTokenUtil.SCOPE_TEMPORARY.equals(scope)) {
            Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
            Object roles = validation.claims.get("roles");
            if (roles instanceof List) {
                for (Object r : (List<?>) roles) {
                    if (r != null) authorities.add(new SimpleGrantedAuthority(r.toString()));
                }
            }

            String username = validation.claims.getSubject();

            UserDetails principal = new User(username, "", true, true, true, true, authorities);
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            request.setAttribute("TEMP_AUTH_USER", username);
            return;
        }

        UserDetails user = userDetailsService.loadUserByUsername(validation.claims.getSubject());
        if (user == null || !user.isEnabled() || !user.isAccountNonLocked()) return;

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            setAuthenticationContext(user, request);
        }
    }

    private TokenValidation validateToken(String token) {
        Claims claims = jwtTokenUtil.parseAccessToken(token);
        if (claims != null) {
            boolean isTemp = false;
            Object scope = claims.get("scope");
            if (scope != null && JwtTokenUtil.SCOPE_TEMPORARY.equals(scope.toString())) {
                isTemp = true;
            }
            return new TokenValidation(claims, false, isTemp);
        }

        claims = jwtTokenUtil.parseRefreshToken(token);
        if (claims != null) {
            return new TokenValidation(claims, true, false);
        }

        return null;
    }

    private void renewTokens(String username, HttpServletResponse response) {
        String newAccess = jwtTokenUtil.createAccessToken(username);
        String newRefresh = jwtTokenUtil.createRefreshToken(username);

        response.setHeader("access-token", newAccess);
        response.setHeader("refresh-token", newRefresh);
        response.setHeader("Access-Control-Expose-Headers", "access-token, refresh-token");
    }

    private void setAuthenticationContext(UserDetails user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private static class TokenValidation {
        final Claims claims;
        final boolean isRefresh;
        final boolean isTemporary;

        TokenValidation(Claims claims, boolean isRefresh, boolean isTemporary) {
            this.claims = claims;
            this.isRefresh = isRefresh;
            this.isTemporary = isTemporary;
        }

        boolean isTemporary() {
            return isTemporary;
        }
    }
}
