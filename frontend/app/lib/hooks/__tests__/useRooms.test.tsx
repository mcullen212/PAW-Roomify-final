import type { ReactNode } from "react";
import type { AxiosInstance } from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { roomKeys, useRooms } from "../services/useRooms.ts";
import { VndType } from "~/lib/api/vndTypes.ts";
import type { RoomCreatePayload, RoomUpdatePayload } from "~/lib/interfaces/rooms";

vi.mock("~/lib/auth/useAuth", () => ({
    useAuth: () => ({ userId: 7 }),
}));

function setup() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });
    const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    return { queryClient, wrapper };
}

const createPayload: RoomCreatePayload = {
    title: "Bright room",
    country: "Argentina",
    city: "Buenos Aires",
    description: "A bright room near the park",
    roomType: "PRIVATE",
    bedType: "DOUBLE",
    privateBathroom: true,
    privateKitchen: false,
    amenities: ["WIFI"],
    dateRange: [{ startDate: "2026-08-01", endDate: "2026-08-05" }],
    dayPrice: 50,
    imageId: 7,
};

describe("useRooms", () => {
    it("gets filtered rooms with the list media type", async () => {
        const get = vi.fn().mockResolvedValue({
            data: [{ id: 1 }],
            headers: {
                link: '</rooms?page=3&destination=Buenos%20Aires>; rel="next", </rooms?page=5&destination=Buenos%20Aires>; rel="last"',
            },
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();
        const filters = { page: 2, destination: "Buenos Aires", roomType: "PRIVATE" };

        const { result } = renderHook(() => useRooms(api).useGetRooms(filters), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [{ id: 1 }],
            pagination: {
                currentPage: 2,
                totalPages: 5,
                links: {
                    first: "",
                    prev: "",
                    next: "/rooms?page=3&destination=Buenos%20Aires",
                    last: "/rooms?page=5&destination=Buenos%20Aires",
                },
            },
        });
        expect(get).toHaveBeenCalledWith("/rooms", {
            headers: { Accept: "application/vnd.roomify.room.v1.list+json" },
            params: filters,
        });
    });

    it("gets a user's rooms with a distinct cache key", async () => {
        const get = vi.fn().mockResolvedValue({ data: [{ id: 2 }], headers: {} });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useRooms(api).useGetMyRooms(9, 3, 25), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [{ id: 2 }],
            pagination: {
                currentPage: 3,
                totalPages: 3,
                links: {
                    first: "",
                    prev: "",
                    next: "",
                    last: "",
                },
            },
        });
        expect(get).toHaveBeenCalledWith("/rooms", {
            headers: { Accept: "application/vnd.roomify.room.v1.list+json" },
            params: { userId: 9, page: 3, pageSize: 25 },
        });
        expect(roomKeys.owner(9, 3, 25)).toEqual(["rooms", "owner", 9, 3, 25]);
    });

    it("does not request a user's rooms without a user id", () => {
        const get = vi.fn();
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useRooms(api).useGetMyRooms(), { wrapper });

        expect(result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("gets a room detail and disables the query without an id", async () => {
        const room = {
            id: 4,
            title: "Bright room",
        };
        const get = vi.fn().mockResolvedValue({ data: room });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const detail = renderHook(() => useRooms(api).useGetRoomById(4), { wrapper });
        await waitFor(() => expect(detail.result.current.isSuccess).toBe(true));
        expect(detail.result.current.data).toEqual(room);
        expect(get).toHaveBeenCalledWith("/rooms/4", {
            headers: { Accept: "application/vnd.roomify.room.v1+json" },
        });

        detail.unmount();
        get.mockClear();
        const disabled = renderHook(() => useRooms(api).useGetRoomById(), { wrapper });
        expect(disabled.result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("gets the room's availability calendar and disables queries without a room id", async () => {
        const calendar = {
            roomId: 4,
            availabilityRanges: [{ startDate: "2026-08-01", endDate: "2026-08-31" }],
            bookedRanges: [{ startDate: "2026-08-10", endDate: "2026-08-12" }],
            selectableRanges: [{ startDate: "2026-08-01", endDate: "2026-08-09" }],
            firstSelectableDate: "2026-08-01",
            hasSelectableStay: true,
        };
        const get = vi.fn().mockResolvedValue({ data: calendar });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const availability = renderHook(
            () => useRooms(api).useGetRoomAvailability(4),
            { wrapper },
        );
        await waitFor(() => expect(availability.result.current.isSuccess).toBe(true));
        expect(availability.result.current.data).toEqual(calendar);
        expect(get).toHaveBeenCalledWith("/rooms/4/availabilities", {
            headers: { Accept: VndType.APPLICATION_ROOM_AVAILABILITY },
        });
        expect(roomKeys.availability(4)).toEqual(["rooms", 4, "availability", undefined, undefined]);

        availability.unmount();
        get.mockClear();
        const disabled = renderHook(
            () => useRooms(api).useGetRoomAvailability(),
            { wrapper },
        );
        expect(disabled.result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("gets the room's availability calendar for a searched date range", async () => {
        const calendar = {
            roomId: 4,
            availabilityRanges: [{ startDate: "2026-08-01", endDate: "2026-08-05" }],
            bookedRanges: [],
            selectableRanges: [{ startDate: "2026-08-01", endDate: "2026-08-05" }],
            firstSelectableDate: "2026-08-01",
            hasSelectableStay: true,
        };
        const get = vi.fn().mockResolvedValue({ data: calendar });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const availability = renderHook(
            () => useRooms(api).useGetRoomAvailability(4, {
                startDate: "2026-08-01",
                endDate: "2026-08-05",
            }),
            { wrapper },
        );

        await waitFor(() => expect(availability.result.current.isSuccess).toBe(true));
        expect(get).toHaveBeenCalledWith("/rooms/4/availabilities", {
            headers: { Accept: VndType.APPLICATION_ROOM_AVAILABILITY },
            params: {
                startDate: "2026-08-01",
                endDate: "2026-08-05",
            },
        });
        expect(roomKeys.availability(4, {
            startDate: "2026-08-01",
            endDate: "2026-08-05",
        })).toEqual(["rooms", 4, "availability", "2026-08-01", "2026-08-05"]);

        availability.unmount();
    });

    it("uploads an image with the injected client and preserves the response", async () => {
        const response = { data: undefined, headers: { location: "/images/7" } };
        const post = vi.fn().mockResolvedValue(response);
        const api = { post } as unknown as AxiosInstance;
        const { wrapper } = setup();
        const image = new File(["image"], "room.png", { type: "image/png" });

        const { result } = renderHook(() => useRooms(api).useCreateImage(), { wrapper });
        const returned = await result.current.mutateAsync(image);

        expect(returned).toBe(response);
        expect(post).toHaveBeenCalledOnce();
        const [url, body, config] = post.mock.calls[0];
        expect(url).toBe("/images");
        expect(body).toBeInstanceOf(FormData);
        expect(body.get("image")).toBe(image);
        expect(config).toEqual({ headers: { Accept: VndType.APPLICATION_IMAGE } });
    });

    it("creates a room, preserves response headers, and invalidates room queries", async () => {
        const response = { data: { id: 1 }, headers: { location: "/rooms/1" } };
        const post = vi.fn().mockResolvedValue(response);
        const api = { post } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

        const { result } = renderHook(() => useRooms(api).useCreateRoom(), { wrapper });
        const returned = await result.current.mutateAsync(createPayload);

        expect(returned).toBe(response);
        expect(post).toHaveBeenCalledWith("/rooms", { ...createPayload, userId: 7 }, {
            headers: {
                Accept: VndType.APPLICATION_ROOM_DETAIL,
                "Content-Type": VndType.APPLICATION_ROOM_DETAIL,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roomKeys.all });
    });

    it("updates a room and invalidates general and detail queries", async () => {
        const patch = vi.fn().mockResolvedValue({ data: { id: 4 } });
        const api = { patch } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const roomData: RoomUpdatePayload = {
            title: "Updated room",
            description: "Updated description",
            amenities: [],
            dayPrice: 75,
        };

        const { result } = renderHook(() => useRooms(api).useUpdateRoom(), { wrapper });
        await result.current.mutateAsync({ roomId: 4, roomData });

        expect(patch).toHaveBeenCalledWith("/rooms/4", roomData, {
            headers: {
                Accept: VndType.APPLICATION_ROOM_DETAIL,
                "Content-Type": VndType.APPLICATION_ROOM_DETAIL,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roomKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roomKeys.detail(4) });
    });

    it("deletes a room and invalidates general and detail queries", async () => {
        const remove = vi.fn().mockResolvedValue({});
        const api = { delete: remove } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

        const { result } = renderHook(() => useRooms(api).useDeleteRoom(), { wrapper });
        await result.current.mutateAsync(4);

        expect(remove).toHaveBeenCalledWith("/rooms/4", {
            headers: { Accept: "application/vnd.roomify.room.v1+json" },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roomKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roomKeys.detail(4) });
    });
});
