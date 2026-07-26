import type { AxiosInstance } from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createRoomAPI } from "~/lib/api/roomAPI";
import type { RoomAvailabilityQuery, RoomCreatePayload, RoomUpdatePayload } from "~/lib/interfaces/rooms";
import { getPaginationFromLinkHeader } from "~/lib/pagination";
import { useAuth } from "~/lib/auth/useAuth";
import i18n from "~/i18n/i18n";

export const roomKeys = {
    all: ["rooms"] as const,
    list: (filters?: unknown, page?: number, pageSize?: number) =>
        ["rooms", filters, page, pageSize] as const,
    owner: (userId?: number, page?: number, pageSize?: number) =>
        ["rooms", "owner", userId, page, pageSize] as const,
    detail: (roomId?: number) => ["rooms", roomId] as const,
    availability: (roomId?: number, query?: RoomAvailabilityQuery) =>
        ["rooms", roomId, "availability", query?.startDate, query?.endDate] as const,
};

export function useRooms(api: AxiosInstance) {
    const roomApi = createRoomAPI(api);

    function useGetRooms(filters?: unknown, page?: number, pageSize?: number) {
        const filterRecord = filters && typeof filters === "object" ? filters as Record<string, unknown> : {};
        const filterPage = typeof filterRecord.page === "number" ? filterRecord.page : undefined;
        const filterPageSize = typeof filterRecord.pageSize === "number" ? filterRecord.pageSize : undefined;
        const currentPage = page ?? filterPage ?? 1;
        const currentPageSize = pageSize ?? filterPageSize;

        return useQuery({
            queryKey: roomKeys.list(filters, currentPage, currentPageSize),
            queryFn: async () => {
                const response = await roomApi.getRooms({
                    ...filterRecord,
                    page: currentPage,
                    pageSize: currentPageSize,
                });
                return {
                    data: response.data,
                    pagination: getPaginationFromLinkHeader(response.headers?.link, currentPage),
                };
            },
        });
    }

    function useGetRoomById(roomId?: number) {
        return useQuery({
            queryKey: roomKeys.detail(roomId),
            queryFn: async () => {
                const response = await roomApi.getRoomById(roomId!);
                return response.data;
            },
            enabled: !!roomId,
        });
    }

    function useGetMyRooms(userId?: number, page = 1, pageSize = 100) {
        return useQuery({
            queryKey: roomKeys.owner(userId, page, pageSize),
            queryFn: async () => {
                const response = await roomApi.getMyRooms(userId!, page, pageSize);
                return {
                    data: response.data,
                    pagination: getPaginationFromLinkHeader(response.headers?.link, page),
                };
            },
            enabled: !!userId,
        });
    }

    function useGetRoomAvailability(roomId?: number, query?: RoomAvailabilityQuery) {
        return useQuery({
            queryKey: roomKeys.availability(roomId, query),
            queryFn: async () => {
                const response = await roomApi.getRoomAvailability(roomId!, query);
                return response.data;
            },
            enabled: !!roomId,
        });
    }

    function useCreateRoom() {
        const queryClient = useQueryClient();
        const { userId } = useAuth();

        return useMutation({
            mutationFn: (roomData: RoomCreatePayload) => {
                if (!userId) {
                    throw new Error(i18n.t("postRoom.errors.signInRequired"));
                }

                return roomApi.createRoom({
                    ...roomData,
                    userId,
                });
            },
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: roomKeys.all });
            },
        });
    }

    function useCreateImage() {
        return useMutation({
            mutationFn: (image: File) => roomApi.createImage(image),
        });
    }

    function useUpdateRoom() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: async ({
                roomId,
                roomData,
            }: {
                roomId: number;
                roomData: RoomUpdatePayload;
            }) => {
                const response = await roomApi.updateRoom(roomId, roomData);
                return response.data;
            },
            onSuccess: (_, variables) => {
                queryClient.invalidateQueries({ queryKey: roomKeys.all });
                queryClient.invalidateQueries({ queryKey: roomKeys.detail(variables.roomId) });
            },
        });
    }

    function useDeleteRoom() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: (roomId: number) => roomApi.deleteRoom(roomId),
            onSuccess: (_, roomId) => {
                queryClient.invalidateQueries({ queryKey: roomKeys.all });
                queryClient.invalidateQueries({ queryKey: roomKeys.detail(roomId) });
            },
        });
    }

    return {
        useGetRooms,
        useGetMyRooms,
        useGetRoomById,
        useGetRoomAvailability,
        useCreateImage,
        useCreateRoom,
        useUpdateRoom,
        useDeleteRoom,
    };
}
