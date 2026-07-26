import type { LucideIcon } from "lucide-react";
import type { RoomAvailabilityCalendar } from "~/lib/interfaces/room-availability";

export type SwapTabId = "sent" | "received" | "active" | "canceled" | "past" | "expired";

export interface SwapTabConfig {
    id: SwapTabId;
    label: string;
    title: string;
    emptyMessage: string;
    icon: LucideIcon;
}

export type RoomSummary = {
    id: number;
    title: string;
    location: string;
    imageUrl: string;
    ownerId: number | null;
    availabilityCalendar: RoomAvailabilityCalendar | null;
};

export type ContactActionMode = "accept" | "reject" | "cancel";

export type ContactActionError = {
    message: string;
    closeDialog: boolean;
    refetch: boolean;
};
