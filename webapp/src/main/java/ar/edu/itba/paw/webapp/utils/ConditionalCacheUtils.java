package ar.edu.itba.paw.webapp.utils;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ConditionalCacheUtils {

    private static final String VARY_HEADER = "Vary";
    private static final String VARY_ACCEPT = "Accept";
    private static final String VARY_ACCEPT_AUTHORIZATION = "Accept, Authorization";

    private ConditionalCacheUtils() {
        // Utility class.
    }

    public static Response buildResponseUsingEtag(final Request request,
                                                  final int hashCode,
                                                  final Supplier<Response.ResponseBuilder> responseBuilderSupplier) {
        return buildResponseUsingEtag(request, hashCode, responseBuilderSupplier, false);
    }

    public static Response buildPrivateResponseUsingEtag(final Request request,
                                                         final int hashCode,
                                                         final Supplier<Response.ResponseBuilder> responseBuilderSupplier) {
        return buildResponseUsingEtag(request, hashCode, responseBuilderSupplier, true);
    }

    public static List<Integer> buildEtagHashList(final List<?> dtos) {
        return dtos.stream()
                .map(Object::hashCode)
                .collect(Collectors.toList());
    }

    public static int buildEtagHash(final Object... hashFields) {
        return Objects.hash(hashFields);
    }

    public static int buildCollectionEtagHash(final List<?> dtos, final Object... hashFields) {
        final List<Object> etagFields = new ArrayList<>(Arrays.asList(hashFields));
        etagFields.add(buildEtagHashList(dtos));
        return buildEtagHash(etagFields.toArray());
    }

    private static Response buildResponseUsingEtag(final Request request,
                                                   final int hashCode,
                                                   final Supplier<Response.ResponseBuilder> responseBuilderSupplier,
                                                   final boolean privateCache) {
        final EntityTag eTag = new EntityTag(String.valueOf(hashCode));
        final Response.ResponseBuilder preconditionResponse = request.evaluatePreconditions(eTag);

        final Response.ResponseBuilder response = preconditionResponse != null
                ? preconditionResponse
                : responseBuilderSupplier.get();

        return addConditionalHeaders(response, privateCache)
                .tag(eTag)
                .build();
    }

    private static Response.ResponseBuilder addConditionalHeaders(final Response.ResponseBuilder response,
                                                                 final boolean privateCache) {
        final CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setPrivate(privateCache);

        return response
                .cacheControl(cacheControl)
                .header(VARY_HEADER, privateCache ? VARY_ACCEPT_AUTHORIZATION : VARY_ACCEPT);
    }
}
