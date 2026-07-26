import type { RoomSearchFilters } from "~/lib/interfaces/room-search"

export type { RoomSearchFilters } from "~/lib/interfaces/room-search"

const API_DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

function dateOnly(date: Date) {
    const normalized = new Date(date)
    normalized.setHours(0, 0, 0, 0)
    return normalized
}

export function getSearchToday() {
    return dateOnly(new Date())
}

export function formatRoomSearchDate(date: Date) {
    const year = date.getFullYear()
    const month = `${date.getMonth() + 1}`.padStart(2, "0")
    const day = `${date.getDate()}`.padStart(2, "0")

    return `${year}-${month}-${day}`
}

export function parseRoomSearchDate(value?: string) {
    if (!value || !API_DATE_PATTERN.test(value)) return undefined

    const [year, month, day] = value.split("-").map(Number)
    const date = dateOnly(new Date(year, month - 1, day))

    return formatRoomSearchDate(date) === value ? date : undefined
}

export function normalizeRoomSearchFilters(filters: RoomSearchFilters, today = getSearchToday()): RoomSearchFilters {
    const normalized: RoomSearchFilters = { ...filters }
    delete normalized.checkIn
    delete normalized.checkOut

    const checkIn = parseRoomSearchDate(filters.checkIn)
    const checkOut = parseRoomSearchDate(filters.checkOut)
    const minimumCheckIn = dateOnly(today)

    if (checkIn && checkOut && checkIn >= minimumCheckIn && checkOut > checkIn) {
        normalized.checkIn = formatRoomSearchDate(checkIn)
        normalized.checkOut = formatRoomSearchDate(checkOut)
    }

    return normalized
}

export function readRoomSearchFilters(params: URLSearchParams): RoomSearchFilters {
    const value = (key: string) => params.get(key) || undefined

    return normalizeRoomSearchFilters({
        destination: value("destination"),
        checkIn: value("checkIn"),
        checkOut: value("checkOut"),
        roomType: value("roomType"),
        bedType: value("bedType"),
        privateBathroom: params.get("privateBathroom") === "true" || undefined,
        privateKitchen: params.get("privateKitchen") === "true" || undefined,
        amenities: params.getAll("amenities").filter(Boolean).length
            ? params.getAll("amenities").filter(Boolean)
            : undefined,
    })
}

export function writeRoomSearchFilters(filters: RoomSearchFilters) {
    const params = new URLSearchParams()
    const normalizedFilters = normalizeRoomSearchFilters(filters)

    const setValue = (key: string, value?: string) => {
        if (value) params.set(key, value)
    }

    setValue("destination", normalizedFilters.destination)
    setValue("checkIn", normalizedFilters.checkIn)
    setValue("checkOut", normalizedFilters.checkOut)
    setValue("roomType", normalizedFilters.roomType)
    setValue("bedType", normalizedFilters.bedType)
    if (normalizedFilters.privateBathroom) params.set("privateBathroom", "true")
    if (normalizedFilters.privateKitchen) params.set("privateKitchen", "true")
    normalizedFilters.amenities?.forEach((amenity) => params.append("amenities", amenity))

    return params
}
