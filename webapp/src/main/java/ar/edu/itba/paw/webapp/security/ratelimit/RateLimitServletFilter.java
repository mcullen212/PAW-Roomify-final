package ar.edu.itba.paw.webapp.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Component
public class RateLimitServletFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimitingService rateLimitingService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isDiscoveryAuth = "HEAD".equalsIgnoreCase(method) &&
                (path.endsWith("/api/") || path.endsWith("/api"));

        if (isDiscoveryAuth) {
            return false;
        }

        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = request.getRemoteAddr();
        Bucket bucket = rateLimitingService.resolveBucket(clientIp);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(Response.Status.TOO_MANY_REQUESTS.getStatusCode());
            response.setContentType("application/json");
            response.getWriter().write(Response.Status.TOO_MANY_REQUESTS.getReasonPhrase());
            response.getWriter().flush();
        }
    }
}