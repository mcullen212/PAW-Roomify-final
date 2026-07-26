package ar.edu.itba.paw.webapp.config;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CacheFilter extends OncePerRequestFilter {
    private static final long ONE_YEAR_IN_SECONDS = 31536000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        response.setHeader(
                "Cache-Control",
                "public, max-age=" + ONE_YEAR_IN_SECONDS + ", immutable"
        );
        chain.doFilter(request, response);
    }
}