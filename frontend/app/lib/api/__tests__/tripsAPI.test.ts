import { beforeEach, describe, expect, it, vi } from "vitest"
import tripsAPI, { createTripsAPI } from "../tripsAPI"
import { VndType } from "../vndTypes"

const apiMock = vi.hoisted(() => ({
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
}))

vi.mock("../api", () => ({
    default: apiMock,
}))

describe("tripsAPI", () => {
    beforeEach(() => {
        apiMock.get.mockReset()
        apiMock.post.mockReset()
        apiMock.patch.mockReset()
        apiMock.get.mockResolvedValue({ data: [], headers: {} })
    })

    it("gets my group trips with user id, status, and filters", async () => {
        await tripsAPI.getMyGroupTrips(7, 1, 6, "PLANNING", {
            country: "Germany",
            checkIn: "2026-08-01",
            checkOut: "2026-08-05",
        })

        expect(apiMock.get).toHaveBeenCalledWith("/group-trips", {
            params: {
                userId: 7,
                page: 1,
                pageSize: 6,
                status: "PLANNING",
                country: "Germany",
                checkIn: "2026-08-01",
                checkOut: "2026-08-05",
            },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP,
            },
        })
    })

    it("uses the injected client when created explicitly", async () => {
        const injectedClient = {
            get: vi.fn().mockResolvedValue({ data: [], headers: {} }),
        }
        const injectedTripsAPI = createTripsAPI(injectedClient as any)

        await injectedTripsAPI.getMyGroupTrips(7, 1, 6, "PLANNING")

        expect(injectedClient.get).toHaveBeenCalledWith("/group-trips", {
            params: {
                userId: 7,
                page: 1,
                pageSize: 6,
                status: "PLANNING",
            },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP,
            },
        })
        expect(apiMock.get).not.toHaveBeenCalled()
    })

    it("gets group trips for association with trip filters", async () => {
        await tripsAPI.getGroupTripsForAssociation(7, 1, 1, {
            country: "Germany",
            checkIn: "2026-08-01",
            checkOut: "2026-08-05",
        })

        expect(apiMock.get).toHaveBeenCalledWith("/group-trips", {
            params: {
                userId: 7,
                page: 1,
                pageSize: 1,
                country: "Germany",
                checkIn: "2026-08-01",
                checkOut: "2026-08-05",
            },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP,
            },
        })
    })

    it("gets destination contacts with the destination detail media type", async () => {
        await tripsAPI.getDestinationContacts(4, 9, 1, 6)

        expect(apiMock.get).toHaveBeenCalledWith("/group-trips/4/trips/9", {
            params: { page: 1, pageSize: 6 },
            headers: {
                Accept: VndType.APPLICATION_GROUP_TRIP_DESTINATION_DETAIL,
            },
        })
    })
})
