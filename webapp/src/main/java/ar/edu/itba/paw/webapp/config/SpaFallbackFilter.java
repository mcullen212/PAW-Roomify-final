package ar.edu.itba.paw.webapp.config;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SpaFallbackFilter implements Filter {

    private static final List<String> EXCLUDED_PREFIXES = Arrays.asList(
            "/api",
            "/assets",
            "/static",
            "/css",
            "/js",
            "/images",
            "/WEB-INF"
    );

    @Override
    public void init(FilterConfig filterConfig) {
        // No configuration needed.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final String path = getPathWithinApplication(httpRequest);

        if (shouldForwardToIndex(httpRequest, path)) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No resources to release.
    }

    private boolean shouldForwardToIndex(HttpServletRequest request, String path) {
        return isNavigationRequest(request)
                && !isExcluded(path)
                && !isFileRequest(path);
    }

    private boolean isNavigationRequest(HttpServletRequest request) {
        final String method = request.getMethod();
        final String accept = request.getHeader("Accept");
        return ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method))
                && (accept == null || accept.contains("text/html"));
    }

    private boolean isExcluded(String path) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return "/index.html".equals(path);
    }

    private boolean isFileRequest(String path) {
        final int lastSlash = path.lastIndexOf('/');
        final int lastDot = path.lastIndexOf('.');
        return lastDot > lastSlash;
    }

    private String getPathWithinApplication(HttpServletRequest request) {
        final String requestUri = request.getRequestURI();
        final String contextPath = request.getContextPath();
        if (contextPath == null || contextPath.isEmpty()) {
            return requestUri;
        }
        return requestUri.substring(contextPath.length());
    }
}
