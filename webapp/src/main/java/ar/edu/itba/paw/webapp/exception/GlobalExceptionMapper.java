package ar.edu.itba.paw.webapp.exception;

import ar.edu.itba.paw.interfaces.exceptions.*;
import ar.edu.itba.paw.webapp.DTO.Output.ApiErrorDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionMapper.class);

    // Inyectado por jersey-spring5 (mismo mecanismo que los recursos JAX-RS).
    @Autowired
    private MessageSource messageSource;

    // El Locale sale del Accept-Language del request (proxy per-request de Jersey).
    @Context
    private HttpHeaders headers;

    public Response toResponse(Throwable exception) {

        // --- 400 ---
        if (exception instanceof ConstraintViolationException) {
            // Bean Validation ya resuelve sus mensajes por clave i18n en el validador.
            String errorMessage = ((ConstraintViolationException) exception).getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            return buildResponse(Response.Status.BAD_REQUEST, errorMessage);
        }
        if (exception instanceof InvalidPasswordException) {
            return buildResponse(Response.Status.BAD_REQUEST, resolve("error.password.invalid", exception));
        }
        if (exception instanceof RoomValidationException) {
            return buildResponse(Response.Status.BAD_REQUEST, resolveLocalized((RoomValidationException) exception));
        }
        if (exception instanceof IllegalArgumentException) {
            return buildResponse(Response.Status.BAD_REQUEST, resolve("error.badRequest", exception));
        }
        if (exception instanceof BusinessException) {
            return buildResponse(Response.Status.BAD_REQUEST, resolve("error.badRequest", exception));
        }

        // --- 422 ---
        if (exception instanceof DateRangeException) {
            // La excepción ya expone la clave i18n en getLocalizedMessage().
            return buildResponse(422, resolveLocalized((DateRangeException) exception));
        }

        // --- 409 / 422 (familia swap) ---
        if (exception instanceof EmailAlreadyExistsException) {
            return buildResponse(Response.Status.CONFLICT, resolve("error.email.exists", exception));
        }
        // Subtipos de SwapException: chequear ANTES que SwapException.
        if (exception instanceof RoomHasActiveSwapsException) {
            // Su localizedMessage es un literal, no una clave: usamos un código propio.
            return buildResponse(Response.Status.CONFLICT, resolve("error.room.activeSwaps", exception));
        }
        if (exception instanceof BookedDateException) {
            return buildResponse(Response.Status.CONFLICT, resolveLocalized((SwapException) exception));
        }
        if (exception instanceof SwapException) {
            return buildResponse(422, resolveLocalized((SwapException) exception));
        }
        if (exception instanceof InvalidContactUpdateException) {
            return buildResponse(422, resolve("error.contact.invalidUpdate", exception));
        }
        if (exception instanceof InvalidContactStateException) {
            return buildResponse(Response.Status.CONFLICT, resolve("error.contact.invalidState", exception));
        }
        if (exception instanceof CancelException) {
            return buildResponse(Response.Status.CONFLICT, resolve("error.cancel", exception));
        }

        // --- 401 ---
        if (exception instanceof AuthenticationException) {
            return buildResponse(Response.Status.UNAUTHORIZED, resolve("error.credentials.invalid", "Invalid credentials."));
        }

        // --- 403 ---
        if (exception instanceof AccessDeniedException) {
            return buildResponse(Response.Status.FORBIDDEN, resolve("error.accessDenied", "Access denied."));
        }
        if (exception instanceof TripContactsNotOwnerException) {
            return buildResponse(Response.Status.FORBIDDEN, resolve(exception.getLocalizedMessage(), exception));
        }
        if (exception instanceof ForbiddenUserOperationException) {
            return buildResponse(Response.Status.FORBIDDEN, resolve(exception.getLocalizedMessage(), exception));
        }

        // --- 404 ---
        if (exception instanceof UserNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND, resolve(exception.getLocalizedMessage(), exception));
        }
        if (exception instanceof RoomNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND, resolve(exception.getLocalizedMessage(), exception));
        }
        if (exception instanceof ReviewNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND, resolve(exception.getLocalizedMessage(), exception));
        }
        if (exception instanceof TripNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND, resolve(exception.getLocalizedMessage(), exception));
        }
        if (exception instanceof GroupTripNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND, resolve(exception.getLocalizedMessage(), exception));
        }
        if (exception instanceof ContactNotFoundException) {
            return buildResponse(Response.Status.NOT_FOUND, resolve(exception.getLocalizedMessage(), exception));
        }

        // --- JERSEY ---
        if (exception instanceof WebApplicationException) {
            WebApplicationException wae = (WebApplicationException) exception;
            return buildResponse(wae.getResponse().getStatus(), wae.getMessage());
        }

        // --- 500 ---
        LOGGER.error("Unhandled exception: ", exception);
        return buildResponse(Response.Status.INTERNAL_SERVER_ERROR, resolve("error.internal", "An unexpected error occurred."));
    }

    /**
     * Resuelve un código i18n contra el Locale del request. Si no hay MessageSource
     * (p. ej. en un test sin contexto) o falta la clave, cae al mensaje por defecto.
     */
    private String resolve(String code, String defaultMessage) {
        if (messageSource == null || code == null) {
            return defaultMessage;
        }
        try {
            return messageSource.getMessage(code, null, defaultMessage, resolveLocale());
        } catch (Exception e) {
            return defaultMessage;
        }
    }

    /** Variante que usa el mensaje (inglés) de la excepción como fallback. */
    private String resolve(String code, Throwable exception) {
        return resolve(code, exception.getMessage());
    }

    /** Para excepciones que ya cargan la clave i18n en getLocalizedMessage(). */
    private String resolveLocalized(SwapException exception) {
        return resolve(exception.getLocalizedMessage(), exception.getMessage());
    }

    private String resolveLocalized(DateRangeException exception) {
        return resolve(exception.getLocalizedMessage(), exception.getMessage());
    }

    private String resolveLocalized(RoomValidationException exception) {
        return resolve(exception.getLocalizedMessage(), exception.getMessage());
    }

    private Locale resolveLocale() {
        if (headers != null) {
            try {
                List<Locale> languages = headers.getAcceptableLanguages();
                if (languages != null && !languages.isEmpty()) {
                    Locale locale = languages.get(0);
                    // Jersey usa "*" como wildcard cuando no hay Accept-Language.
                    if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isEmpty()
                            && !"*".equals(locale.getLanguage())) {
                        return locale;
                    }
                }
            } catch (Exception ignored) {
                // sin headers disponibles → default
            }
        }
        return Locale.ENGLISH;
    }

    private Response buildResponse(Response.Status status, String message) {
        return buildResponse(status.getStatusCode(), message);
    }

    private Response buildResponse(int statusCode, String message) {
        return Response.status(statusCode)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ApiErrorDTO(statusCode, message))
                .build();
    }
}
