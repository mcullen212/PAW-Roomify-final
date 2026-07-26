import { format } from "date-fns";
import { AlertCircle, CalendarDays } from "lucide-react";
import type { DateRange } from "react-day-picker";
import { useTranslation } from "react-i18next";
import type { Contact } from "@/lib/interfaces/contacts";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import type { ContactActionMode, RoomSummary } from "~/lib/interfaces/swaps";
import {
    formatMoney,
    formatRange,
    getRequestedNights,
    getSelectedNights,
    isSwapAcceptDateDisabled,
    validateSwapAcceptanceRange,
} from "~/lib/swaps/swaps-utils";
import { cn } from "@/lib/utils";
import {
    getAvailabilityDefaultMonth,
    isDateInsideRanges,
} from "@/lib/room-availability";

interface ContactActionDialogProps {
    actionMode: ContactActionMode | null;
    contact: Contact | null;
    requestedRoom?: RoomSummary;
    offeredRoom?: RoomSummary;
    selectedRange?: DateRange;
    datePopoverOpen: boolean;
    actionError: string | null;
    isSubmitting: boolean;
    onDatePopoverOpenChange: (open: boolean) => void;
    onOpenChange: (open: boolean) => void;
    onRangeChange: (range: DateRange | undefined) => void;
    onAccept: () => void;
    onReject: () => void;
    onCancel: () => void;
}

export function ContactActionDialog({
    actionMode,
    contact,
    requestedRoom,
    offeredRoom,
    selectedRange,
    datePopoverOpen,
    actionError,
    isSubmitting,
    onDatePopoverOpenChange,
    onOpenChange,
    onRangeChange,
    onAccept,
    onReject,
    onCancel,
}: ContactActionDialogProps) {
    const { t } = useTranslation();
    const open = Boolean(actionMode && contact);

    if (!contact) {
        return null;
    }

    const isAccept = actionMode === "accept";
    const isCancel = actionMode === "cancel";
    const isSwapAccept = isAccept && contact.isSwap;
    const requestedNights = getRequestedNights(contact);
    const selectedRangeLabel = selectedRange?.from
        ? selectedRange.to
            ? `${format(selectedRange.from, "LLL dd, yyyy")} - ${format(selectedRange.to, "LLL dd, yyyy")}`
            : format(selectedRange.from, "LLL dd, yyyy")
        : t("swapActions.selectDates");
    const selectedNights = selectedRange?.from && selectedRange.to
        ? getSelectedNights(selectedRange)
        : 0;
    const selectedStayTooLong = isSwapAccept
        && requestedNights > 0
        && selectedNights > requestedNights;
    const defaultCalendarMonth = getAvailabilityDefaultMonth(offeredRoom?.availabilityCalendar, selectedRange)
        ?? undefined;
    const canSubmitAccept = !isSwapAccept || !validateSwapAcceptanceRange(contact, selectedRange, offeredRoom);

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-2xl">
                <DialogHeader>
                    <DialogTitle>
                        {isAccept
                            ? t("swapActions.acceptTitle")
                            : isCancel
                                ? t("swapActions.cancelTitle")
                                : t("swapActions.rejectTitle")}
                    </DialogTitle>
                    <DialogDescription>
                        {isAccept
                            ? t("swapActions.acceptDescription")
                            : isCancel
                                ? t("swapActions.cancelDescription")
                                : t("swapActions.rejectDescription")}
                    </DialogDescription>
                </DialogHeader>

                <div className="grid gap-4">
                    <ActionSummary
                        contact={contact}
                        requestedRoom={requestedRoom}
                        offeredRoom={offeredRoom}
                    />

                    {isSwapAccept ? (
                        <div className="rounded-md border border-border bg-muted/30 p-4">
                            <Label className="text-sm font-semibold text-foreground">
                                {t("swapActions.offeredRoomDates")}
                            </Label>
                            <Popover open={datePopoverOpen} onOpenChange={onDatePopoverOpenChange}>
                                <PopoverTrigger asChild>
                                    <Button
                                        className={cn(
                                            "mt-3 h-11 w-full justify-start border bg-background text-left font-normal hover:border-primary/50 hover:bg-muted/50 hover:text-foreground",
                                            !selectedRange?.from && "text-muted-foreground",
                                        )}
                                        disabled={isSubmitting}
                                        type="button"
                                        variant="outline"
                                    >
                                        <CalendarDays className="h-4 w-4" aria-hidden="true" />
                                        <span className="truncate">{selectedRangeLabel}</span>
                                    </Button>
                                </PopoverTrigger>
                                <PopoverContent align="start" className="w-auto max-w-[calc(100vw-2rem)] p-0">
                                    <Calendar
                                        autoFocus
                                        mode="range"
                                        defaultMonth={defaultCalendarMonth}
                                        selected={selectedRange}
                                        disabled={(date) => isSwapAcceptDateDisabled(date, offeredRoom)}
                                        modifiers={{
                                            booked: (date) => Boolean(
                                                offeredRoom?.availabilityCalendar
                                                && isDateInsideRanges(date, offeredRoom.availabilityCalendar.bookedRanges)
                                            ),
                                        }}
                                        modifiersClassNames={{
                                            booked: "line-through decoration-2 decoration-destructive/70",
                                        }}
                                        excludeDisabled
                                        onSelect={onRangeChange}
                                        min={1}
                                        footer={
                                            <div className="flex items-center justify-between gap-3 border-t border-border p-3 text-xs text-muted-foreground">
                                                <span>{t("swapActions.nightMaximum", { count: requestedNights })}</span>
                                                <Button
                                                    className="text-xs text-muted-foreground hover:bg-accent hover:text-primary"
                                                    disabled={isSubmitting}
                                                    onClick={() => onRangeChange(undefined)}
                                                    size="sm"
                                                    type="button"
                                                    variant="ghost"
                                                >
                                                    {t("swapActions.resetDates")}
                                                </Button>
                                            </div>
                                        }
                                    />
                                </PopoverContent>
                            </Popover>
                            {selectedStayTooLong ? (
                                <div className="mt-3 flex gap-2 rounded-md border border-amber-200 bg-amber-50 p-3 text-sm text-amber-950">
                                    <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" aria-hidden="true" />
                                    <p>
                                        {t("swapActions.stayTooLong", {
                                            count: requestedNights,
                                            selected: selectedNights,
                                        })}
                                    </p>
                                </div>
                            ) : null}
                        </div>
                    ) : null}

                    {actionError ? (
                        <div className="flex gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                            <p>{actionError}</p>
                        </div>
                    ) : null}
                </div>

                <DialogFooter>
                    <Button
                        className="hover:bg-secondary hover:text-primary"
                        disabled={isSubmitting}
                        onClick={() => onOpenChange(false)}
                        type="button"
                        variant="ghost"
                    >
                        {t("swapActions.cancel")}
                    </Button>
                    {isAccept ? (
                        <Button
                            className="bg-emerald-600 text-white hover:bg-emerald-700 hover:shadow-md"
                            disabled={isSubmitting || !canSubmitAccept}
                            onClick={onAccept}
                            type="button"
                        >
                            {isSubmitting ? t("swapActions.accepting") : t("swapActions.accept")}
                        </Button>
                    ) : isCancel ? (
                        <Button
                            className="hover:shadow-md"
                            disabled={isSubmitting}
                            onClick={onCancel}
                            type="button"
                            variant="destructive"
                        >
                            {isSubmitting ? t("swapActions.canceling") : t("swapActions.cancelSwap")}
                        </Button>
                    ) : (
                        <Button
                            className="hover:shadow-md"
                            disabled={isSubmitting}
                            onClick={onReject}
                            type="button"
                            variant="destructive"
                        >
                            {isSubmitting ? t("swapActions.rejecting") : t("swapActions.reject")}
                        </Button>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}

interface ActionSummaryProps {
    contact: Contact;
    requestedRoom?: RoomSummary;
    offeredRoom?: RoomSummary;
}

function ActionSummary({ contact, requestedRoom, offeredRoom }: ActionSummaryProps) {
    const { t } = useTranslation();

    return (
        <div className="grid gap-3 md:grid-cols-2">
            <SummaryItem
                label={t("swaps.dialog.requestedRoom")}
                title={requestedRoom?.title ?? t("swaps.cards.roomFallback", { id: contact.roomRequestedId })}
                detail={formatRange(contact.requestedRange)}
            />
            {contact.isSwap ? (
                <SummaryItem
                    label={t("swaps.dialog.offeredRoom")}
                    title={offeredRoom?.title ?? (contact.roomOfferedId ? t("swaps.cards.roomFallback", { id: contact.roomOfferedId }) : t("swaps.dialog.roomUnavailable"))}
                    detail={[
                        formatRange(contact.offeredRange),
                        contact.offerUserName ? t("swaps.dialog.offeredBy", { name: contact.offerUserName }) : t("swaps.contactType.swap"),
                    ].filter(Boolean).join(" - ")}
                />
            ) : (
                <SummaryItem
                    label={t("swaps.dialog.moneyOffer")}
                    title={formatMoney(contact.moneyOffer)}
                    detail={contact.offerUserName ? t("swaps.dialog.offeredBy", { name: contact.offerUserName }) : t("swaps.contactType.money")}
                />
            )}
        </div>
    );
}

interface SummaryItemProps {
    label: string;
    title: string;
    detail: string;
}

function SummaryItem({ label, title, detail }: SummaryItemProps) {
    return (
        <div className="rounded-md border border-border bg-card p-4">
            <p className="text-xs font-semibold uppercase text-muted-foreground">{label}</p>
            <h3 className="mt-1 text-base font-semibold text-foreground">{title}</h3>
            <p className="mt-2 text-sm text-muted-foreground">{detail}</p>
        </div>
    );
}
