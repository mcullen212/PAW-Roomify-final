package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.service.*;
import ar.edu.itba.paw.model.DTO.ContactPage;
import ar.edu.itba.paw.model.swaps.Contact;
import ar.edu.itba.paw.model.swaps.ContactView;
import ar.edu.itba.paw.webapp.DTO.Input.ContactInputDTO;
import ar.edu.itba.paw.webapp.DTO.Input.ContactUpdateDTO;
import ar.edu.itba.paw.webapp.DTO.Output.ContactOutputDTO;
import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.ConditionalCacheUtils;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("contacts")
@Component
public class ContactController {

    private final ContactService contactService;
    private final SwapRequestService swapRequestService;

    @Context
    private UriInfo uriInfo;

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactController.class);

    @Autowired
    public ContactController(ContactService contactService, SwapRequestService swapRequestService) {
        this.contactService = contactService;
        this.swapRequestService = swapRequestService;
    }

    @GET
    @Produces(VndType.APPLICATION_CONTACT)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER')")
    public Response getContacts(@QueryParam("view") @DefaultValue("sent") String view,
                                @QueryParam("page") @DefaultValue("1") int page,
                                @QueryParam("pageSize") @DefaultValue("10") int pageSize,
                                @QueryParam("tripId") Long tripId,
                                @Context Request request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        ContactPage contactPage = contactService.findContactsPage(email, ContactView.fromQueryParam(view), tripId, page, pageSize);
        List<ContactOutputDTO> contactDtos = contactPage.getContacts().stream()
                .map(contact -> new ContactOutputDTO(contact, uriInfo, contactPage.isReviewPending(contact.getId())))
                .collect(Collectors.toList());

        final int hashCode = ConditionalCacheUtils.buildCollectionEtagHash(
                contactDtos,
                contactPage.getCurrentPage(),
                contactPage.getPageSize(),
                contactPage.getTotalItems(),
                contactPage.getTotalPages()
        );

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
                request,
                hashCode,
                () -> {
                    Response.ResponseBuilder response = Response.ok(new GenericEntity<>(contactDtos) {});
                    PaginationUtils.addLinks(response, uriInfo, contactPage.getCurrentPage(), contactPage.getTotalPages());
                    return response;
                }
        );
    }

    @GET
    @Path("/{id}")
    @Produces(VndType.APPLICATION_CONTACT_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.belongsToSwap(#contactId, authentication)")
    public Response getContact(@PathParam("id") long contactId, @Context Request request) {
        Contact contact = contactService.getContactById(contactId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        ContactOutputDTO contactDto = new ContactOutputDTO(contact, uriInfo);

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
                request,
                contactDto.hashCode(),
                () -> Response.ok(contactDto)
        );
    }

    @POST
    @Consumes(VndType.APPLICATION_CONTACT_DETAIL)
    @Produces(VndType.APPLICATION_CONTACT_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER')")
    public Response requestContact(@Valid ContactInputDTO form,
                                   @QueryParam("tripId") Long tripId) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Contact contact = swapRequestService.processSwapRequest(
                form.getRoomRequestedId(),
                form.getCheckIn(),
                form.getCheckOut(),
                form.getIsSwap(),
                form.getDayPrice(),
                form.getRoomOfferedId(),
                tripId,
                email
        );

        LOGGER.info("Swap request created with ID {} by user {}", contact.getId(), email);

        URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(contact.getId()))
                .build();

        return Response.created(location).build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndType.APPLICATION_CONTACT_DETAIL)
    @Produces(VndType.APPLICATION_CONTACT_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER')")
    public Response updateContact(@PathParam("id") long contactId,
                                  @NotNull(message = "Contact update body is required") @Valid ContactUpdateDTO contactUpdateDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Contact contact = contactService.updateContactStatus(
                contactId,
                contactUpdateDto.getParsedStatus(),
                contactUpdateDto.getCheckIn(),
                contactUpdateDto.getCheckOut(),
                email
        );

        return Response.ok(new ContactOutputDTO(contact, uriInfo)).build();
    }
}
