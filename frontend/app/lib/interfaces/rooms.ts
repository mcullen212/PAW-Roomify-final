import type { Amenity, Room } from "@/types";

export interface RoomUpdatePayload {
    title: string;
    description: string;
    amenities: Amenity[];
    dayPrice: number;
}

export type RoomDetail = Room & {
    description: string;
    amenities: Amenity[];
    bedType: string;
    roomType: string;
    privateBathroom: boolean;
    privateKitchen: boolean;
    owner: string;
    reviews: string;
    totalReviews: number;
    averageRating: number;
};

export type RoomAvailabilityRange = {
    startDate: string;
    endDate: string;
};

export type RoomCreatePayload = {
    title: string;
    country: string;
    city: string;
    description: string;
    roomType: string;
    bedType: string;
    privateBathroom: boolean;
    privateKitchen: boolean;
    amenities: string[];
    dateRange: RoomAvailabilityRange[];
    dayPrice: number;
    imageId: number;
};

export type RoomCreateRequestPayload = RoomCreatePayload & {
    userId: number;
};

export type RoomAvailabilityQuery = {
    startDate?: string;
    endDate?: string;
};
