import { AlertCircle, Star } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { Contact } from "@/lib/interfaces/contacts";
import { Button } from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import type { RoomSummary } from "~/lib/interfaces/swaps";
import { formatRange } from "~/lib/swaps/swaps-utils";
import { cn } from "@/lib/utils";

interface ReviewDialogProps {
    contact: Contact | null;
    requestedRoom?: RoomSummary;
    offeredRoom?: RoomSummary;
    userId?: number | null;
    rating: number | null;
    comment: string;
    error: string | null;
    isSubmitting: boolean;
    onOpenChange: (open: boolean) => void;
    onRatingChange: (rating: number) => void;
    onCommentChange: (comment: string) => void;
    onSubmit: () => void;
}

export function ReviewDialog({
    contact,
    requestedRoom,
    offeredRoom,
    userId,
    rating,
    comment,
    error,
    isSubmitting,
    onOpenChange,
    onRatingChange,
    onCommentChange,
    onSubmit,
}: ReviewDialogProps) {
    const { t } = useTranslation();

    if (!contact) {
        return null;
    }

    const isRequestedRoomOwner = Boolean(
        userId && (contact.roomRequestedOwnerId ?? requestedRoom?.ownerId) === userId,
    );
    const reviewedRoom = contact.isSwap && isRequestedRoomOwner ? offeredRoom : requestedRoom;
    const reviewedRange = contact.isSwap && isRequestedRoomOwner ? contact.offeredRange : contact.requestedRange;
    const reviewedFallbackId = contact.isSwap && isRequestedRoomOwner && contact.roomOfferedId
        ? contact.roomOfferedId
        : contact.roomRequestedId;

    return (
        <Dialog open={Boolean(contact)} onOpenChange={onOpenChange}>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>{t("reviewActions.title")}</DialogTitle>
                    <DialogDescription>
                        {t("reviewActions.description")}
                    </DialogDescription>
                </DialogHeader>

                <div className="grid gap-4">
                    <div className="rounded-md border border-border bg-muted/30 p-4">
                        <p className="text-xs font-semibold uppercase text-muted-foreground">
                            {t("reviewActions.reviewing")}
                        </p>
                        <h3 className="mt-1 text-base font-semibold text-foreground">
                            {reviewedRoom?.title ?? t("swaps.cards.roomFallback", {
                                id: reviewedFallbackId,
                            })}
                        </h3>
                        <p className="mt-2 text-sm text-muted-foreground">
                            {formatRange(reviewedRange)}
                        </p>
                    </div>

                    <div className="grid gap-2">
                        <Label className="text-sm font-semibold text-foreground">
                            {t("reviewActions.rating")}
                        </Label>
                        <div
                            aria-label={t("reviewActions.ratingAria")}
                            className="flex items-center gap-1"
                            role="radiogroup"
                        >
                            {[1, 2, 3, 4, 5].map((value) => {
                                const selected = Boolean(rating && value <= rating);

                                return (
                                    <button
                                        aria-checked={rating === value}
                                        aria-label={t("reviewActions.starLabel", { count: value })}
                                        className="rounded-md p-1 text-amber-500 transition-colors hover:bg-amber-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                                        disabled={isSubmitting}
                                        key={value}
                                        onClick={() => onRatingChange(value)}
                                        role="radio"
                                        type="button"
                                    >
                                        <Star
                                            className={cn(
                                                "h-7 w-7",
                                                selected ? "fill-amber-400" : "fill-transparent text-muted-foreground",
                                            )}
                                            aria-hidden="true"
                                        />
                                    </button>
                                );
                            })}
                        </div>
                    </div>

                    <div className="grid gap-2">
                        <Label className="text-sm font-semibold text-foreground" htmlFor="review-comment">
                            {t("reviewActions.comment")}
                        </Label>
                        <Textarea
                            className="min-h-32 resize-none"
                            disabled={isSubmitting}
                            id="review-comment"
                            maxLength={500}
                            onChange={(event) => onCommentChange(event.target.value)}
                            placeholder={t("reviewActions.commentPlaceholder")}
                            value={comment}
                        />
                        <p className="text-xs text-muted-foreground">
                            {t("reviewActions.characters", { count: comment.length })}
                        </p>
                    </div>

                    {error ? (
                        <div className="flex gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive">
                            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                            <p>{error}</p>
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
                    <Button
                        className="bg-[#2563eb] text-white hover:bg-[#1d4ed8] hover:shadow-md"
                        disabled={isSubmitting}
                        onClick={onSubmit}
                        type="button"
                    >
                        {isSubmitting ? t("reviewActions.submitting") : t("reviewActions.submit")}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
