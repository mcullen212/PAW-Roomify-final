package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.exceptions.DateRangeException;
import ar.edu.itba.paw.interfaces.exceptions.GroupTripNotFoundException;
import ar.edu.itba.paw.interfaces.service.GroupTripService;
import ar.edu.itba.paw.interfaces.service.TripService;
import ar.edu.itba.paw.model.DTO.GroupTripPage;
import ar.edu.itba.paw.model.trip.GroupTrip;
import ar.edu.itba.paw.model.trip.Trip;
import ar.edu.itba.paw.model.trip.TripStatus;
import ar.edu.itba.paw.webapp.DTO.Input.CreateGroupTripDTO;
import ar.edu.itba.paw.webapp.DTO.Input.CreateTripDTO;
import ar.edu.itba.paw.webapp.DTO.Input.UpdateGroupTripDTO;
import ar.edu.itba.paw.webapp.DTO.Input.UpdateTripDTO;
import ar.edu.itba.paw.webapp.DTO.Output.GroupTripDTO;
import ar.edu.itba.paw.webapp.DTO.Output.TripDTO;
import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.ConditionalCacheUtils;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Path("group-trips")
@Component
public class GroupTripsController {

    private final GroupTripService groupTripService;
    private final TripService tripService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public GroupTripsController(final GroupTripService groupTripService,
                                final TripService tripService) {
        this.groupTripService = groupTripService;
        this.tripService = tripService;
    }

    @POST
    @Consumes(VndType.APPLICATION_GROUP_TRIP_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isAuthorized(#createGroupTripDTO.ownerId, authentication)")
    public Response createGroupTrip(@Valid CreateGroupTripDTO createGroupTripDTO) {
        final GroupTrip created = groupTripService.create(
                createGroupTripDTO.getOwnerId(),
                createGroupTripDTO.getTitle()
        );

        final URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(created.getId()))
                .build();

        return Response.created(location).build();
    }

    @POST
    @Path("/{groupTripId}/trips")
    @Consumes(VndType.APPLICATION_GROUP_TRIP_DESTINATION)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isOwnerTrip(#groupTripId, authentication)")
    public Response createTrip(@PathParam("groupTripId") long groupTripId,
                               @Valid CreateTripDTO createTripDTO)
            throws DateRangeException {
        final Trip created = tripService.create(
                groupTripId,
                createTripDTO.getCountry(),
                createTripDTO.getStartDate(),
                createTripDTO.getEndDate()
        );

        final URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(created.getId()))
                .build();

        return Response.created(location).build();
    }

    @GET
    @Produces(VndType.APPLICATION_GROUP_TRIP)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isAuthorized(#userId, authentication)")
    public Response getMyGroupTrips(
                @QueryParam("userId") Long userId,
                @QueryParam("country") String country,
                @QueryParam("checkIn") String checkIn,
                @QueryParam("checkOut") String checkOut,
                @QueryParam("status") TripStatus status,
                @QueryParam("page") @DefaultValue("1") int page,
                @QueryParam("pageSize") @DefaultValue("12") int pageSize,
                @Context Request request
    ) {
        final int safePage = Math.max(page, 1);
        final int safePageSize = Math.max(pageSize, 1);
        final GroupTripPage groupTripPage = groupTripService.searchGroupTrips(
                userId,
                country,
                checkIn,
                checkOut,
                status,
                safePage,
                safePageSize
        );

        final List<GroupTripDTO> dtos = groupTripPage
                .getItems()
                .stream()
                .map(item -> new GroupTripDTO(item, uriInfo))
                .collect(Collectors.toList());

        final int hashCode = ConditionalCacheUtils.buildCollectionEtagHash(
                dtos,
                userId,
                country,
                checkIn,
                checkOut,
                status,
                safePage,
                safePageSize,
                groupTripPage.getTotalItems(),
                groupTripPage.getTotalPages()
        );

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
                request,
                hashCode,
                () -> {
                    Response.ResponseBuilder responseBuilder = Response.ok(new GenericEntity<List<GroupTripDTO>>(dtos) {});
                    PaginationUtils.addLinks(responseBuilder, uriInfo, safePage, groupTripPage.getTotalPages());
                    return responseBuilder;
                }
        );
    }

    @GET
    @Path("/{id}")
    @Produces(VndType.APPLICATION_GROUP_TRIP_DETAIL)
    @PreAuthorize("@webAuthHelper.isOwnerTrip(#id, authentication)")
    public Response getGroupTrip(@PathParam("id") long id,
                                 @Context Request request) {
        final GroupTrip groupTrip = groupTripService.findGroupTripById(id)
                .orElseThrow(() -> new GroupTripNotFoundException(id));
        final GroupTripDTO groupTripDTO = new GroupTripDTO(groupTrip, uriInfo);

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
                request,
                groupTripDTO.hashCode(),
                () -> Response.ok(groupTripDTO)
        );
    }

    @GET
    @Path("/{groupTripId}/trips")
    @Produces(VndType.APPLICATION_GROUP_TRIP_DESTINATION)
    @PreAuthorize("@webAuthHelper.isOwnerTrip(#groupTripId, authentication)")
    public Response getTripsByGroupTrip(
            @PathParam("groupTripId") long groupTripId,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("12") int pageSize,
            @Context Request request
    ) {
        final int safePage = Math.max(page, 1);
        final int safePageSize = Math.max(pageSize, 1);

        final List<TripDTO> dtos = tripService
                .findTripsByGroupTripId(groupTripId, safePage, safePageSize)
                .stream()
                .map(trip -> new TripDTO(trip, uriInfo))
                .collect(Collectors.toList());

        final int totalItems = tripService.countTripsByGroupTripId(groupTripId);
        final int totalPages = (int) Math.ceil((double) totalItems / safePageSize);

        final int hashCode = ConditionalCacheUtils.buildCollectionEtagHash(
                dtos,
                groupTripId,
                safePage,
                safePageSize,
                totalItems,
                totalPages
        );

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
                request,
                hashCode,
                () -> {
                    Response.ResponseBuilder responseBuilder = Response.ok(new GenericEntity<>(dtos) {});
                    PaginationUtils.addLinks(responseBuilder, uriInfo, safePage, totalPages);
                    return responseBuilder;
                }
        );
    }

    @GET
    @Path("/{groupTripId}/trips/{tripId}")
    @Produces(VndType.APPLICATION_GROUP_TRIP_DESTINATION_DETAIL)
    @PreAuthorize("@webAuthHelper.isOwnerTrip(#groupTripId, authentication)")
    public Response getTrip(
            @PathParam("groupTripId") long groupTripId,
            @PathParam("tripId") long tripId,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("12") int pageSize,
            @Context Request request)
    {
        final int safePage = Math.max(page, 1);
        final int safePageSize = Math.max(pageSize, 1);
        final TripDTO tripDTO = new TripDTO(
                tripService.findTripByGroupTripId(groupTripId, tripId),
                uriInfo,
                safePage,
                safePageSize
        );

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
                request,
                tripDTO.hashCode(),
                () -> Response.ok(tripDTO)
        );
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndType.APPLICATION_GROUP_TRIP_DETAIL)
    @Produces(VndType.APPLICATION_GROUP_TRIP_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isOwnerTrip(#id, authentication)")
    public Response updateGroupTrip(@PathParam("id") long id, @Valid UpdateGroupTripDTO updateGroupTripDTO) {
        final GroupTrip updated = groupTripService.updateGroupTrip(id, updateGroupTripDTO.getTitle(), updateGroupTripDTO.getStatus());

        return Response.ok(new GroupTripDTO(updated, uriInfo)).build();
    }

    @PATCH
    @Path("/{groupTripId}/trips/{id}")
    @Consumes(VndType.APPLICATION_GROUP_TRIP_DETAIL)
    @Produces(VndType.APPLICATION_GROUP_TRIP_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isOwnerTrip(#groupTripId, authentication)")
    public Response updateGroupTrip(
            @PathParam("groupTripId") long groupTripId,
            @PathParam("id") long id,
            @Valid UpdateTripDTO updateTripDTO) {

        final Trip trip = tripService.matchRoomToTrip(updateTripDTO.getRoomId(), groupTripId, id);

        return Response.ok(new TripDTO(trip, uriInfo)).build();
    }

}
