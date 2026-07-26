import { Link } from "react-router";
import { Calendar as CalendarIcon, Home, Star, Trash2, User } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { formatApiDate } from "@/lib/utils";
import type { Review } from "@/lib/interfaces/reviews";

type ReviewCardProps = {
    review: Review;
    onDelete?: (review: Review) => void;
    isDeleting?: boolean;
    showRoomLink?: boolean;
};

export function ReviewCard({ review, onDelete, isDeleting = false, showRoomLink = true }: ReviewCardProps) {
    const { t } = useTranslation();
    const reviewerName = review.reviewerName || t("roomDetails.fallback.guest");
    const roomTitle = review.roomId
        ? review.roomTitle || t("swaps.cards.roomFallback", { id: review.roomId })
        : null;

    return (
        <Card className="rounded-lg border border-border shadow-sm">
            <CardContent className="p-4">
                <div className="flex flex-col gap-3 border-b border-border pb-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="flex min-w-0 items-start gap-2.5">
                        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-blue-700 text-white">
                            <User className="h-5 w-5" />
                        </div>
                        <div className="min-w-0">
                            <p className="break-words text-base font-semibold leading-tight text-foreground">
                                {reviewerName}
                            </p>
                            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
                                <CalendarIcon className="h-3.5 w-3.5" />
                                {formatApiDate(review.date)}
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="flex items-center gap-0.5" aria-label={t("roomDetails.ratingStarsLabel", { rating: review.rating })}>
                            {Array.from({ length: 5 }, (_, index) => {
                                const filled = index < Math.round(review.rating);
                                return (
                                    <Star
                                        key={index}
                                        className={`h-4 w-4 ${filled ? "fill-amber-400 text-amber-400" : "text-amber-400"}`}
                                    />
                                );
                            })}
                        </div>
                        {onDelete ? (
                            <Button
                                type="button"
                                size="icon-sm"
                                variant="destructive"
                                disabled={isDeleting}
                                aria-label={t("profileReviews.delete.action")}
                                title={t("profileReviews.delete.action")}
                                onClick={() => onDelete(review)}
                                className="rounded-md shadow-sm"
                            >
                                <Trash2 className="size-3.5" />
                            </Button>
                        ) : null}
                    </div>
                </div>

                <p className="py-3 text-sm leading-6 text-slate-700">
                    {review.comment}
                </p>
                {showRoomLink && review.roomId && roomTitle ? (
                    <div className="border-t border-border pt-3">
                        <p className="flex items-start gap-1.5 text-xs text-muted-foreground">
                            <Home className="mt-0.5 h-3.5 w-3.5 shrink-0" />
                            <span className="shrink-0">{t("reviewActions.room")}</span>
                            <Link
                                className="min-w-0 break-words font-semibold text-[#2563eb] transition-colors hover:text-[#1d4ed8] hover:underline focus-visible:rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2563eb] focus-visible:ring-offset-2"
                                to={`/room/${review.roomId}`}
                            >
                                {roomTitle}
                            </Link>
                        </p>
                    </div>
                ) : null}
            </CardContent>
        </Card>
    );
}
