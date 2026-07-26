package ar.edu.itba.paw.webapp.mediaType;

public class VndType {

    public VndType() {
        throw new AssertionError();
    }

    public static final String APPLICATION_API =
            "application/vnd.roomify.api.v1+json";

    public static final String APPLICATION_USER =
        "application/vnd.roomify.user.v1+json";
    public static final String APPLICATION_USER_PROFILE =
        "application/vnd.roomify.user.profile.v1+json";
    public static final String APPLICATION_USER_PASSWORD_RESET =
        "application/vnd.roomify.user.password-reset.v1+json";

    public static final String APPLICATION_ROOMS =
        "application/vnd.roomify.room.v1.list+json";
    public static final String APPLICATION_ROOM_DETAIL =
        "application/vnd.roomify.room.v1+json";
    public static final String APPLICATION_ROOM_FORM =
        "application/vnd.roomify.room.form.v1+json";
    public static final String APPLICATION_ROOM_AVAILABILITY =
        "application/vnd.roomify.room.availability.v1+json";

    public static final String APPLICATION_CONTACT =
        "application/vnd.roomify.contact.v1.list+json";
    public static final String APPLICATION_CONTACT_DETAIL =
        "application/vnd.roomify.contact.v1+json";

    public static final String APPLICATION_REVIEWS =
        "application/vnd.roomify.reviews.v1.list+json";
    public static final String APPLICATION_REVIEW_DETAIL =
        "application/vnd.roomify.review.v1+json";

    public static final String APPLICATION_IMAGE =
        "application/vnd.roomify.image.v1+json";

    public static final String APPLICATION_COUNTRIES =
        "application/vnd.roomify.country.v1.list+json";

    public static final String APPLICATION_GROUP_TRIP =
        "application/vnd.roomify.group-trip.v1.list+json";
    public static final String APPLICATION_GROUP_TRIP_DETAIL =
        "application/vnd.roomify.group-trip.v1+json";
    public static final String APPLICATION_GROUP_TRIP_DESTINATION =
        "application/vnd.roomify.group-trip.trip.v1.list+json";
    public static final String APPLICATION_GROUP_TRIP_DESTINATION_DETAIL =
            "application/vnd.roomify.group-trip.trip.v1+json";
}
