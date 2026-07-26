package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.service.AuthService;
import ar.edu.itba.paw.webapp.security.jwt.JwtTokenUtil;
import java.util.Optional;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import ar.edu.itba.paw.webapp.mediaType.VndType;

@Path("/")
@Component
public class DiscoveryController {

    @HEAD
    @Produces(VndType.APPLICATION_API)
    public Response headDiscovery(
            @Context SecurityContext securityContext
    ) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        return Response.noContent().build();
    }
}