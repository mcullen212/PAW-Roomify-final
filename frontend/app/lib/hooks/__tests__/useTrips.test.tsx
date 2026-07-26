import type { ReactNode } from "react";
import type { AxiosInstance } from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VndType } from "~/lib/api/vndTypes.ts";
import { tripKeys, useTrips } from "../services/useTrips.ts";

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

describe("useTrips", () => {
    it("gets paginated group trips with status, filters, and parsed pagination", async () => {
        const groupTrip = { id: 4, title: "Europe", status: "PLANNING" };
        const get = vi.fn().mockResolvedValue({
            data: [groupTrip],
            headers: {
                link: '</group-trips?page=2&pageSize=6>; rel="next", </group-trips?page=3&pageSize=6>; rel="last"',
            },
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(
            () => useTrips(api).useGetMyGroupTrips(7, 1, 6, "PLANNING", {
                country: "Germany",
                checkIn: "2026-08-01",
                checkOut: "2026-08-05",
            }),
            { wrapper },
        );

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [groupTrip],
            pagination: {
                currentPage: 1,
                totalPages: 3,
                links: {
                    first: "",
                    prev: "",
                    next: "/group-trips?page=2&pageSize=6",
                    last: "/group-trips?page=3&pageSize=6",
                },
            },
        });
        expect(get).toHaveBeenCalledWith("/group-trips", {
            params: {
                userId: 7,
                page: 1,
                pageSize: 6,
                status: "PLANNING",
                country: "Germany",
                checkIn: "2026-08-01",
                checkOut: "2026-08-05",
            },
            headers: { Accept: VndType.APPLICATION_GROUP_TRIP },
        });
        expect(tripKeys.myGroupTrips(7, 1, 6, "PLANNING")).not.toEqual(
            tripKeys.myGroupTrips(7, 1, 6, "DONE"),
        );
    });

    it("does not request group trip detail without an id", () => {
        const get = vi.fn();
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useTrips(api).useGetGroupTrip(), { wrapper });

        expect(result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("gets group trip detail with the detail media type", async () => {
        const groupTrip = { id: 4, title: "Europe", status: "PLANNING" };
        const get = vi.fn().mockResolvedValue({ data: groupTrip });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useTrips(api).useGetGroupTrip(4), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual(groupTrip);
        expect(get).toHaveBeenCalledWith("/group-trips/4", {
            headers: { Accept: VndType.APPLICATION_GROUP_TRIP_DETAIL },
        });
    });

    it("gets destinations and destination contacts with distinct keys", async () => {
        const get = vi
            .fn()
            .mockResolvedValueOnce({ data: [{ id: 9, country: "Germany" }], headers: {} })
            .mockResolvedValueOnce({
                data: {
                    id: 9,
                    _links: {
                        contacts: "/contacts?tripId=9&page=1&pageSize=6",
                    },
                },
                headers: {},
            })
            .mockResolvedValueOnce({ data: [{ id: 12, status: "ACCEPTED" }], headers: {} });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const destinations = renderHook(() => useTrips(api).useGetDestinations(4, 2, 6), { wrapper });
        await waitFor(() => expect(destinations.result.current.isSuccess).toBe(true));

        const contacts = renderHook(() => useTrips(api).useGetDestinationContacts(4, 9, 1, 6), { wrapper });
        await waitFor(() => expect(contacts.result.current.isSuccess).toBe(true));

        expect(get).toHaveBeenNthCalledWith(1, "/group-trips/4/trips", {
            params: { page: 2, pageSize: 6 },
            headers: { Accept: VndType.APPLICATION_GROUP_TRIP_DESTINATION },
        });
        expect(get).toHaveBeenNthCalledWith(2, "/group-trips/4/trips/9", {
            params: { page: 1, pageSize: 6 },
            headers: { Accept: VndType.APPLICATION_GROUP_TRIP_DESTINATION_DETAIL },
        });
        expect(get).toHaveBeenNthCalledWith(3, "/contacts?tripId=9&page=1&pageSize=6", {
            headers: { Accept: VndType.APPLICATION_CONTACT },
        });
        expect(tripKeys.destinations(4, 1, 6)).not.toEqual(tripKeys.destinationContacts(4, 9, 1, 6));
    });

    it("creates a group trip, preserves Location, and invalidates trips", async () => {
        const response = { data: undefined, headers: { location: "/group-trips/4" } };
        const post = vi.fn().mockResolvedValue(response);
        const api = { post } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

        const { result } = renderHook(() => useTrips(api).useCreateGroupTrip(), { wrapper });
        const returned = await result.current.mutateAsync({
            ownerId: 7,
            title: "Europe",
        });

        expect(returned).toBe(response);
        expect(post).toHaveBeenCalledWith("/group-trips", {
            ownerId: 7,
            title: "Europe",
        }, {
            headers: { "Content-Type": VndType.APPLICATION_GROUP_TRIP_DETAIL },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: tripKeys.all });
    });

    it("updates and completes group trips with general and detail invalidations", async () => {
        const patch = vi.fn().mockResolvedValue({ data: { id: 4, status: "UPCOMING" } });
        const api = { patch } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");

        const { result } = renderHook(() => useTrips(api).useUpdateGroupTrip(), { wrapper });
        const returned = await result.current.mutateAsync({
            groupTripId: 4,
            payload: { status: "UPCOMING" },
        });

        expect(returned).toEqual({ id: 4, status: "UPCOMING" });
        expect(patch).toHaveBeenCalledWith("/group-trips/4", { status: "UPCOMING" }, {
            headers: {
                "Content-Type": VndType.APPLICATION_GROUP_TRIP_DETAIL,
                Accept: VndType.APPLICATION_GROUP_TRIP_DETAIL,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: tripKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: tripKeys.groupTripDetail(4) });
    });

    it("finds group trips for association through the injected client", async () => {
        const get = vi.fn().mockResolvedValue({ data: [{ id: 4, tripId: 9 }], headers: {} });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useTrips(api).useFindGroupTripsForAssociation(), { wrapper });
        const response = await result.current({
            userId: 7,
            page: 1,
            pageSize: 1,
            filters: { country: "Germany", checkIn: "2026-08-01", checkOut: "2026-08-05" },
        });

        expect(response.data).toEqual([{ id: 4, tripId: 9 }]);
        expect(get).toHaveBeenCalledWith("/group-trips", {
            params: {
                userId: 7,
                page: 1,
                pageSize: 1,
                country: "Germany",
                checkIn: "2026-08-01",
                checkOut: "2026-08-05",
            },
            headers: { Accept: VndType.APPLICATION_GROUP_TRIP },
        });
    });
});
