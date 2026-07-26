export interface RoomSearchFilters {
    destination?: string;
    checkIn?: string;
    checkOut?: string;
    roomType?: string;
    bedType?: string;
    privateBathroom?: boolean;
    privateKitchen?: boolean;
    amenities?: string[];
}
