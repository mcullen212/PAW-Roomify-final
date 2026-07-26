export type TripStatus = "planning" | "confirmed" | "pending" | "completed"
export type Amenity = "WIFI" | "AC" | "HEATING" | "PARKING" | "POOL" | "GYM"

export interface Destination {
    id: string
    name: string
    countryCode: string
    days: number
    flagEmoji: string
}

export interface Trip {
    id: string
    title: string
    location: string
    status: TripStatus
    startDate: string
    endDate: string
    destinations: Destination[]
    daysUntil?: number
}

export interface Room {
    id: number
    title: string
    description?: string
    imageUrl?: string
    country: string
    city: string
    dayPrice: number
    amenities?: Amenity[]
    bedType?: string
    roomType?: string
    privateBathroom?: boolean
    privateKitchen?: boolean
    owner?: string
    reviews?: string
    self?: string
    totalReviews?: number
    averageRating?: number
}
