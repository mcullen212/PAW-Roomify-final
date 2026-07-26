import { describe, expect, it } from "vitest"
import type { DateRange } from "react-day-picker"
import {
    dateOnly,
    formatDateForApi,
    getAvailabilityDefaultMonth,
    getAvailabilityMonth,
    getAvailabilityQueryForYear,
    getInitialAvailabilityMonth,
    hasOverlappingDateRanges,
    hasSelectableStay,
    isDateSelectable,
    isRangeSelectable,
    parseApiDate,
    type RoomAvailabilityCalendar,
} from "../room-availability"

const calendar: RoomAvailabilityCalendar = {
    roomId: 1,
    availabilityRanges: [
        { startDate: "2026-02-01", endDate: "2026-02-28" },
    ],
    bookedRanges: [
        { startDate: "2026-02-10", endDate: "2026-02-14" },
    ],
    selectableRanges: [
        { startDate: "2026-02-01", endDate: "2026-02-09" },
        { startDate: "2026-02-15", endDate: "2026-02-28" },
    ],
    firstSelectableDate: "2026-02-01",
    hasSelectableStay: true,
}

const today = new Date("2026-01-01T00:00:00")

describe("room availability helpers", () => {
    it("parses and formats API dates without timezone shifts", () => {
        expect(formatDateForApi(new Date(2026, 1, 3, 18, 30))).toBe("2026-02-03")
        expect(parseApiDate("2026-02-03")?.getFullYear()).toBe(2026)
        expect(parseApiDate("2026-02-03")?.getMonth()).toBe(1)
        expect(parseApiDate("2026-02-03")?.getDate()).toBe(3)
        expect(parseApiDate("bad-date")).toBeNull()
        expect(dateOnly(new Date("2026-02-03T18:30:00")).getHours()).toBe(0)
    })

    it("allows dates only inside availability and outside booked ranges", () => {
        expect(isDateSelectable(new Date("2026-02-01T00:00:00"), calendar, today)).toBe(true)
        expect(isDateSelectable(new Date("2026-01-31T00:00:00"), calendar, today)).toBe(false)
        expect(isDateSelectable(new Date("2026-02-12T00:00:00"), calendar, today)).toBe(false)
        expect(isDateSelectable(new Date("2026-03-01T00:00:00"), calendar, today)).toBe(false)
    })

    it("rejects ranges that leave availability or cross booked dates", () => {
        const validRange: DateRange = {
            from: new Date("2026-02-15T00:00:00"),
            to: new Date("2026-02-18T00:00:00"),
        }
        const crossesBooking: DateRange = {
            from: new Date("2026-02-08T00:00:00"),
            to: new Date("2026-02-15T00:00:00"),
        }
        const outsideAvailability: DateRange = {
            from: new Date("2026-02-27T00:00:00"),
            to: new Date("2026-03-02T00:00:00"),
        }

        expect(isRangeSelectable(validRange, calendar, today)).toBe(true)
        expect(isRangeSelectable(crossesBooking, calendar, today)).toBe(false)
        expect(isRangeSelectable(outsideAvailability, calendar, today)).toBe(false)
        expect(isRangeSelectable({ from: validRange.from, to: validRange.from }, calendar, today)).toBe(false)
    })

    it("detects overlapping API date ranges", () => {
        expect(hasOverlappingDateRanges([
            { startDate: "2026-08-10", endDate: "2026-08-15" },
            { startDate: "2026-08-14", endDate: "2026-08-20" },
        ])).toBe(true)
        expect(hasOverlappingDateRanges([
            { startDate: "2026-09-01", endDate: "2026-09-05" },
            { startDate: "2026-08-10", endDate: "2026-08-15" },
        ])).toBe(false)
    })

    it("chooses the first useful month when no range is selected", () => {
        expect(getAvailabilityDefaultMonth(calendar)?.getMonth()).toBe(1)
        expect(getAvailabilityDefaultMonth(calendar, { from: new Date("2026-02-20T00:00:00") })?.getDate()).toBe(20)
        expect(hasSelectableStay(calendar)).toBe(true)
        expect(hasSelectableStay({ ...calendar, hasSelectableStay: false })).toBe(false)
    })

    it("builds availability queries for the visible year", () => {
        expect(getAvailabilityMonth(new Date(2026, 6, 18))).toEqual(new Date(2026, 6, 1))
        expect(getAvailabilityQueryForYear(new Date(2026, 6, 18))).toEqual({
            startDate: "2026-01-01",
            endDate: "2026-12-31",
        })
        expect(getAvailabilityQueryForYear(new Date(2027, 0, 10))).toEqual({
            startDate: "2027-01-01",
            endDate: "2027-12-31",
        })
    })

    it("uses searched check-in month as the initial availability month", () => {
        expect(getInitialAvailabilityMonth({
            from: new Date(2026, 8, 15),
            to: new Date(2026, 8, 20),
        })).toEqual(new Date(2026, 8, 1))
        expect(getInitialAvailabilityMonth(undefined, new Date(2026, 6, 18))).toEqual(new Date(2026, 6, 1))
    })
})
