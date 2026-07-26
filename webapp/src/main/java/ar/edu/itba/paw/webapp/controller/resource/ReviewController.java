package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.service.ReviewService;
import ar.edu.itba.paw.model.DTO.ReviewPageDTO;
import ar.edu.itba.paw.model.rooms.Review;
import ar.edu.itba.paw.webapp.DTO.Input.ReviewInputDTO;
import ar.edu.itba.paw.webapp.DTO.Output.ReviewDTO;
import ar.edu.itba.paw.webapp.DTO.Output.ReviewsDTO;
import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.ConditionalCacheUtils;
import ar.edu.itba.paw.webapp.utils.PaginationUtils;
import ar.edu.itba.paw.interfaces.exceptions.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Path("reviews")
@Component
public class ReviewController {

    private final ReviewService reviewService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GET
    @Produces(VndType.APPLICATION_REVIEWS)
    public Response getReviews(@QueryParam("roomId") Long roomId,
                             @QueryParam("userId") Long userId,
                             @QueryParam("roomOwnerId") Long roomOwnerId,
                             @QueryParam("page") @DefaultValue("1") int page,
                             @QueryParam("pageSize") @DefaultValue("12") int pageSize,
                             @Context Request request) {
        final ReviewPageDTO reviewPage = reviewService.getReviewsPage(roomId, userId, roomOwnerId, page, pageSize);

        final List<ReviewDTO> reviewDTOs = reviewPage.getReviews().stream()
                .map(r -> new ReviewDTO(r, uriInfo))
                .collect(Collectors.toList());

        final ReviewsDTO reviewsDTO = new ReviewsDTO(
                reviewDTOs,
                reviewPage.getTotalReviews(),
                reviewPage.getAverageRating()
        );

        return ConditionalCacheUtils.buildResponseUsingEtag(
                request,
                reviewsDTO.hashCode(),
                () -> {
                    Response.ResponseBuilder response = Response.ok(reviewsDTO);
                    PaginationUtils.addLinks(response, uriInfo, page, reviewPage.getTotalPages());
                    return response;
                }
        );
    }

    @GET
    @Path("/{id}")
    @Produces(VndType.APPLICATION_REVIEW_DETAIL)
    public Response getReview(@PathParam("id") long id, @Context Request request) {
        Review review = reviewService.findReviewById(id);
        if (review == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final ReviewDTO reviewDTO = new ReviewDTO(review, uriInfo);
        return ConditionalCacheUtils.buildResponseUsingEtag(
                request,
                reviewDTO.hashCode(),
                () -> Response.ok(reviewDTO)
                        .link(uriInfo.getAbsolutePath(), "self")
        );
    }

    @POST
    @Consumes(VndType.APPLICATION_REVIEW_DETAIL)
    @PreAuthorize("isAuthenticated() and hasAuthority('ROLE_VERIFIED_USER') and @webAuthHelper.isAuthorized(#reviewDto.reviewerId, authentication) and @webAuthHelper.belongsToSwap(#reviewDto.contactId, authentication)")
    public Response createReview(@Valid ReviewInputDTO reviewDto, @Context HttpHeaders request) throws BusinessException {
        Locale locale = request.getAcceptableLanguages().stream().findFirst().orElse(Locale.getDefault());

        Review review = reviewService.addReview(reviewDto.getContactId(),
                reviewDto.getReviewerId(),
                reviewDto.getRating(),
                reviewDto.getComment(),
                locale);

        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(review.getId())).build();
        return Response.created(location).build();
    }

    @DELETE
    @Path("/{id}")
    @PreAuthorize("@webAuthHelper.isOwnerReview(#id, authentication)")
    public Response deleteReview(@PathParam("id") long id) {
        reviewService.deleteReview(id);
        return Response.noContent().build();
    }
}
