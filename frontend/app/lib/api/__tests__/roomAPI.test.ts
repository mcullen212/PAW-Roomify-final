import { beforeEach, describe, expect, it, vi } from "vitest"
import { createRoomAPI } from "../roomAPI"
import { VndType } from "../vndTypes"

const client = {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
}

describe("roomApi", () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it("loads effective room availability with the availability media type", () => {
        const roomApi = createRoomAPI(client as any)

        roomApi.getRoomAvailability(42)

        expect(client.get).toHaveBeenCalledWith("/rooms/42/availabilities", {
            headers: {
                Accept: VndType.APPLICATION_ROOM_AVAILABILITY,
            },
        })
    })

    it("loads effective room availability scoped to a date range", () => {
        const roomApi = createRoomAPI(client as any)

        roomApi.getRoomAvailability(42, {
            startDate: "2026-08-01",
            endDate: "2026-08-05",
        })

        expect(client.get).toHaveBeenCalledWith("/rooms/42/availabilities", {
            headers: {
                Accept: VndType.APPLICATION_ROOM_AVAILABILITY,
            },
            params: {
                startDate: "2026-08-01",
                endDate: "2026-08-05",
            },
        })
    })
})
