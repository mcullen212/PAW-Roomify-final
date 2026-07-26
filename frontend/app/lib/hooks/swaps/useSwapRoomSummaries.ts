import type { AxiosInstance } from "axios";
import { useQuery } from "@tanstack/react-query";
import type { Contact } from "~/lib/interfaces/contacts";
import { VndType } from "~/lib/api/vndTypes";
import type { RoomSummary } from "~/lib/interfaces/swaps";
import {
    createFallbackRoomSummary,
    getSwapRoomIds,
    mapRoomSummary,
} from "~/lib/swaps/swaps-utils";

export const swapRoomSummaryKeys = {
    all: ["swap-room-summaries"] as const,
    list: (roomIds: number[]) => [...swapRoomSummaryKeys.all, roomIds] as const,
};

export function useSwapRoomSummaries(
    api: AxiosInstance,
    contacts: Contact[],
    enabled = true,
) {
    const roomIds = getSwapRoomIds(contacts);

    return useQuery({
        queryKey: swapRoomSummaryKeys.list(roomIds),
        queryFn: async () => {
            if (roomIds.length === 0) {
                return {};
            }

            const roomEntries = await Promise.all(
                roomIds.map(async (roomId) => {
                    try {
                        const [response, availabilityResponse] = await Promise.all([
                            api.get(`/rooms/${roomId}`, {
                                headers: {
                                    Accept: VndType.APPLICATION_ROOM_DETAIL,
                                },
                            }),
                            api.get(`/rooms/${roomId}/availabilities`, {
                                headers: {
                                    Accept: VndType.APPLICATION_ROOM_AVAILABILITY,
                                },
                            }).catch(() => null),
                        ]);

                        return [roomId, mapRoomSummary(roomId, response.data, availabilityResponse?.data ?? null)] as const;
                    } catch (roomError) {
                        console.error("Failed to load room summary:", roomError);
                        return [roomId, createFallbackRoomSummary(roomId)] as const;
                    }
                }),
            );

            return Object.fromEntries(roomEntries) as Record<number, RoomSummary>;
        },
        enabled,
    });
}
