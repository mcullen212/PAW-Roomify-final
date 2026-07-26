export type ContactDateRange = {
    startDate: string;
    endDate: string;
};

export type ContactStatus =
    | "PENDING"
    | "ACCEPTED"
    | "REJECTED"
    | "COMPLETED"
    | "CANCELED"
    | "EXPIRED";

export type Contact = {
    id: number;
    contactDate: string | null;
    status: ContactStatus;
    isSwap: boolean;
    moneyOffer: number;
    requestedRange: ContactDateRange | null;
    offeredRange: ContactDateRange | null;
    offerUserId: number | null;
    offerUserName?: string | null;
    roomRequestedId: number;
    roomRequestedOwnerId?: number | null;
    roomRequestedOwnerName?: string | null;
    roomOfferedId: number | null;
    roomOfferedOwnerId?: number | null;
    roomOfferedOwnerName?: string | null;
    pendingReview?: boolean | null;
    links?: Record<string, string>;
};

export type CreateContactPayload = {
    roomRequestedId: number;
    checkIn: string;
    checkOut: string;
    isSwap: boolean;
    dayPrice?: number;
    roomOfferedId?: number;
};

export type UpdateContactPayload = {
    status: Extract<ContactStatus, "ACCEPTED" | "REJECTED" | "CANCELED">;
    checkIn?: string;
    checkOut?: string;
};
