import { afterEach, describe, expect, it, vi } from "vitest"
import {
    normalizeRoomSearchFilters,
    readRoomSearchFilters,
    writeRoomSearchFilters,
} from "../room-search-params"

const today = new Date("2026-07-18T00:00:00")

afterEach(() => {
    vi.useRealTimers()
})

describe("room search params", () => {
    it("keeps valid date ranges from today onwards", () => {
        expect(normalizeRoomSearchFilters({
            destination: "Buenos Aires",
            checkIn: "2026-07-18",
            checkOut: "2026-07-20",
        }, today)).toEqual({
            destination: "Buenos Aires",
            checkIn: "2026-07-18",
            checkOut: "2026-07-20",
        })
    })

    it("clears past date ranges while preserving other filters", () => {
        expect(normalizeRoomSearchFilters({
            destination: "Bariloche",
            checkIn: "2026-07-17",
            checkOut: "2026-07-20",
            amenities: ["WIFI"],
        }, today)).toEqual({
            destination: "Bariloche",
            amenities: ["WIFI"],
        })
    })

    it("clears incomplete or non-forward date ranges", () => {
        expect(normalizeRoomSearchFilters({
            checkIn: "2026-07-18",
        }, today)).toEqual({})

        expect(normalizeRoomSearchFilters({
            checkIn: "2026-07-20",
            checkOut: "2026-07-20",
        }, today)).toEqual({})

        expect(normalizeRoomSearchFilters({
            checkIn: "2026-07-21",
            checkOut: "2026-07-20",
        }, today)).toEqual({})
    })

    it("normalizes dates when reading and writing URL params", () => {
        vi.useFakeTimers()
        vi.setSystemTime(new Date("2026-07-18T12:00:00"))

        const params = new URLSearchParams()
        params.set("destination", "Mendoza")
        params.set("checkIn", "2026-07-01")
        params.set("checkOut", "2026-07-05")
        params.append("amenities", "WIFI")

        expect(readRoomSearchFilters(params)).toEqual({
            destination: "Mendoza",
            amenities: ["WIFI"],
        })

        expect(writeRoomSearchFilters({
            destination: "Mendoza",
            checkIn: "2026-07-20",
            checkOut: "2026-07-19",
            amenities: ["WIFI"],
        }).toString()).toBe("destination=Mendoza&amenities=WIFI")
    })
})
