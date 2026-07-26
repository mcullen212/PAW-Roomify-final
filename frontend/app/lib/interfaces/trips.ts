import type { Contact } from "~/lib/interfaces/contacts";
import type { PaginationState } from "~/lib/interfaces/pagination";

export interface ResourceLinks {
    self?: string;
    owner?: string;
    destinations?: string;
    groupTrip?: string;
    contacts?: string;
    [key: string]: string | undefined;
}

export interface GroupTripDTO {
    id: number;
    title: string;
    status: string;
    startDate: string | null;
    endDate: string | null;
    tripId?: number | null;
    country?: string | null;
    countryCode?: string | null;
    tripStartDate?: string | null;
    tripEndDate?: string | null;
    _links?: ResourceLinks;
}

export interface GroupTripAssociationDTO extends GroupTripDTO {
    tripId: number;
    country: string;
    tripStartDate: string;
    tripEndDate: string;
}

export interface TripDTO {
    id: number;
    country: string;
    countryCode?: string;
    startDate: string;
    endDate: string;
    _links?: ResourceLinks;
}

export interface PaginatedResponse<T> {
    data: T[];
    pagination: PaginationState;
}

export interface CreateGroupTripPayload {
    ownerId: number;
    title: string;
}

export interface CreateTripPayload {
    country: string;
    startDate: string;
    endDate: string;
}

export interface UpdateGroupTripPayload {
    status: string;
}

export interface TripFilters {
    country?: string;
    checkIn?: string;
    checkOut?: string;
}

export type TripMatchDecision = "CONTAINED" | "DATES_OUTSIDE" | "NONE";

export interface TripMatchResult {
    decision: TripMatchDecision;
    tripId?: number | null;
    groupTripId?: number | null;
    groupTripTitle?: string | null;
    startDate?: string | null;
    endDate?: string | null;
    _links?: ResourceLinks;
}

export type DestinationContactsResponse = PaginatedResponse<Contact>;
