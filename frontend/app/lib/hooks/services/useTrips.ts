import type { AxiosInstance, AxiosResponse } from "axios";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import type { DateRange } from "react-day-picker";
import { createTripsAPI } from "~/lib/api/tripsAPI";
import { formatDateForApi } from "~/lib/datesUtils";
import { getPaginationFromLinkHeader } from "~/lib/pagination";
import type {
    GroupTripAssociationDTO,
    GroupTripDTO,
    PaginatedResponse,
    TripDTO,
    TripFilters,
    UpdateGroupTripPayload,
} from "~/lib/interfaces/trips";
import type { Contact } from "~/lib/interfaces/contacts";
import i18n from "~/i18n/i18n";

export interface CreateGroupTripInput {
    ownerId: number;
    title: string;
}

export interface CreateDestinationInput {
    country: string;
    dateRange: DateRange;
}

export const tripKeys = {
    all: ["trips"] as const,
    myGroupTrips: (userId?: number, page?: number, pageSize?: number, status?: string, filters?: TripFilters) =>
        ["trips", "groupTrips", "mine", userId, page, pageSize, status, filters?.country, filters?.checkIn, filters?.checkOut] as const,
    groupTripDetail: (groupTripId?: number) => ["trips", "groupTrips", groupTripId] as const,
    destinations: (groupTripId?: number, page?: number, pageSize?: number) =>
        ["trips", "groupTrips", groupTripId, "destinations", page, pageSize] as const,
    destinationContacts: (groupTripId?: number, tripId?: number, page?: number, pageSize?: number) =>
        ["trips", "groupTrips", groupTripId, "destinations", tripId, "contacts", page, pageSize] as const,
    association: (userId?: number, page?: number, pageSize?: number, filters?: TripFilters) =>
        ["trips", "association", userId, page, pageSize, filters?.country, filters?.checkIn, filters?.checkOut] as const,
};

export function useTrips(api: AxiosInstance) {
    const tripsApi = createTripsAPI(api);

    const toPaginatedResponse = <T,>(response: AxiosResponse<T[]>, page: number): PaginatedResponse<T> => ({
        data: Array.isArray(response.data) ? response.data : [],
        pagination: getPaginationFromLinkHeader(response.headers?.link, page),
    });

    const toApiDateRange = (dateRange: DateRange) => {
        if (!dateRange.from || !dateRange.to) {
            throw new Error(i18n.t("tripDetails.errors.dateRangeRequired"));
        }

        return {
            startDate: formatDateForApi(dateRange.from),
            endDate: formatDateForApi(dateRange.to),
        };
    };

    function useGetMyGroupTrips(userId?: number, page = 1, pageSize = 10, status?: string, filters: TripFilters = {}) {
        return useQuery({
            queryKey: tripKeys.myGroupTrips(userId, page, pageSize, status, filters),
            queryFn: async () => {
                const response = await tripsApi.getMyGroupTrips(userId!, page, pageSize, status, filters);
                return toPaginatedResponse<GroupTripDTO>(response, page);
            },
            enabled: !!userId,
        });
    }

    function useGetGroupTrip(groupTripId?: number) {
        return useQuery({
            queryKey: tripKeys.groupTripDetail(groupTripId),
            queryFn: () => tripsApi.getGroupTrip(groupTripId!),
            enabled: !!groupTripId,
        });
    }

    function useGetDestinations(groupTripId?: number, page = 1, pageSize = 10) {
        return useQuery({
            queryKey: tripKeys.destinations(groupTripId, page, pageSize),
            queryFn: async () => {
                const response = await tripsApi.getDestinations(groupTripId!, page, pageSize);
                return toPaginatedResponse<TripDTO>(response, page);
            },
            enabled: !!groupTripId,
        });
    }

    function useGetDestinationsList(groupTripIds: (number | undefined)[], page = 1, pageSize = 10) {
        return useQueries({
            queries: groupTripIds.map((groupTripId) => ({
                queryKey: tripKeys.destinations(groupTripId, page, pageSize),
                queryFn: async () => {
                    const response = await tripsApi.getDestinations(groupTripId!, page, pageSize);
                    return toPaginatedResponse<TripDTO>(response, page);
                },
                enabled: !!groupTripId,
            })),
        });
    }

    function useGetDestinationContacts(groupTripId?: number, tripId?: number, page = 1, pageSize = 10) {
        return useQuery({
            queryKey: tripKeys.destinationContacts(groupTripId, tripId, page, pageSize),
            queryFn: async () => {
                const response = await tripsApi.getDestinationContacts(groupTripId!, tripId!, page, pageSize);
                return toPaginatedResponse<Contact>(response, page);
            },
            enabled: !!groupTripId && !!tripId,
        });
    }

    function useGetDestinationContactsList(
        groupTripId: number | undefined,
        tripIds: (number | undefined)[],
        page = 1,
        pageSize = 10,
    ) {
        return useQueries({
            queries: tripIds.map((tripId) => ({
                queryKey: tripKeys.destinationContacts(groupTripId, tripId, page, pageSize),
                queryFn: async () => {
                    const response = await tripsApi.getDestinationContacts(groupTripId!, tripId!, page, pageSize);
                    return toPaginatedResponse<Contact>(response, page);
                },
                enabled: !!groupTripId && !!tripId,
            })),
        });
    }

    function useGetGroupTripsForAssociation(
        userId?: number,
        page = 1,
        pageSize = 10,
        filters: TripFilters = {},
        enabled = true,
    ) {
        return useQuery({
            queryKey: tripKeys.association(userId, page, pageSize, filters),
            queryFn: async () => {
                const response = await tripsApi.getGroupTripsForAssociation(userId!, page, pageSize, filters);
                return toPaginatedResponse<GroupTripAssociationDTO>(response, page);
            },
            enabled: enabled && !!userId,
        });
    }

    function useFindGroupTripsForAssociation() {
        const queryClient = useQueryClient();

        return ({
            userId,
            page = 1,
            pageSize = 10,
            filters = {},
        }: {
            userId: number;
            page?: number;
            pageSize?: number;
            filters?: TripFilters;
        }) => queryClient.fetchQuery({
            queryKey: tripKeys.association(userId, page, pageSize, filters),
            queryFn: async () => {
                const response = await tripsApi.getGroupTripsForAssociation(userId, page, pageSize, filters);
                return toPaginatedResponse<GroupTripAssociationDTO>(response, page);
            },
        });
    }

    function useCreateGroupTrip() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: (payload: CreateGroupTripInput) => tripsApi.createGroupTrip({
                ownerId: payload.ownerId,
                title: payload.title,
            }),
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: tripKeys.all });
            },
        });
    }

    function useUpdateGroupTrip() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: ({
                groupTripId,
                payload,
            }: {
                groupTripId: number;
                payload: UpdateGroupTripPayload;
            }) => tripsApi.updateGroupTrip(groupTripId, payload),
            onSuccess: (_, variables) => {
                queryClient.invalidateQueries({ queryKey: tripKeys.all });
                queryClient.invalidateQueries({ queryKey: tripKeys.groupTripDetail(variables.groupTripId) });
            },
        });
    }

    function useCompletePlanning() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: (groupTripId: number) => tripsApi.completePlanning(groupTripId),
            onSuccess: (_, groupTripId) => {
                queryClient.invalidateQueries({ queryKey: tripKeys.all });
                queryClient.invalidateQueries({ queryKey: tripKeys.groupTripDetail(groupTripId) });
            },
        });
    }

    function useCreateDestination() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: ({
                groupTripId,
                payload,
            }: {
                groupTripId: number;
                payload: CreateDestinationInput;
            }) => tripsApi.createDestination(groupTripId, {
                country: payload.country,
                ...toApiDateRange(payload.dateRange),
            }),
            onSuccess: (_, variables) => {
                queryClient.invalidateQueries({ queryKey: tripKeys.all });
                queryClient.invalidateQueries({ queryKey: tripKeys.destinations(variables.groupTripId) });
                queryClient.invalidateQueries({ queryKey: tripKeys.groupTripDetail(variables.groupTripId) });
            },
        });
    }

    return {
        useGetMyGroupTrips,
        useGetGroupTrip,
        useGetDestinations,
        useGetDestinationsList,
        useGetDestinationContacts,
        useGetDestinationContactsList,
        useGetGroupTripsForAssociation,
        useFindGroupTripsForAssociation,
        useCreateGroupTrip,
        useUpdateGroupTrip,
        useCompletePlanning,
        useCreateDestination,
    };
}
