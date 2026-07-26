import { useContacts } from "~/lib/hooks/services/useContacts";
import { useReviews } from "~/lib/hooks/services/useReviews";
import { useRooms } from "~/lib/hooks/services/useRooms";
import { useTrips } from "~/lib/hooks/services/useTrips";
import { useUsers } from "~/lib/hooks/services/useUsers";
import { useApi } from "~/lib/hooks/useApi";

export function useApiServices() {
    const api = useApi();
    const userService = useUsers(api);
    const roomService = useRooms(api);
    const reviewService = useReviews(api);
    const contactService = useContacts(api);
    const tripService = useTrips(api);

    return {
        api,
        userService,
        roomService,
        reviewService,
        contactService,
        tripService,
    };
}
