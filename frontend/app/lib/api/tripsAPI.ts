import api from "./api";
import type { AxiosInstance } from "axios";
import { VndType } from "./vndTypes";
import type { Contact } from "~/lib/interfaces/contacts";
import type {
    CreateGroupTripPayload,
    CreateTripPayload,
    GroupTripAssociationDTO,
    GroupTripDTO,
    TripDTO,
    TripFilters,
    UpdateGroupTripPayload,
} from "~/lib/interfaces/trips";

export type {
    CreateGroupTripPayload,
    CreateTripPayload,
    DestinationContactsResponse,
    GroupTripAssociationDTO,
    GroupTripDTO,
    PaginatedResponse,
    ResourceLinks,
    TripDTO,
    TripFilters,
    TripMatchDecision,
    TripMatchResult,
    UpdateGroupTripPayload,
} from "~/lib/interfaces/trips";

export const createTripsAPI = (client: AxiosInstance) => {
    const getMyGroupTrips = (
        userId: number,
        page: number,
        pageSize: number,
        status?: string,
        filters: TripFilters = {}
    ) => {
        return client.get<GroupTripDTO[]>("/group-trips", {
            params: {
                userId,
                page,
                pageSize,
                ...(status ? { status } : {}),
                ...(filters.country ? { country: filters.country } : {}),
                ...(filters.checkIn ? { checkIn: filters.checkIn } : {}),
                ...(filters.checkOut ? { checkOut: filters.checkOut } : {}),
            },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP,
            },
        });
    };

    const createGroupTrip = (payload: CreateGroupTripPayload) => {
        return client.post("/group-trips", payload, {
            headers: {
                "Content-Type": VndType.APPLICATION_GROUP_TRIP_DETAIL,
            },
        });
    };

    const getGroupTrip = async (id: number): Promise<GroupTripDTO> => {
        const response = await client.get(`/group-trips/${id}`, {
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP_DETAIL,
            },
        });

        return response.data as GroupTripDTO;
    };

    const updateGroupTrip = async (
        id: number,
        payload: UpdateGroupTripPayload
    ): Promise<GroupTripDTO> => {
        const response = await client.patch(`/group-trips/${id}`, payload, {
            headers: {
                "Content-Type": VndType.APPLICATION_GROUP_TRIP_DETAIL,
                Accept: VndType.APPLICATION_GROUP_TRIP_DETAIL,
            },
        });

        return response.data as GroupTripDTO;
    };

    const completePlanning = (id: number): Promise<GroupTripDTO> => {
        return updateGroupTrip(id, { status: "UPCOMING" });
    };

    const getDestinations = (
        groupTripId: number,
        page: number,
        pageSize: number
    ) => {
        return client.get<TripDTO[]>(`/group-trips/${groupTripId}/trips`, {
            params: { page, pageSize },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP_DESTINATION,
            },
        });
    };

    const createDestination = (groupTripId: number, payload: CreateTripPayload) => {
        return client.post(`/group-trips/${groupTripId}/trips`, payload, {
            headers: {
                "Content-Type": VndType.APPLICATION_GROUP_TRIP_DESTINATION,
            },
        });
    };

    const getDestinationContacts = (
        groupTripId: number,
        tripId: number,
        page: number,
        pageSize: number
    ) => {
        return client.get<Contact[]>(`/group-trips/${groupTripId}/trips/${tripId}`, {
            params: { page, pageSize },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP_DESTINATION_DETAIL,
            },
        });
    };

    const getGroupTripsForAssociation = (
        userId: number,
        page: number,
        pageSize: number,
        filters: TripFilters = {}
    ) => {
        return client.get<GroupTripAssociationDTO[]>("/group-trips", {
            params: {
                userId,
                page,
                pageSize,
                ...(filters.country ? { country: filters.country } : {}),
                ...(filters.checkIn ? { checkIn: filters.checkIn } : {}),
                ...(filters.checkOut ? { checkOut: filters.checkOut } : {}),
            },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP,
            },
        });
    };

    return {
        getMyGroupTrips,
        createGroupTrip,
        getGroupTrip,
        updateGroupTrip,
        completePlanning,
        getDestinations,
        createDestination,
        getDestinationContacts,
        getGroupTripsForAssociation,
    };
};

const tripsAPI = createTripsAPI(api);
export default tripsAPI;
