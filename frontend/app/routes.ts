import { type RouteConfig, index, layout, route } from "@react-router/dev/routes"

export default [
    index("routes/home.tsx"),
    route("/room/:id", "routes/Rooms/room-details.tsx"),
    route("/users/:id", "routes/users/public-profile.tsx"),
    route("/login", "routes/Login/login.tsx"),
    route("/signUp", "routes/Login/signup.tsx"),
    route("/login/forgot-password", "routes/Login/forgot-password.tsx"),
    route("/verify-token", "routes/users/verify-token.tsx"),
    layout("lib/auth/ProtectedRoute.tsx", [
        route("/profile", "routes/users/profile.tsx"),
        route("/profile/reviews", "routes/users/profile-reviews.tsx"),
        route("/login/reset-password", "routes/users/reset-password.tsx"),
        layout("lib/auth/VerifiedRoomsRoute.tsx", [
            route("/host", "routes/Rooms/post-room.tsx"),
            route("/my-rooms", "routes/Rooms/my-rooms.tsx"),
            route("/room/:id/edit", "routes/Rooms/edit-room.tsx"),
        ]),
        layout("lib/auth/VerifiedSwapsRoute.tsx", [
            route("/swaps", "routes/Swaps/swaps.tsx"),
        ]),
        layout("lib/auth/VerifiedTripsRoute.tsx", [
            route("/trips", "routes/Trips/trips.tsx"),
            route("/trips/:id", "routes/Trips/trip-details.tsx"),
            route("/trips/:id/contacts", "routes/Trips/trip-destination-contacts.tsx"),
            route("/trips/plan", "routes/Trips/plan-trip.tsx"),
        ]),
    ]),
    route("/400", "routes/errors/bad-request.tsx"),
    route("/403", "routes/errors/forbidden-page.tsx"),
    route("/500", "routes/errors/internal-error-page.tsx"),
    route("*", "routes/errors/not-found-page.tsx"),
] satisfies RouteConfig
