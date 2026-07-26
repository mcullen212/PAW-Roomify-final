package ar.edu.itba.paw.webapp.controller.resource;

import ar.edu.itba.paw.interfaces.service.*;
import ar.edu.itba.paw.webapp.DTO.Input.RegisterUserDTO;
import ar.edu.itba.paw.webapp.DTO.Input.UserResetPasswordDto;
import ar.edu.itba.paw.webapp.DTO.Output.ProfileStatsDTO;
import ar.edu.itba.paw.webapp.DTO.Output.PublicUserDTO;
import ar.edu.itba.paw.webapp.DTO.Output.UserDTO;
import ar.edu.itba.paw.webapp.DTO.UserEmailDto;
import ar.edu.itba.paw.webapp.DTO.UserUpdateDto;
import ar.edu.itba.paw.webapp.mediaType.VndType;
import ar.edu.itba.paw.webapp.utils.ConditionalCacheUtils;
import ar.edu.itba.paw.model.User;
import java.net.URI;
import java.util.Locale;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;

@Path("users")
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;
    private final AuthService authService;

    @Context
    private UriInfo uriInfo;

    @Autowired
    public UserController(
        UserService userService,
        ProfileService profileService,
        AuthService authService
    ) {
        this.userService = userService;
        this.profileService = profileService;
        this.authService = authService;
    }

    @GET
    @Path("/{id}")
    @Produces(VndType.APPLICATION_USER_PROFILE)
    @PreAuthorize("@webAuthHelper.isAuthorized(#id, authentication)")
    public Response getProfile(@PathParam("id") long id, @Context Request request) {
        ProfileStatsDTO profileStatsDTO = new ProfileStatsDTO(profileService.getPrivateProfile(id), uriInfo);

        return ConditionalCacheUtils.buildPrivateResponseUsingEtag(
            request,
            profileStatsDTO.hashCode(),
            () -> Response.ok(profileStatsDTO)
        );
    }

    @GET
    @Path("/{id}")
    @Produces(VndType.APPLICATION_USER)
    public Response getUserById(@PathParam("id") long id, @Context Request request) {
        PublicUserDTO publicUserDTO = new PublicUserDTO(profileService.getPublicProfile(id), uriInfo);

        return ConditionalCacheUtils.buildResponseUsingEtag(
            request,
            publicUserDTO.hashCode(),
            () -> Response.ok(publicUserDTO)
        );
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndType.APPLICATION_USER)
    @Produces(VndType.APPLICATION_USER_PROFILE)
    @PreAuthorize("@webAuthHelper.isAuthorized(#id, authentication)")
    public Response updateUser(
            @PathParam("id") long id,
            @NotNull(message = "{request.body.required}") @Valid UserUpdateDto updateDto
    ) {
        User updatedUser = userService.updateUser(
                id,
                updateDto.getBio(),
                updateDto.getTravelPreferences(),
                updateDto.getLocale(),
                updateDto.getOldPassword(),
                updateDto.getNewPassword()
        );
        return Response.ok(new UserDTO(updatedUser, uriInfo)).build();
    }

    @POST
    @Consumes(VndType.APPLICATION_USER)
    @Produces(VndType.APPLICATION_USER)
    public Response registerUser(@Valid RegisterUserDTO registerDto) {
        Locale userLocale = (registerDto.getLocale() != null &&
            !registerDto.getLocale().trim().isEmpty())
            ? Locale.forLanguageTag(registerDto.getLocale())
            : Locale.ENGLISH;

        User user = userService.create(
            registerDto.getName(),
            registerDto.getEmail(),
            registerDto.getPassword(),
            userLocale
        );

        authService.sendVerificationEmail(registerDto.getEmail());

        final URI location = uriInfo
            .getAbsolutePathBuilder()
            .path(String.valueOf(user.getId()))
            .build();

        return Response.created(location)
            .build();
    }

    @POST
    @Consumes(VndType.APPLICATION_USER_PASSWORD_RESET)
    public Response requestPasswordResetOtp(@Valid UserEmailDto emailDto) {
        authService.requestPasswordReset(emailDto.getEmail());
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}")
    @Consumes(VndType.APPLICATION_USER_PASSWORD_RESET)
    @PreAuthorize("hasRole('RESET_PASSWORD_PRIVILEGE') and @webAuthHelper.isAuthorized(#id, authentication)")
    public Response resetPassword(
            @PathParam("id") long id,
            @Valid UserResetPasswordDto dto
    ) {
        User user = userService.findUserById(id).orElseThrow(NotFoundException::new);

        userService.resetPassword(user.getId(), dto.getNewPassword());

        return Response.noContent().build();
    }

}
