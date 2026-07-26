import { act, renderHook, waitFor } from "@testing-library/react";
import type { TFunction } from "i18next";
import type { FormEvent } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { GroupTripAssociationDTO } from "~/lib/interfaces/trips";
import type { RoomAvailabilityCalendar } from "~/lib/room-availability";
import type { Room } from "~/types";
import { useRoomRequestForm } from "../useRoomRequestForm";

const navigateMock = vi.hoisted(() => vi.fn());
const toastSuccessMock = vi.hoisted(() => vi.fn());
const toastErrorMock = vi.hoisted(() => vi.fn());
const mutateContactMock = vi.hoisted(() => vi.fn());
const findGroupTripsMock = vi.hoisted(() => vi.fn());

vi.mock("react-router", async () => {
    const actual = await vi.importActual<typeof import("react-router")>("react-router");
    return {
        ...actual,
        useNavigate: () => navigateMock,
    };
});

vi.mock("sonner", () => ({
    toast: {
        success: toastSuccessMock,
        error: toastErrorMock,
    },
}));

vi.mock("~/lib/hooks/useApiServices.ts", () => ({
    useApiServices: () => ({
        contactService: {
            useCreateContact: () => ({
                mutateAsync: mutateContactMock,
            }),
        },
        roomService: {
            useGetMyRooms: () => ({
                data: { data: [offeredRoom] },
                isLoading: false,
                isFetching: false,
                isError: false,
            }),
            useGetRoomAvailability: () => ({
                data: availabilityCalendar,
                isLoading: false,
                isFetching: false,
                isError: false,
                refetch: vi.fn(),
            }),
        },
        tripService: {
            useFindGroupTripsForAssociation: () => findGroupTripsMock,
        },
    }),
}));

const t = ((key: string) => key) as TFunction;

const requestedRoom: Room = {
    id: 4,
    title: "Paris room",
    country: "France",
    city: "Paris",
    dayPrice: 100,
    owner: "/users/2",
};

const offeredRoom: Room = {
    id: 8,
    title: "My room",
    country: "Argentina",
    city: "Buenos Aires",
    dayPrice: 90,
    owner: "/users/1",
};

const availabilityCalendar: RoomAvailabilityCalendar = {
    roomId: 4,
    availabilityRanges: [{ startDate: "2026-08-01", endDate: "2026-08-31" }],
    bookedRanges: [],
    selectableRanges: [{ startDate: "2026-08-01", endDate: "2026-08-31" }],
    firstSelectableDate: "2026-08-01",
    hasSelectableStay: true,
};

const matchingTrip: GroupTripAssociationDTO = {
    id: 11,
    title: "Europe",
    status: "PLANNING",
    startDate: "2026-08-01",
    endDate: "2026-08-20",
    tripId: 22,
    country: "France",
    tripStartDate: "2026-08-01",
    tripEndDate: "2026-08-20",
};

const secondMatchingTrip: GroupTripAssociationDTO = {
    ...matchingTrip,
    id: 12,
    title: "Backup Europe",
    tripId: 23,
};

function submitEvent() {
    return { preventDefault: vi.fn() } as unknown as FormEvent<HTMLFormElement>;
}

function renderRequestForm() {
    const hook = renderHook(() => useRoomRequestForm({
        authenticated: true,
        authLoading: false,
        goToLogin: vi.fn(),
        room: requestedRoom,
        roomId: requestedRoom.id,
        t,
        userId: 1,
        verified: true,
    }));

    act(() => {
        hook.result.current.setDateRange({
            from: new Date(2026, 7, 10),
            to: new Date(2026, 7, 15),
        });
    });

    return hook;
}

describe("useRoomRequestForm trip matching", () => {
    beforeEach(() => {
        navigateMock.mockClear();
        toastSuccessMock.mockClear();
        toastErrorMock.mockClear();
        mutateContactMock.mockReset().mockResolvedValue({ data: undefined });
        findGroupTripsMock.mockReset();
    });

    it("opens add-to-trip state and defaults to the first match", async () => {
        findGroupTripsMock.mockResolvedValue({ data: [matchingTrip, secondMatchingTrip] });
        const { result } = renderRequestForm();

        await act(async () => {
            await result.current.handleSubmitRequest(submitEvent());
        });

        await waitFor(() => expect(result.current.addToTripDialogOpen).toBe(true));
        expect(result.current.matchingTrips).toEqual([matchingTrip, secondMatchingTrip]);
        expect(result.current.selectedMatchingTripId).toBe(matchingTrip.tripId);
        expect(mutateContactMock).not.toHaveBeenCalled();
        expect(findGroupTripsMock).toHaveBeenCalledWith({
            userId: 1,
            page: 1,
            pageSize: 100,
            filters: {
                country: "France",
                checkIn: "2026-08-10",
                checkOut: "2026-08-15",
            },
        });
    });

    it("confirms by creating the contact with the selected trip and redirecting to destination contacts", async () => {
        findGroupTripsMock.mockResolvedValue({ data: [matchingTrip, secondMatchingTrip] });
        const { result } = renderRequestForm();

        await act(async () => {
            await result.current.handleSubmitRequest(submitEvent());
        });
        act(() => {
            result.current.setSelectedMatchingTripId(secondMatchingTrip.tripId);
        });
        await act(async () => {
            await result.current.confirmAddToTrip();
        });

        expect(mutateContactMock).toHaveBeenCalledWith({
            contactData: {
                roomRequestedId: 4,
                checkIn: "2026-08-10",
                checkOut: "2026-08-15",
                isSwap: true,
                roomOfferedId: 8,
            },
            tripId: 23,
        });
        expect(navigateMock).toHaveBeenCalledWith("/trips/12/contacts?destinationId=23");
    });

    it("cancels by creating the contact without a trip and redirecting to swaps", async () => {
        findGroupTripsMock.mockResolvedValue({ data: [matchingTrip] });
        const { result } = renderRequestForm();

        await act(async () => {
            await result.current.handleSubmitRequest(submitEvent());
        });
        await act(async () => {
            await result.current.cancelAddToTrip();
        });

        expect(mutateContactMock).toHaveBeenCalledWith({
            contactData: {
                roomRequestedId: 4,
                checkIn: "2026-08-10",
                checkOut: "2026-08-15",
                isSwap: true,
                roomOfferedId: 8,
            },
            tripId: undefined,
        });
        expect(navigateMock).toHaveBeenCalledWith("/swaps?view=sent&page=1");
    });

    it("creates without a trip when no association matches", async () => {
        findGroupTripsMock.mockResolvedValue({ data: [] });
        const { result } = renderRequestForm();

        await act(async () => {
            await result.current.handleSubmitRequest(submitEvent());
        });

        expect(result.current.addToTripDialogOpen).toBe(false);
        expect(mutateContactMock).toHaveBeenCalledWith({
            contactData: {
                roomRequestedId: 4,
                checkIn: "2026-08-10",
                checkOut: "2026-08-15",
                isSwap: true,
                roomOfferedId: 8,
            },
            tripId: undefined,
        });
        expect(navigateMock).toHaveBeenCalledWith("/swaps?view=sent&page=1");
    });

    it("creates without a trip when association lookup fails", async () => {
        findGroupTripsMock.mockRejectedValue(new Error("lookup failed"));
        const { result } = renderRequestForm();

        await act(async () => {
            await result.current.handleSubmitRequest(submitEvent());
        });

        expect(result.current.addToTripDialogOpen).toBe(false);
        expect(mutateContactMock).toHaveBeenCalledWith(expect.objectContaining({
            tripId: undefined,
        }));
        expect(navigateMock).toHaveBeenCalledWith("/swaps?view=sent&page=1");
    });
});
