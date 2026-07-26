package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.service.ImageService;
import ar.edu.itba.paw.model.Image;
import ar.edu.itba.paw.webapp.DTO.Input.ImageInputDTO;
import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.NonConditionalCacheUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;

@Path("images")
@Component
public class ImageController {

    private final ImageService imageService;

    @Context
    private UriInfo uriInfo;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(VndType.APPLICATION_IMAGE)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER')")
    public Response uploadImage(
            @FormDataParam("image") byte[] imageBytes,
            @FormDataParam("image") FormDataBodyPart imagePart
    ) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Image image = imageService.uploadRoomImage(email, ImageInputDTO.fromMultipart(imageBytes, imagePart));

        final URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(image.getId()))
                .build();

        return Response.created(location).build();
    }

    @GET
    @Path("/{id}")
    public Response getImage(@PathParam("id") long id) {
        final Image image = imageService.findImageById(id)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        return NonConditionalCacheUtils.setPublicMaxAge(
                Response.ok(image.getData())
                        .type(image.getContentType()),
                NonConditionalCacheUtils.IMAGE_MAX_AGE_SECONDS
        ).build();
    }
}
