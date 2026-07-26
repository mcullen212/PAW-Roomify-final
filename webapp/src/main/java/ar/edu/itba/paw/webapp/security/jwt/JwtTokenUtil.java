package ar.edu.itba.paw.webapp.security.jwt;

import ar.edu.itba.paw.webapp.config.PawUserDetails;
import ar.edu.itba.paw.webapp.security.PawUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class JwtTokenUtil {

    @Autowired
    private PawUserDetailsService userDetailsService;


    private static final long AUTH_EXPIRY_TIME = 2 * 15 * 60 * 1000L; // 30 mins
    private static final long REFRESH_EXPIRY_TIME = 7 * 24 * 60 * 60 * 1000L; // 7 days (Refresh Token)
    private static final long SHORT_LIVED_EXPIRY_TIME = 5 * 60 * 1000L; // 5 min

    public static final String SCOPE_FULL_ACCESS = "FULL";
    public static final String SCOPE_TEMPORARY = "TEMP_AUTH";

    private final Key accessKey;
    private final Key refreshKey;

    public JwtTokenUtil(Resource accessKeyResource, Resource refreshKeyResource) throws IOException {
        this.accessKey = Keys.hmacShaKeyFor(
                FileCopyUtils.copyToString(new InputStreamReader(accessKeyResource.getInputStream()))
                        .getBytes(StandardCharsets.UTF_8)
        );
        this.refreshKey = Keys.hmacShaKeyFor(
                FileCopyUtils.copyToString(new InputStreamReader(refreshKeyResource.getInputStream()))
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims parseAccessToken(String jws) {
        return parseTokenClaims(jws, accessKey);
    }

    public Claims parseRefreshToken(String jws) {
        return parseTokenClaims(jws, refreshKey);
    }

    private Claims parseTokenClaims(String jws, Key key) {
        try {
            if (jws != null && jws.startsWith("Bearer ")) {
                jws = jws.substring(7);
            }

            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jws)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public String createAccessToken(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return generateToken(
                email,
                AUTH_EXPIRY_TIME,
                SCOPE_FULL_ACCESS,
                accessKey,
                new ArrayList<>(userDetails.getAuthorities())
        );
    }

    public String createRefreshToken(String email) {
        return generateToken(email, REFRESH_EXPIRY_TIME, SCOPE_FULL_ACCESS, refreshKey, null);
    }

    public String createShortLivedToken(String email, List<GrantedAuthority> authorities) {
        return generateToken(email, SHORT_LIVED_EXPIRY_TIME, SCOPE_TEMPORARY, accessKey, authorities);
    }

    private String generateToken(String email, long expiryTime, String scope, Key key, List<GrantedAuthority> authorities) {
        Claims claims = Jwts.claims();
        claims.setSubject(email);
        claims.put("scope", scope);

        if (authorities != null && !authorities.isEmpty()) {
            claims.put("roles", authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (userDetails instanceof PawUserDetails) {
            claims.put("userId", ((PawUserDetails) userDetails).getId());
        }

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiryTime))
                .signWith(key)
                .compact();
    }
}
