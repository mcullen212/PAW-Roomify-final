import { format } from "date-fns";
import type { DateRange } from "react-day-picker";
import type { Contact } from "~/lib/interfaces/contacts";
import {
    hasSelectableStay,
    isDateSelectable,
    isRangeSelectable,
} from "~/lib/room-availability";
import type { RoomAvailabilityCalendar } from "~/lib/interfaces/room-availability";
import type {
    ContactActionError,
    ContactActionMode,
    RoomSummary,
    SwapTabId,
} from "~/lib/interfaces/swaps";
import i18n from "~/i18n/i18n";

type RawRoomSummary = {
    title?: string | null;
    city?: string | null;
    country?: string | null;
    imageUrl?: unknown;
    owner?: unknown;
};

type Translate = (key: string, options?: Record<string, unknown>) => string;

export function getSwapRoomIds(contacts: Contact[]) {
    return Array.from(new Set(
        contacts.flatMap((contact) => [
            contact.roomRequestedId,
            contact.roomOfferedId,
        ]).filter((roomId): roomId is number => typeof roomId === "number"),
    ));
}

export function createFallbackRoomSummary(roomId: number): RoomSummary {
    return {
        id: roomId,
        title: `Room #${roomId}`,
        location: "",
        imageUrl: "",
        ownerId: null,
        availabilityCalendar: null,
    };
}

export function mapRoomSummary(
    roomId: number,
    room: RawRoomSummary,
    availabilityCalendar: RoomAvailabilityCalendar | null = null,
): RoomSummary {
    return {
        id: roomId,
        title: room.title ?? `Room #${roomId}`,
        location: [room.city, room.country].filter(Boolean).join(", "),
        imageUrl: String(room.imageUrl ?? ""),
        ownerId: extractIdFromUri(room.owner),
        availabilityCalendar,
    };
}

export function isReceivedPendingOwner(
    contact: Contact,
    requestedRoom: Pick<RoomSummary, "ownerId"> | undefined,
    activeTab: SwapTabId,
    userId?: number,
) {
    if (activeTab !== "received" || contact.status !== "PENDING" || !userId) {
        return false;
    }

    return (contact.roomRequestedOwnerId ?? requestedRoom?.ownerId) === userId;
}

export function isContactParticipant(
    contact: Contact,
    requestedRoom: Pick<RoomSummary, "ownerId"> | undefined,
    userId?: number,
) {
    if (!userId) {
        return false;
    }

    const requestedOwnerId = contact.roomRequestedOwnerId ?? requestedRoom?.ownerId;

    return contact.offerUserId === userId || requestedOwnerId === userId;
}

export function getEarliestContactStartDate(
    contact: Pick<Contact, "requestedRange" | "offeredRange">,
) {
    const startDates = [
        contact.requestedRange?.startDate,
        contact.offeredRange?.startDate,
    ]
        .filter((date): date is string => Boolean(date))
        .map((date) => new Date(`${date}T00:00:00`))
        .filter((date) => !Number.isNaN(date.getTime()));

    if (startDates.length === 0) {
        return null;
    }

    return startDates.reduce((earliest, date) => (
        date.getTime() < earliest.getTime() ? date : earliest
    ));
}

export function isMoreThanSevenDaysAway(date: Date) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const candidate = new Date(date);
    candidate.setHours(0, 0, 0, 0);

    const millisecondsPerDay = 1000 * 60 * 60 * 24;
    const daysAway = Math.round((candidate.getTime() - today.getTime()) / millisecondsPerDay);

    return daysAway > 7;
}

export function canCancelContact(
    contact: Contact,
    requestedRoom: Pick<RoomSummary, "ownerId"> | undefined,
    activeTab: SwapTabId,
    userId?: number,
) {
    if (!userId || !isContactParticipant(contact, requestedRoom, userId)) {
        return false;
    }

    if (activeTab !== "active" || contact.status !== "ACCEPTED") {
        return false;
    }

    const earliestStartDate = getEarliestContactStartDate(contact);

    return Boolean(earliestStartDate && isMoreThanSevenDaysAway(earliestStartDate));
}

export function canReviewContact(
    contact: Contact,
    requestedRoom: Pick<RoomSummary, "ownerId"> | undefined,
    activeTab: SwapTabId,
    userId?: number,
) {
    return activeTab === "past"
        && contact.status === "ACCEPTED"
        && contact.pendingReview === true
        && isContactParticipant(contact, requestedRoom, userId);
}

export function getRequestedNights(contact: Pick<Contact, "requestedRange">) {
    if (!contact.requestedRange?.startDate || !contact.requestedRange?.endDate) {
        return 0;
    }

    const start = new Date(`${contact.requestedRange.startDate}T00:00:00`);
    const end = new Date(`${contact.requestedRange.endDate}T00:00:00`);
    const millisecondsPerDay = 1000 * 60 * 60 * 24;

    return Math.max(0, Math.round((end.getTime() - start.getTime()) / millisecondsPerDay));
}

export function formatDateForApi(date: Date) {
    return format(date, "yyyy-MM-dd");
}

export function isSwapAcceptDateDisabled(date: Date, offeredRoom?: RoomSummary) {
    return !isDateSelectable(date, offeredRoom?.availabilityCalendar);
}

export function validateSwapAcceptanceRange(
    contact: Contact,
    selectedRange: DateRange | undefined,
    offeredRoom?: RoomSummary,
    t?: Translate,
) {
    const translate = t ?? ((key: string, options?: Record<string, unknown>) => {
        if (key === "swapActions.validation.nightLimit") {
            return `Choose ${options?.count} nights or fewer for the offered room.`;
        }
        return key;
    });

    if (!selectedRange?.from || !selectedRange?.to) {
        return translate("swapActions.validation.selectOfferedRoomDates");
    }

    if (selectedRange.to <= selectedRange.from) {
        return translate("swapActions.validation.invalidDateRange");
    }

    if (!offeredRoom?.availabilityCalendar) {
        return translate("swapActions.validation.loadOfferedRoomAvailability");
    }

    if (!hasSelectableStay(offeredRoom.availabilityCalendar)) {
        return translate("swapActions.validation.noSelectableDates");
    }

    if (!isRangeSelectable(selectedRange, offeredRoom.availabilityCalendar)) {
        return translate("swapActions.validation.unavailableDates");
    }

    const requestedNights = getRequestedNights(contact);
    const selectedNights = getSelectedNights(selectedRange);

    if (requestedNights > 0 && selectedNights > requestedNights) {
        return translate("swapActions.validation.nightLimit", { count: requestedNights });
    }

    return null;
}

export function getSelectedNights(range: DateRange) {
    if (!range.from || !range.to) {
        return 0;
    }

    const millisecondsPerDay = 1000 * 60 * 60 * 24;

    return Math.max(0, Math.round((range.to.getTime() - range.from.getTime()) / millisecondsPerDay));
}

export function mapContactActionError(
    error: any,
    actionMode: ContactActionMode,
    isSwapAccept = false,
    t?: Translate,
): ContactActionError {
    const translate = t ?? ((key: string, options?: Record<string, unknown>) => i18n.t(key, options));
    const status = error?.response?.status;
    const backendMessage = error?.response?.data?.message;
    const normalizedBackendMessage = String(backendMessage ?? "").toLowerCase();
    const isCancel = actionMode === "cancel";

    switch (status) {
        case 400:
            return {
                message: isCancel
                    ? translate("swapActions.cancelInvalid")
                    : translate("swapActions.errors.invalidRequest"),
                closeDialog: false,
                refetch: false,
            };
        case 401:
            return {
                message: translate("swapActions.errors.sessionExpired"),
                closeDialog: true,
                refetch: false,
            };
        case 403:
            return {
                message: isCancel
                    ? translate("swapActions.cancelForbidden")
                    : translate("swapActions.errors.ownerOnly"),
                closeDialog: true,
                refetch: true,
            };
        case 404:
            return {
                message: translate("swapActions.errors.notFound"),
                closeDialog: true,
                refetch: true,
            };
        case 409:
            if (
                isCancel
                && (
                    normalizedBackendMessage.includes("7")
                    || normalizedBackendMessage.includes("seven")
                    || normalizedBackendMessage.includes("week")
                    || normalizedBackendMessage.includes("cancellation")
                    || normalizedBackendMessage.includes("advance")
                )
            ) {
                return {
                    message: translate("swapActions.cancelWindow"),
                    closeDialog: true,
                    refetch: true,
                };
            }

            if (isCancel) {
                return {
                    message: translate("swapActions.errors.alreadyHandled"),
                    closeDialog: true,
                    refetch: true,
                };
            }

            if (normalizedBackendMessage.includes("book") || normalizedBackendMessage.includes("date")) {
                return {
                    message: translate("swapActions.errors.datesUnavailable"),
                    closeDialog: false,
                    refetch: false,
                };
            }

            return {
                message: translate("swapActions.errors.alreadyHandled"),
                closeDialog: true,
                refetch: true,
            };
        case 422:
            return {
                message: isCancel
                    ? backendMessage || translate("swapActions.cancelGeneralError")
                    : isSwapAccept
                    ? backendMessage || translate("swapActions.errors.invalidSwapDates")
                    : backendMessage || translate("swapActions.errors.invalidAction"),
                closeDialog: false,
                refetch: false,
            };
        default:
            return {
                message: backendMessage || (isCancel
                    ? translate("swapActions.errors.cancelNow")
                    : translate("swapActions.errors.updateNow")),
                closeDialog: false,
                refetch: false,
            };
    }
}

export function getOtherUser(
    contact: Contact,
    requestedRoom: RoomSummary | undefined,
    myUserId: number | null,
) {
    const requestedOwnerId = contact.roomRequestedOwnerId ?? requestedRoom?.ownerId;
    const requestedOwnerName = contact.roomRequestedOwnerName;
    const offerUserName = contact.offerUserName;

    if (myUserId && requestedOwnerId === myUserId && contact.offerUserId) {
        return {
            id: contact.offerUserId,
            name: offerUserName || `User #${contact.offerUserId}`,
        };
    }

    if (requestedOwnerId) {
        return {
            id: requestedOwnerId,
            name: requestedOwnerName || `User #${requestedOwnerId}`,
        };
    }

    if (contact.offerUserId && contact.offerUserId !== myUserId) {
        return {
            id: contact.offerUserId,
            name: offerUserName || `User #${contact.offerUserId}`,
        };
    }

    return undefined;
}

export function extractIdFromUri(uri: unknown) {
    if (typeof uri !== "string") {
        return null;
    }

    const match = uri.match(/\/(\d+)\/?$/);
    return match ? Number.parseInt(match[1], 10) : null;
}

export function formatRange(range: Contact["requestedRange"], t?: Translate, locale = "en-US") {
    if (!range?.startDate || !range?.endDate) {
        return t ? t("swaps.dates.pending") : "Dates pending";
    }

    return `${formatDate(range.startDate, locale)} - ${formatDate(range.endDate, locale)}`;
}

function formatDate(date: string, locale = "en-US") {
    return new Intl.DateTimeFormat(locale, {
        month: "short",
        day: "numeric",
        year: "numeric",
    }).format(new Date(`${date}T00:00:00`));
}

export function formatContactDate(date: string | null, t?: Translate, locale = "en-US") {
    if (!date) {
        return t ? t("swaps.dates.createdUnavailable") : "Created date unavailable";
    }

    return new Intl.DateTimeFormat(locale, {
        month: "short",
        day: "numeric",
        year: "numeric",
    }).format(new Date(date));
}

export function formatStatus(status: string, t?: Translate) {
    if (t) {
        const key = `swaps.status.${status.toUpperCase()}`;
        const translated = t(key);
        if (translated !== key) {
            return translated;
        }
    }

    return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
}

export function statusClass(status: string) {
    switch (status.toUpperCase()) {
        case "ACCEPTED":
            return "border-transparent bg-emerald-100 text-emerald-700";
        case "PENDING":
            return "border-transparent bg-amber-100 text-amber-700";
        case "REJECTED":
        case "CANCELED":
        case "EXPIRED":
            return "border-transparent bg-rose-100 text-rose-700";
        default:
            return "border-transparent bg-secondary text-secondary-foreground";
    }
}

export function formatMoney(amount: number) {
    return new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: 0,
    }).format(amount ?? 0);
}
