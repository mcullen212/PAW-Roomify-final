package ar.edu.itba.paw.webapp.utils;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public class PaginationUtils {

    private PaginationUtils() {
        // Utility class
    }

    public static void addLinks(Response.ResponseBuilder responseBuilder, UriInfo uriInfo, int page, int totalPages) {
        if (totalPages <= 0) {
            return;
        }

        final int safePage = clampPage(page, totalPages);

        Link first = Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", 1))
                .rel("first").build();

        Link last = Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", totalPages))
                .rel("last").build();

        responseBuilder.links(first, last);

        if (safePage > 1) {
            Link prev = Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", safePage - 1))
                    .rel("prev").build();
            responseBuilder.links(prev);
        }

        if (safePage < totalPages) {
            Link next = Link.fromUriBuilder(uriInfo.getRequestUriBuilder().replaceQueryParam("page", safePage + 1))
                    .rel("next").build();
            responseBuilder.links(next);
        }
    }

    public static int clampPage(int page, int totalPages) {
        return Math.min(Math.max(page, 1), totalPages);
    }
}
