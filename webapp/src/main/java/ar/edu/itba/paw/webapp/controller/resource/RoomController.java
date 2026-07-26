package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.service.RoomAvailabilityCalendarService;
import ar.edu.itba.paw.interfaces.service.RoomService;
import ar.edu.itba.paw.model.DTO.RoomCreateRequest;
import ar.edu.itba.paw.model.DTO.RoomCreationResult;
import ar.edu.itba.paw.model.DTO.RoomAvailabilityCalendar;
import ar.edu.itba.paw.model.DTO.RoomCardResult;
import ar.edu.itba.paw.model.DTO.RoomSearchCriteria;
import ar.edu.itba.paw.model.rooms.Room;
import ar.edu.itba.paw.webapp.DTO.Input.RoomInputDTO;
import ar.edu.itba.paw.webapp.DTO.Input.RoomUpdateDTO;
import ar.edu.itba.paw.webapp.DTO.Output.RoomCardDTO;
import ar.edu.itba.paw.webapp.DTO.Output.RoomAvailabilityCalendarDTO;
import ar.edu.itba.paw.webapp.DTO.Output.RoomOutputDTO;
import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.ConditionalCacheUtils;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Path("rooms")
@Component
public class RoomController {

    private final RoomService roomService;
    private final RoomAvailabilityCalendarService roomAvailabilityCalendarService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public RoomController(RoomService roomService,
                          RoomAvailabilityCalendarService roomAvailabilityCalendarService) {
        this.roomService = roomService;
        this.roomAvailabilityCalendarService = roomAvailabilityCalendarService;
    }

    @GET
    @Produces(VndType.APPLICATION_ROOMS)
    public Response getRooms(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("pageSize") @DefaultValue("12") int pageSize,
            @QueryParam("destination") String destination,
            @QueryParam("checkIn") String checkInStr,
            @QueryParam("checkOut") String checkOutStr,
            @QueryParam("roomType") String roomTypeStr,
            @QueryParam("bedType") String bedTypeStr,
            @QueryParam("privateBathroom") Boolean privateBathroom,
            @QueryParam("privateKitchen") Boolean privateKitchen,
            @QueryParam("userId") Long userId,
            @QueryParam("amenities") List<String> amenities,
            @Context Request request
    ) {
        final int safePage = Math.max(page, 1);
        final int safePageSize = Math.max(pageSize, 1);

        RoomSearchCriteria criteria = roomService.buildSearchCriteria(
                destination,
                checkInStr,
                checkOutStr,
                roomTypeStr,
                bedTypeStr,
                privateBathroom,
                privateKitchen,
                userId,
                amenities
        );

        List<RoomCardResult> rooms = roomService.searchRoomCards(criteria, safePage, safePageSize);

        int totalRooms = roomService.countSearchRooms(criteria);
        int totalPages = (int) Math.ceil((double) totalRooms / safePageSize);

        List<RoomCardDTO> dtos = rooms.stream()
                .map(room -> new RoomCardDTO(room, uriInfo))
                .collect(Collectors.toList());

        final int hashCode = ConditionalCacheUtils.buildCollectionEtagHash(
                dtos,
                safePage,
                safePageSize,
                destination,
                checkInStr,
                checkOutStr,
                roomTypeStr,
                bedTypeStr,
                privateBathroom,
                privateKitchen,
                userId,
                amenities,
                totalRooms,
                totalPages
        );

        return ConditionalCacheUtils.buildResponseUsingEtag(
                request,
                hashCode,
                () -> {
                    Response.ResponseBuilder responseBuilder = Response.ok(new GenericEntity<>(dtos) {});
                    PaginationUtils.addLinks(responseBuilder, uriInfo, safePage, totalPages);
                    return responseBuilder;
                }
        );
    }

    @POST
    @Consumes(VndType.APPLICATION_ROOM_DETAIL)
    @Produces(VndType.APPLICATION_ROOM_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isAuthorized(#roomDTO.userId, authentication)")
    public Response createRoom(@Valid RoomInputDTO roomDTO) {
        RoomCreateRequest request = new RoomCreateRequest(
                roomDTO.getUserId(),
                roomDTO.getTitle(),
                roomDTO.getCountry(),
                roomDTO.getCity(),
                roomDTO.getDescription(),
                roomDTO.getRoomType(),
                roomDTO.getBedType(),
                roomDTO.getPrivateBathroom(),
                roomDTO.getPrivateKitchen(),
                roomDTO.getAmenities(),
                roomDTO.getDateRanges(),
                roomDTO.getDayPrice(),
                roomDTO.getImageId()
        );

        RoomCreationResult result = roomService.createRoomWithAvailability(request);

        final URI location = uriInfo.getAbsolutePathBuilder()
                .path(String.valueOf(result.getRoom().getId()))
                .build();

        return Response.created(location).build();
    }

    @GET
    @Path("/{id}")
    @Produces(VndType.APPLICATION_ROOM_DETAIL)
    public Response getRoom(@PathParam("id") long id, @Context Request request) {
        final Room room = roomService.findRoomById(id)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));

        // El ETag del room depende solo del room; los agregados de reviews viven en GET /reviews?roomId=.
        return ConditionalCacheUtils.buildResponseUsingEtag(
                request,
                room.hashCode(),
                () -> Response.ok(new RoomOutputDTO(room, uriInfo))
        );
    }

    @GET
    @Path("/{id}/availabilities")
    @Produces(VndType.APPLICATION_ROOM_AVAILABILITY)
    public Response getRoomAvailability(@PathParam("id") long id,
                                        @QueryParam("startDate") String startDateStr,
                                        @QueryParam("endDate") String endDateStr,
                                        @Context Request request) {
        final RoomAvailabilityCalendar calendar = roomAvailabilityCalendarService.getRoomAvailabilityCalendar(
                id,
                LocalDate.now(),
                startDateStr,
                endDateStr
        );
        final RoomAvailabilityCalendarDTO calendarDTO = new RoomAvailabilityCalendarDTO(calendar, uriInfo);

        return ConditionalCacheUtils.buildResponseUsingEtag(
                request,
                calendar.hashCode(),
                () -> Response.ok(calendarDTO)
        );
    }

    @DELETE
    @Path("/{id}")
    @Produces(VndType.APPLICATION_ROOM_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isOwnerRoom(#id, authentication)")
    public Response deleteRoom(@PathParam("id") long id) {
        roomService.deleteRoom(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndType.APPLICATION_ROOM_DETAIL)
    @Produces(VndType.APPLICATION_ROOM_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isOwnerRoom(#id, authentication)")
    public Response updateRoom(@PathParam("id") long id, @Valid RoomUpdateDTO updateDto) {
        Room updatedRoom = roomService.updateRoom(
                id,
                updateDto.getTitle(),
                updateDto.getDescription(),
                updateDto.getAmenities(),
                updateDto.getDayPrice()
        );
        return Response.ok(new RoomOutputDTO(updatedRoom, uriInfo)).build();
    }
}
