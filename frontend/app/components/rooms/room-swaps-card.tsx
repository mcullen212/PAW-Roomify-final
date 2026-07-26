import { format } from "date-fns";
import { Calendar as CalendarIcon, DollarSign, Info, LogIn, Repeat2, Send } from "lucide-react";
import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { cn } from "@/lib/utils";
import {
    isDateInsideRanges,
    isDateSelectable,
    isRangeSelectable,
} from "@/lib/room-availability";
import {
    requestCurrencyFormatter,
    type RoomRequestFormState,
} from "~/lib/forms/useRoomRequestForm.ts";
import { TripMismatchModal } from "@/components/modals/trip-mismatch-modal.tsx";

type RoomBookingCardProps = {
    authLoading: boolean;
    authenticated: boolean;
    email?: string | null;
    goToLogin: () => void;
    requestForm: RoomRequestFormState;
    roomDayPrice: number;
    verified: boolean;
};

export function RoomSwapsCard({
    authLoading,
    authenticated,
    email,
    goToLogin,
    requestForm,
    roomDayPrice,
    verified,
}: RoomBookingCardProps) {
    const { t } = useTranslation();

    const selectedMatchingTrip = requestForm.matchingTrips.find(
        (trip) => trip.tripId === requestForm.selectedMatchingTripId,
    ) ?? requestForm.matchingTrips[0];
    const formatIsoDate = (iso: string) => {
        const [year, month, day] = iso.split("-").map(Number);
        if (!year || !month || !day) return iso;
        return format(new Date(year, month - 1, day), "LLL dd, yyyy");
    };
    const matchingTripLabel = selectedMatchingTrip
        ? `${selectedMatchingTrip.title ? `"${selectedMatchingTrip.title}" · ` : ""}${formatIsoDate(selectedMatchingTrip.tripStartDate)} – ${formatIsoDate(selectedMatchingTrip.tripEndDate)}`
        : "";

    return (
        <div className="lg:col-span-1">
            <Card className="sticky top-24 border-2">
                <CardContent className="p-6 space-y-6">
                    <div className="text-3xl font-bold">
                        ${roomDayPrice}<span className="text-lg font-normal text-muted-foreground">{t("roomDetails.pricePerDay")}</span>
                    </div>

                    {authLoading ? (
                        <div className="h-28 animate-pulse rounded-md bg-muted/40" />
                    ) : !authenticated ? (
                        <div className="space-y-4">
                            <p className="text-sm text-muted-foreground">{t("roomDetails.request.loginMessage")}</p>
                            <Button className="w-full gap-2" size="lg" type="button" onClick={goToLogin}>
                                <LogIn className="h-4 w-4" />
                                {t("roomDetails.request.loginCta")}
                            </Button>
                        </div>
                    ) : requestForm.isOwnRoom ? (
                        <div className="space-y-4">
                            <div className="rounded-md border border-blue-200 bg-blue-50 p-4 text-sm text-blue-900">
                                <div className="flex items-start gap-3">
                                    <Info className="mt-0.5 h-5 w-5 shrink-0" />
                                    <p>{t("roomDetails.request.ownRoomMessage")}</p>
                                </div>
                            </div>
                            <Button className="w-full" variant="outline" asChild>
                                <Link to="/my-rooms">{t("roomDetails.request.manageRooms")}</Link>
                            </Button>
                        </div>
                    ) : !verified ? (
                        <div className="space-y-4">
                            <div className="rounded-md border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">
                                <div className="flex items-start gap-3">
                                    <Info className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
                                    <div className="space-y-2">
                                        <p>{t("roomDetails.request.verifyMessage")}</p>
                                    </div>
                                </div>
                            </div>
                            <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
                                <Button className="w-full" asChild>
                                    <Link to={`/verify-token?${new URLSearchParams({
                                        type: "verify",
                                        ...(email ? { email } : {}),
                                    }).toString()}`}>
                                        {t("popUps.verification.enter_code")}
                                    </Link>
                                </Button>
                                <Button className="w-full" variant="outline" asChild>
                                    <Link to="/profile">{t("roomDetails.request.verifyProfile")}</Link>
                                </Button>
                            </div>
                        </div>
                    ) : (
                        <form className="space-y-5" onSubmit={requestForm.handleSubmitRequest}>
                            <div className="space-y-2">
                                <Label>{t("roomDetails.request.dates")}</Label>
                                <Popover open={requestForm.datePopoverOpen} onOpenChange={requestForm.setDatePopoverOpen}>
                                    <PopoverTrigger asChild>
                                        <Button
                                            className={cn(
                                                "h-12 w-full justify-start border bg-muted/50 px-4 text-left font-normal transition-colors hover:bg-muted/70 enabled:cursor-pointer",
                                                !requestForm.dateRange?.from && "text-muted-foreground",
                                            )}
                                            disabled={requestForm.availabilityLoading || Boolean(requestForm.availabilityError) || !requestForm.hasAvailableStay}
                                            type="button"
                                            variant="outline"
                                        >
                                            <CalendarIcon className="h-5 w-5 shrink-0 text-muted-foreground" />
                                            <span className="min-w-0 flex-1 truncate">
                                                {requestForm.dateRange?.from ? (
                                                    requestForm.dateRange.to ? (
                                                        `${format(requestForm.dateRange.from, "LLL dd, yyyy")} - ${format(requestForm.dateRange.to, "LLL dd, yyyy")}`
                                                    ) : (
                                                        format(requestForm.dateRange.from, "LLL dd, yyyy")
                                                    )
                                                ) : (
                                                    t("roomDetails.request.selectDates")
                                                )}
                                            </span>
                                        </Button>
                                    </PopoverTrigger>
                                    <PopoverContent className="w-auto max-w-[calc(100vw-2rem)] p-0" align="start">
                                        <Calendar
                                            autoFocus
                                            mode="range"
                                            month={requestForm.calendarMonth}
                                            onMonthChange={requestForm.setCalendarMonth}
                                            selected={requestForm.dateRange}
                                            disabled={(date) => requestForm.availabilityLoading || !isDateSelectable(date, requestForm.availabilityCalendar, requestForm.today)}
                                            modifiers={{
                                                booked: (date) => Boolean(
                                                    requestForm.availabilityCalendar
                                                    && isDateInsideRanges(date, requestForm.availabilityCalendar.bookedRanges)
                                                ),
                                            }}
                                            modifiersClassNames={{
                                                booked: "line-through decoration-2 decoration-destructive/70",
                                            }}
                                            excludeDisabled
                                            onSelect={(range) => {
                                                if (requestForm.availabilityLoading) {
                                                    return;
                                                }

                                                if (range?.from && range?.to && !isRangeSelectable(range, requestForm.availabilityCalendar, requestForm.today)) {
                                                    requestForm.setDateRange(undefined);
                                                    requestForm.setRequestError(t("roomDetails.request.errors.unavailableDates"));
                                                    return;
                                                }

                                                requestForm.setDateRange(range);
                                                if (range?.from && range?.to && range.from.getTime() !== range.to.getTime()) {
                                                    setTimeout(() => requestForm.setDatePopoverOpen(false), 200);
                                                }
                                            }}
                                            min={1}
                                            footer={
                                                <div className="flex justify-end border-t border-border p-3">
                                                    <Button
                                                        className="cursor-pointer text-xs text-muted-foreground hover:text-primary"
                                                        size="sm"
                                                        type="button"
                                                        variant="ghost"
                                                        onClick={() => requestForm.setDateRange(undefined)}
                                                    >
                                                        {t("roomDetails.request.resetDates")}
                                                    </Button>
                                                </div>
                                            }
                                        />
                                    </PopoverContent>
                                </Popover>
                                {requestForm.availabilityLoading && (
                                    <p className="text-sm text-muted-foreground">
                                        {t("roomDetails.request.loadingAvailability")}
                                    </p>
                                )}
                                {!requestForm.availabilityLoading && requestForm.availabilityError && (
                                    <div className="flex items-center justify-between gap-3 rounded-md border border-destructive/30 bg-destructive/5 p-3">
                                        <p className="text-sm text-destructive" role="alert">
                                            {requestForm.availabilityError}
                                        </p>
                                        <Button
                                            type="button"
                                            variant="outline"
                                            size="sm"
                                            onClick={() => requestForm.refetchAvailability()}
                                        >
                                            {t("roomDetails.request.retryAvailability")}
                                        </Button>
                                    </div>
                                )}
                                {!requestForm.availabilityLoading && requestForm.availabilityReady && !requestForm.hasAvailableStay && (
                                        <p className="text-sm text-muted-foreground">
                                            {t("roomDetails.request.noSelectableDates")}
                                        </p>
                                    )}
                                </div>

                            <div className="grid grid-cols-2 gap-2 rounded-lg bg-muted/50 p-1">
                                <button
                                    className={cn(
                                        "flex min-h-10 cursor-pointer items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-semibold transition-colors hover:bg-background/80 hover:text-foreground disabled:cursor-not-allowed disabled:hover:bg-transparent",
                                        requestForm.requestMode === "swap"
                                            ? "bg-background text-foreground shadow-sm hover:bg-background"
                                            : "text-muted-foreground hover:text-foreground",
                                        (!requestForm.hasOfferableRooms || requestForm.myRoomsLoading) && "opacity-60",
                                    )}
                                    disabled={!requestForm.hasOfferableRooms || requestForm.myRoomsLoading}
                                    onClick={() => requestForm.setRequestMode("swap")}
                                    type="button"
                                >
                                    <Repeat2 className="h-4 w-4" />
                                    {t("roomDetails.request.roomSwap")}
                                </button>
                                <button
                                    className={cn(
                                        "flex min-h-10 cursor-pointer items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-semibold transition-colors hover:bg-background/80 hover:text-foreground",
                                        requestForm.requestMode === "money"
                                            ? "bg-background text-foreground shadow-sm hover:bg-background"
                                            : "text-muted-foreground hover:text-foreground",
                                    )}
                                    onClick={() => requestForm.setRequestMode("money")}
                                    type="button"
                                >
                                    <DollarSign className="h-4 w-4" />
                                    {t("roomDetails.request.moneyOffer")}
                                </button>
                            </div>

                            {requestForm.requestMode === "swap" ? (
                                <div className="space-y-2">
                                    <Label htmlFor="room-offered">{t("roomDetails.request.offeredRoom")}</Label>
                                    <Select
                                        disabled={requestForm.myRoomsLoading || !requestForm.hasOfferableRooms}
                                        value={requestForm.roomOfferedId}
                                        onValueChange={requestForm.setRoomOfferedId}
                                    >
                                        <SelectTrigger className="w-full cursor-pointer bg-muted/50 transition-colors hover:bg-muted/70 disabled:cursor-not-allowed disabled:hover:bg-transparent" id="room-offered">
                                            <SelectValue placeholder={requestForm.myRoomsLoading ? t("roomDetails.request.loadingRooms") : t("roomDetails.request.selectRoom")} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {requestForm.offerableRooms.map((myRoom) => (
                                                <SelectItem className="cursor-pointer" key={myRoom.id} value={String(myRoom.id)}>
                                                    {myRoom.title}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                            ) : (
                                <div className="space-y-3">
                                    <div className="space-y-2">
                                        <Label htmlFor="money-offer">{t("roomDetails.request.amount")}</Label>
                                        <div className="relative">
                                            <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-sm text-muted-foreground">
                                                $
                                            </span>
                                            <Input
                                                id="money-offer"
                                                className="pl-8"
                                                inputMode="decimal"
                                                min="1"
                                                type="text"
                                                value={requestForm.formatMoneyInputValue(requestForm.moneyOffer)}
                                                onChange={(event) => requestForm.setMoneyOffer(requestForm.normalizeMoneyInput(event.target.value))}
                                                placeholder={requestForm.suggestedMoneyOffer > 0 ? requestCurrencyFormatter.format(requestForm.suggestedMoneyOffer) : undefined}
                                            />
                                        </div>
                                    </div>
                                    {requestForm.suggestedMoneyOffer > 0 && (
                                        <div className="flex items-center justify-between gap-3 rounded-md border border-border bg-muted/30 px-3 py-2 text-sm">
                                            <span className="text-muted-foreground">
                                                {t("roomDetails.request.suggestedAmount", {
                                                    amount: requestForm.formatCurrencyAmount(requestForm.suggestedMoneyOffer),
                                                    nights: requestForm.nights,
                                                })}
                                            </span>
                                            <Button
                                                className="h-8 shrink-0 cursor-pointer"
                                                type="button"
                                                variant="outline"
                                                onClick={() => requestForm.setMoneyOffer(String(requestForm.suggestedMoneyOffer))}
                                            >
                                                {t("roomDetails.request.useSuggested")}
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            )}

                            {!requestForm.myRoomsLoading && !requestForm.hasOfferableRooms && (
                                <p className="text-sm text-muted-foreground">{t("roomDetails.request.noRooms")}</p>
                            )}

                            {requestForm.requestError && (
                                <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                                    {requestForm.requestError}
                                </div>
                            )}

                            <Button className="w-full cursor-pointer gap-2 disabled:cursor-not-allowed" disabled={!requestForm.canSubmitRequest} size="lg" type="submit">
                                <Send className="h-4 w-4" />
                                {requestForm.submittingRequest ? t("roomDetails.request.submitting") : t("roomDetails.request.submit")}
                            </Button>
                        </form>
                    )}
                </CardContent>
            </Card>

            <TripMismatchModal
                open={requestForm.addToTripDialogOpen}
                onOpenChange={(open) => {
                    if (!open) {
                        requestForm.cancelAddToTrip();
                    } else {
                        requestForm.setAddToTripDialogOpen(true);
                    }
                }}
                onConfirm={requestForm.confirmAddToTrip}
                title={t("roomDetails.request.addToTrip.title")}
                description={t("roomDetails.request.addToTrip.description")}
                tripLabel={matchingTripLabel}
                confirmLabel={t("roomDetails.request.addToTrip.confirm")}
                cancelLabel={t("roomDetails.request.addToTrip.cancel")}
                matchingTrips={requestForm.matchingTrips}
                selectedTripId={requestForm.selectedMatchingTripId}
                onSelectedTripIdChange={requestForm.setSelectedMatchingTripId}
            />
        </div>
    );
}
