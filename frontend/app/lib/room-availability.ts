import type { DateRange } from "react-day-picker"
import type { ApiDateRange, RoomAvailabilityCalendar } from "~/lib/interfaces/room-availability"
import { formatDateForApi } from "~/lib/datesUtils"

export type { ApiDateRange, RoomAvailabilityCalendar } from "~/lib/interfaces/room-availability"
export { formatDateForApi } from "~/lib/datesUtils"

const MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000

export function parseApiDate(value?: string | null) {
    if (!value) return null

    const [year, month, day] = value.split("-").map(Number)
    if ([year, month, day].some((part) => !Number.isFinite(part))) {
        return null
    }

    return new Date(year, month - 1, day)
}

export function getAvailabilityMonth(date: Date) {
    return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function getAvailabilityQueryForYear(month: Date) {
    const visibleYear = month.getFullYear()

    return {
        startDate: formatDateForApi(new Date(visibleYear, 0, 1)),
        endDate: formatDateForApi(new Date(visibleYear, 11, 31)),
    }
}

export function getInitialAvailabilityMonth(initialRange?: DateRange, today = dateOnly(new Date())) {
    return getAvailabilityMonth(initialRange?.from ?? today)
}

export function dateOnly(value: Date) {
    const normalized = new Date(value)
    normalized.setHours(0, 0, 0, 0)
    return normalized
}

export function isDateInsideRanges(date: Date, ranges: ApiDateRange[]) {
    const candidate = formatDateForApi(date)

    return ranges.some((range) => (
        candidate >= range.startDate && candidate <= range.endDate
    ))
}

export function hasOverlappingDateRanges(ranges: ApiDateRange[]) {
    const sortedRanges = [...ranges].sort((a, b) => a.startDate.localeCompare(b.startDate))

    return sortedRanges.some((range, index) => (
        index > 0 && range.startDate <= sortedRanges[index - 1].endDate
    ))
}

export function isDateSelectable(date: Date, calendar?: RoomAvailabilityCalendar | null, today = dateOnly(new Date())) {
    const candidate = dateOnly(date)

    if (candidate < today || !calendar) {
        return false
    }

    return isDateInsideRanges(candidate, calendar.selectableRanges)
}

export function isRangeSelectable(range: DateRange | undefined, calendar?: RoomAvailabilityCalendar | null, today = dateOnly(new Date())) {
    if (!range?.from || !range.to || range.to <= range.from) {
        return false
    }

    const startTime = dateOnly(range.from).getTime()
    const endTime = dateOnly(range.to).getTime()

    for (let time = startTime; time <= endTime; time += MILLISECONDS_PER_DAY) {
        if (!isDateSelectable(new Date(time), calendar, today)) {
            return false
        }
    }

    return true
}

export function getAvailabilityDefaultMonth(calendar?: RoomAvailabilityCalendar | null, selectedRange?: DateRange) {
    return selectedRange?.from
        ?? parseApiDate(calendar?.firstSelectableDate)
        ?? undefined
}

export function isAvailabilityLoaded(calendar?: RoomAvailabilityCalendar | null): calendar is RoomAvailabilityCalendar {
    return Boolean(calendar)
}

export function hasSelectableStay(calendar?: RoomAvailabilityCalendar | null) {
    return Boolean(calendar?.hasSelectableStay)
}
