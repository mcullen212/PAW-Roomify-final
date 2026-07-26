import { format } from "date-fns";
import type { FormEvent } from "react";
import { useEffect, useMemo, useState } from "react";
import type { DateRange } from "react-day-picker";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import type { TFunction } from "i18next";
import type { CreateContactPayload } from "~/lib/interfaces/contacts";
import type { RequestMode } from "~/lib/interfaces/room-requests";
import type { GroupTripAssociationDTO } from "~/lib/interfaces/trips";
import { useApiServices } from "~/lib/hooks/useApiServices.ts";
import {
    dateOnly,
    getAvailabilityQueryForYear,
    getInitialAvailabilityMonth,
    hasSelectableStay,
    isRangeSelectable,
    parseApiDate,
    type RoomAvailabilityCalendar,
} from "~/lib/room-availability";
import type { Room } from "~/types";
import { getApiErrorMessage } from "~/lib/api/api-error-message";
import { countDays } from "~/lib/datesUtils";

export type { RequestMode } from "~/lib/interfaces/room-requests";

export const requestCurrencyFormatter = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
});

function parseIdFromUri(uri?: string | null) {
    if (!uri) return null;

    const match = uri.match(/\/(\d+)(?:\?.*)?$/);
    if (!match) return null;

    const parsed = Number.parseInt(match[1], 10);
    return Number.isFinite(parsed) ? parsed : null;
}

export function normalizeMoneyInput(value: string) {
    const cleaned = value.replace(/[^\d.]/g, "");
    const [integerPart, ...decimalParts] = cleaned.split(".");
    const decimalPart = decimalParts.join("").slice(0, 2);

    return decimalParts.length > 0 ? `${integerPart}.${decimalPart}` : integerPart;
}

export function formatMoneyInputValue(value: string) {
    if (!value) return "";

    const [integerPart, decimalPart] = value.split(".");
    const formattedInteger = integerPart ? requestCurrencyFormatter.format(Number(integerPart)) : "";

    return value.includes(".") ? `${formattedInteger}.${decimalPart ?? ""}` : formattedInteger;
}

export function formatCurrencyAmount(amount: number) {
    return `$${requestCurrencyFormatter.format(amount)}`;
}

type UseRoomRequestFormParams = {
    authenticated: boolean;
    authLoading: boolean;
    goToLogin: () => void;
    initialDateRange?: DateRange;
    requestTripId?: number;
    room?: Room | null;
    roomId?: number;
    t: TFunction;
    userId?: number | null;
    verified: boolean;
};

export type RoomRequestFormState = ReturnType<typeof useRoomRequestForm>;

export function useRoomRequestForm({
    authenticated,
    authLoading,
    goToLogin,
    requestTripId,
    room,
    roomId,
    t,
    userId,
    verified,
    initialDateRange,
}: UseRoomRequestFormParams) {
    const navigate = useNavigate();
    const { contactService, roomService, tripService } = useApiServices();
    const createContactMutation = contactService.useCreateContact();
    const findGroupTripsForAssociation = tripService.useFindGroupTripsForAssociation();
    const myRoomsQuery = roomService.useGetMyRooms(userId ?? undefined);
    const today = useMemo(() => dateOnly(new Date()), []);
    const [calendarMonth, setCalendarMonth] = useState(() => getInitialAvailabilityMonth(initialDateRange, today));
    const availabilityQuery = useMemo(() => getAvailabilityQueryForYear(calendarMonth), [calendarMonth]);
    const availabilityResult = roomService.useGetRoomAvailability(roomId, availabilityQuery);
    const [datePopoverOpen, setDatePopoverOpen] = useState(false);
    const [dateRange, setDateRange] = useState<DateRange | undefined>();
    const [appliedInitialDateRangeKey, setAppliedInitialDateRangeKey] = useState("");
    const [requestMode, setRequestMode] = useState<RequestMode>("swap");
    const [roomOfferedId, setRoomOfferedId] = useState("");
    const [moneyOffer, setMoneyOffer] = useState("");
    const [requestError, setRequestError] = useState("");
    const [submittingRequest, setSubmittingRequest] = useState(false);
    const [addToTripDialogOpen, setAddToTripDialogOpen] = useState(false);
    const [matchingTrips, setMatchingTrips] = useState<GroupTripAssociationDTO[]>([]);
    const [selectedMatchingTripId, setSelectedMatchingTripId] = useState<number | undefined>();
    const myRooms = (myRoomsQuery.data?.data ?? []) as Room[];
    const myRoomsLoading = myRoomsQuery.isLoading || myRoomsQuery.isFetching;
    const availabilityCalendar = (availabilityResult.data ?? null) as RoomAvailabilityCalendar | null;
    const availabilityLoading = !availabilityCalendar && (availabilityResult.isLoading || availabilityResult.isFetching);
    const availabilityError = availabilityResult.isError ? t("roomDetails.request.errors.loadAvailability") : "";
    const initialDateRangeKey = initialDateRange?.from && initialDateRange.to
        ? `${format(initialDateRange.from, "yyyy-MM-dd")}:${format(initialDateRange.to, "yyyy-MM-dd")}`
        : "";

    useEffect(() => {
        if (!authenticated || !userId) {
            setRoomOfferedId("");
            return;
        }

        if (myRoomsQuery.isError) {
            setRoomOfferedId("");
            setRequestMode("money");
            setRequestError(getApiErrorMessage(myRoomsQuery.error) || t("roomDetails.request.errors.loadRooms"));
            return;
        }

        if (!myRoomsQuery.data) return;

        const selectableRooms = Number.isFinite(roomId)
            ? myRooms.filter((userRoom) => userRoom.id !== roomId)
            : myRooms;

        if (selectableRooms.length === 0) {
            setRequestMode("money");
            setRoomOfferedId("");
            return;
        }

        setRoomOfferedId((current) => (
            selectableRooms.some((userRoom) => String(userRoom.id) === current)
                ? current
                : String(selectableRooms[0].id)
        ));
    }, [authenticated, myRooms, myRoomsQuery.data, myRoomsQuery.error, myRoomsQuery.isError, roomId, t, userId]);

    useEffect(() => {
        setCalendarMonth(getInitialAvailabilityMonth(initialDateRange, today));
    }, [initialDateRangeKey, initialDateRange, today]);

    useEffect(() => {
        if (!initialDateRangeKey || appliedInitialDateRangeKey === initialDateRangeKey) {
            return;
        }

        if (!availabilityCalendar || availabilityLoading || availabilityError) {
            return;
        }

        setAppliedInitialDateRangeKey(initialDateRangeKey);
        if (isRangeSelectable(initialDateRange, availabilityCalendar, today)) {
            setDateRange(initialDateRange);
        }
    }, [
        appliedInitialDateRangeKey,
        availabilityCalendar,
        availabilityError,
        availabilityLoading,
        initialDateRange,
        initialDateRangeKey,
        today,
    ]);

    const checkIn = dateRange?.from ? format(dateRange.from, "yyyy-MM-dd") : "";
    const checkOut = dateRange?.to ? format(dateRange.to, "yyyy-MM-dd") : "";
    const ownerId = parseIdFromUri(room?.owner);
    const isOwnRoom = authenticated && !authLoading && (
        (ownerId !== null && userId === ownerId)
        || (Number.isFinite(roomId) && myRooms.some((myRoom) => myRoom.id === roomId))
    );
    const offerableRooms = Number.isFinite(roomId)
        ? myRooms.filter((myRoom) => myRoom.id !== roomId)
        : myRooms;
    const nights = countDays(checkIn, checkOut);
    const suggestedMoneyOffer = nights > 0 ? Number(room?.dayPrice || 0) * nights : 0;
    const moneyOfferValue = Number(moneyOffer);
    const hasValidDateRange = Boolean(checkIn && checkOut && nights > 0);
    const availabilityReady = Boolean(availabilityCalendar) && !availabilityLoading && !availabilityError;
    const hasAvailableStay = availabilityReady && hasSelectableStay(availabilityCalendar);
    const hasSelectableDateRange = hasValidDateRange && isRangeSelectable(dateRange, availabilityCalendar, today);
    const hasOfferableRooms = offerableRooms.length > 0;
    const canSubmitRequest = authenticated
        && verified
        && !isOwnRoom
        && hasSelectableDateRange
        && hasAvailableStay
        && (requestMode === "swap"
            ? Boolean(roomOfferedId) && hasOfferableRooms && !myRoomsLoading
            : Number.isFinite(moneyOfferValue) && moneyOfferValue > 0)
        && !submittingRequest
        && !authLoading;

    const setDateRangeAndClearError = (range: DateRange | undefined) => {
        setDateRange(range);
        setRequestError("");
    };

    const setRequestModeAndClearError = (mode: RequestMode) => {
        setRequestMode(mode);
        setRequestError("");
    };

    const setRoomOfferedIdAndClearError = (value: string) => {
        setRoomOfferedId(value);
        setRequestError("");
    };

    const setMoneyOfferAndClearError = (value: string) => {
        setMoneyOffer(value);
        setRequestError("");
    };

    const validateRequest = () => {
        if (!checkIn || !checkOut) {
            return t("roomDetails.request.errors.missingDates");
        }

        if (nights <= 0) {
            return t("roomDetails.request.errors.invalidDateRange");
        }

        if (availabilityLoading) {
            return t("roomDetails.request.errors.availabilityLoading");
        }

        if (availabilityError || !availabilityCalendar) {
            return t("roomDetails.request.errors.loadAvailability");
        }

        if (!hasSelectableStay(availabilityCalendar)) {
            return t("roomDetails.request.errors.noAvailability");
        }

        if (!isRangeSelectable(dateRange, availabilityCalendar, today)) {
            return t("roomDetails.request.errors.unavailableDates");
        }

        const parsedCheckIn = parseApiDate(checkIn);
        if (!parsedCheckIn || parsedCheckIn < today) {
            return t("roomDetails.request.errors.pastDate");
        }

        if (requestMode === "swap" && !roomOfferedId) {
            return t("roomDetails.request.errors.missingOfferedRoom");
        }

        if (requestMode === "money" && (!Number.isFinite(moneyOfferValue) || moneyOfferValue <= 0)) {
            return t("roomDetails.request.errors.invalidMoney");
        }

        return "";
    };

    const buildPayload = (): CreateContactPayload => ({
        roomRequestedId: Number(roomId),
        checkIn,
        checkOut,
        isSwap: requestMode === "swap",
        ...(requestMode === "swap"
            ? { roomOfferedId: Number(roomOfferedId) }
            : { dayPrice: moneyOfferValue }),
    });

    const getSelectedMatchingTrip = () => (
        matchingTrips.find((trip) => trip.tripId === selectedMatchingTripId) ?? matchingTrips[0] ?? null
    );

    const navigateAfterSubmit = (linkedTrip?: GroupTripAssociationDTO | null) => {
        if (linkedTrip?.tripId && linkedTrip.id) {
            navigate(`/trips/${linkedTrip.id}/contacts?destinationId=${linkedTrip.tripId}`);
            return;
        }

        navigate("/swaps?view=sent&page=1");
    };

    // Creates the contact. When a trip is passed, the backend links the contact to that trip.
    const submitContact = async (linkTrip?: GroupTripAssociationDTO | null) => {
        setSubmittingRequest(true);
        setRequestError("");

        try {
            await createContactMutation.mutateAsync({ contactData: buildPayload(), tripId: linkTrip?.tripId });
            toast.success(
                linkTrip?.tripId !== undefined
                    ? t("roomDetails.request.successAddedToTrip")
                    : t("roomDetails.request.success")
            );
            navigateAfterSubmit(linkTrip);
        } catch (error) {
            const message = getApiErrorMessage(error) || t("roomDetails.request.errors.submit");
            setRequestError(message);
            toast.error(message);
        } finally {
            setSubmittingRequest(false);
        }
    };

    const handleSubmitRequest = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!authenticated) {
            goToLogin();
            return;
        }

        if (isOwnRoom) {
            setRequestError(t("roomDetails.request.ownRoomMessage"));
            return;
        }

        if (!verified) {
            setRequestError(t("roomDetails.request.verifyMessage"));
            return;
        }

        const validationError = validateRequest();
        if (validationError) {
            setRequestError(validationError);
            return;
        }

        setSubmittingRequest(true);
        setRequestError("");

        let matches: GroupTripAssociationDTO[] = [];
        try {
            if (userId && room?.country) {
                const matchResponse = await findGroupTripsForAssociation({
                    userId,
                    page: 1,
                    pageSize: 100,
                    filters: {
                        country: room.country,
                        checkIn,
                        checkOut,
                    },
                });
                matches = matchResponse.data;
            }
        } catch {
            matches = [];
        }

        if (matches.length > 0) {
            const defaultMatch = (
                requestTripId
                    ? matches.find((trip) => trip.tripId === requestTripId)
                    : undefined
            ) ?? matches[0];
            setMatchingTrips(matches);
            setSelectedMatchingTripId(defaultMatch.tripId);
            setAddToTripDialogOpen(true);
            setSubmittingRequest(false);
            return;
        }

        await submitContact(null);
    };

    const resetAddToTripDialog = () => {
        setAddToTripDialogOpen(false);
        setMatchingTrips([]);
        setSelectedMatchingTripId(undefined);
    };

    const confirmAddToTrip = async () => {
        const selectedTrip = getSelectedMatchingTrip();
        resetAddToTripDialog();
        await submitContact(selectedTrip);
    };

    const cancelAddToTrip = async () => {
        resetAddToTripDialog();
        await submitContact(null);
    };

    return {
        availabilityCalendar,
        availabilityError,
        availabilityLoading,
        availabilityReady,
        canSubmitRequest,
        calendarMonth,
        checkIn,
        checkOut,
        datePopoverOpen,
        dateRange,
        formatCurrencyAmount,
        formatMoneyInputValue,
        hasAvailableStay,
        hasOfferableRooms,
        hasSelectableDateRange,
        handleSubmitRequest,
        isOwnRoom,
        moneyOffer,
        myRoomsLoading,
        nights,
        normalizeMoneyInput,
        offerableRooms,
        ownerId,
        requestError,
        requestMode,
        roomOfferedId,
        setDatePopoverOpen,
        setDateRange: setDateRangeAndClearError,
        setCalendarMonth,
        setMoneyOffer: setMoneyOfferAndClearError,
        setRequestError,
        setRequestMode: setRequestModeAndClearError,
        setRoomOfferedId: setRoomOfferedIdAndClearError,
        submittingRequest,
        suggestedMoneyOffer,
        today,
        refetchAvailability: availabilityResult.refetch,
        addToTripDialogOpen,
        setAddToTripDialogOpen,
        matchingTrips,
        selectedMatchingTripId,
        setSelectedMatchingTripId,
        confirmAddToTrip,
        cancelAddToTrip,
    };
}
