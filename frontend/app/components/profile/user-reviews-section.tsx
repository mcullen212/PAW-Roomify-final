import { useEffect, useState } from "react";
import { MessageSquare } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";
import { PagingBar } from "@/components/PagingBar";
import { ConfirmDeleteModal } from "@/components/modals/confirm-delete-modal";
import { ReviewCard } from "@/components/reviews/review-card";
import { Card } from "@/components/ui/card";
import type { Review } from "@/lib/interfaces/reviews";
import { getPaginationWithFallback } from "~/lib/pagination";
import { useApiServices } from "~/lib/hooks/useApiServices";
import { useNormalizePaginationPage, usePaginationParams } from "~/lib/hooks/usePaginationParams";

const reviewsPageSize = 12;

type UserReviewsSectionProps = {
    userId: number;
    currentUserId?: number;
    source?: "written" | "received";
    allowDeleteOwnReviews?: boolean;
    pageParamName?: string;
    panelId?: string;
    labelledBy?: string;
    title: string;
    description?: string;
    emptyMessage: string;
    errorMessage: string;
};

export function UserReviewsSection({
    userId,
    currentUserId,
    source = "received",
    allowDeleteOwnReviews = false,
    pageParamName = "reviewsPage",
    panelId,
    labelledBy,
    title,
    description,
    emptyMessage,
    errorMessage,
}: UserReviewsSectionProps) {
    const { t } = useTranslation();
    const { reviewService } = useApiServices();
    const {
        currentPage: currentReviewsPage,
        setCurrentPage: setCurrentReviewsPage,
    } = usePaginationParams(pageParamName);
    const [reviews, setReviews] = useState<Review[]>([]);
    const [reviewToDelete, setReviewToDelete] = useState<Review | null>(null);

    const authoredReviewsQuery = reviewService.useGetReviewsByUser(
        source === "written" ? userId : undefined,
        currentReviewsPage,
        reviewsPageSize,
    );
    const receivedReviewsQuery = reviewService.useGetReviewsByRoomOwner(
        source === "received" ? userId : undefined,
        currentReviewsPage,
        reviewsPageSize,
    );
    const reviewsQuery = source === "written" ? authoredReviewsQuery : receivedReviewsQuery;
    const deleteReview = reviewService.useDeleteReview();
    const reviewPagination = getPaginationWithFallback(reviewsQuery.data?.pagination, currentReviewsPage);
    const reviewTotalPages = reviewPagination.totalPages;
    useNormalizePaginationPage(reviewsQuery.data?.pagination, currentReviewsPage, setCurrentReviewsPage);

    useEffect(() => {
        if (reviewsQuery.isError) {
            setReviews([]);
            return;
        }

        if (reviewsQuery.data) {
            setReviews(reviewsQuery.data.data);
        }
    }, [reviewsQuery.data, reviewsQuery.isError]);

    const handleConfirmDelete = async () => {
        if (!reviewToDelete) {
            return;
        }

        try {
            await deleteReview.mutateAsync(reviewToDelete.id);
            toast.success(t("profileReviews.delete.success"));
            setReviewToDelete(null);
        } catch (error) {
            toast.error(t("profileReviews.delete.error"));
        }
    };

    const reviewsLoading = reviewsQuery.isLoading || reviewsQuery.isFetching;
    const canDeleteReview = (review: Review) => (
        allowDeleteOwnReviews && Boolean(currentUserId) && review.reviewerId === currentUserId
    );

    return (
        <Card
            aria-labelledby={labelledBy}
            className="overflow-hidden border border-border bg-card py-0 shadow-sm"
            id={panelId}
            role={panelId ? "tabpanel" : undefined}
        >
            <ConfirmDeleteModal
                open={Boolean(reviewToDelete)}
                onOpenChange={(open) => {
                    if (!open) {
                        setReviewToDelete(null);
                    }
                }}
                onConfirm={handleConfirmDelete}
                title={t("profileReviews.delete.title")}
                description={t("profileReviews.delete.description")}
                itemName={reviewToDelete?.roomTitle}
                isLoading={deleteReview.isPending}
            />

            <div className="border-b border-border px-5 py-6 sm:px-8">
                <div className="flex items-start gap-3">
                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary/10">
                        <MessageSquare className="h-5 w-5 text-primary" />
                    </div>
                    <div>
                        <h2 className="text-2xl font-semibold text-foreground">{title}</h2>
                        {description ? (
                            <p className="mt-1 text-sm text-muted-foreground">{description}</p>
                        ) : null}
                    </div>
                </div>
            </div>

            <div className="px-5 py-6 sm:px-8">
                {reviewsLoading ? (
                    <p className="text-muted-foreground">{t("profile.loading")}</p>
                ) : reviewsQuery.isError ? (
                    <p className="text-red-500">{errorMessage}</p>
                ) : reviews.length ? (
                    <>
                        <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                            {reviews.map((review) => (
                                <ReviewCard
                                    key={review.id}
                                    review={review}
                                    onDelete={canDeleteReview(review) ? setReviewToDelete : undefined}
                                    isDeleting={deleteReview.isPending && reviewToDelete?.id === review.id}
                                />
                            ))}
                        </div>

                        {reviewTotalPages > 1 ? (
                            <PagingBar
                                currentPage={reviewPagination.currentPage}
                                totalPages={reviewTotalPages}
                                links={reviewPagination.links}
                                onPageChange={setCurrentReviewsPage}
                            />
                        ) : null}
                    </>
                ) : (
                    <div className="py-12 text-center">
                        <p className="text-muted-foreground">{emptyMessage}</p>
                    </div>
                )}
            </div>
        </Card>
    );
}
