package ar.edu.itba.paw.webapp.utils;

import javax.ws.rs.core.Response;

public final class NonConditionalCacheUtils {

    public static final int IMAGE_MAX_AGE_SECONDS = 2592000;
    public static final int COUNTRIES_MAX_AGE_SECONDS = 2592000;
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final String PUBLIC_MAX_AGE_CACHE_CONTROL = "public, max-age=%d";

    private NonConditionalCacheUtils() {
        // Utility class.
    }

    public static Response.ResponseBuilder setPublicMaxAge(final Response.ResponseBuilder response, final int maxAgeSeconds) {
        return response.header(CACHE_CONTROL_HEADER, String.format(PUBLIC_MAX_AGE_CACHE_CONTROL, maxAgeSeconds));
    }
}
