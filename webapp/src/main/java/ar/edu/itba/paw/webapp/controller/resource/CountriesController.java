package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.CountryUtils;
import ar.edu.itba.paw.webapp.utils.NonConditionalCacheUtils;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("countries")
@Component
public class CountriesController {

    @GET
    @Produces(VndType.APPLICATION_COUNTRIES)
    public Response getCountries() {
        final List<String> countries = CountryUtils.getCountries();
        return NonConditionalCacheUtils.setPublicMaxAge(
                Response.ok(new GenericEntity<List<String>>(countries) {}),
                NonConditionalCacheUtils.COUNTRIES_MAX_AGE_SECONDS
        ).build();
    }
}
