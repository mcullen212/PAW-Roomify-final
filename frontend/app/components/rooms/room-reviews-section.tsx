import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { PagingBar } from "@/components/PagingBar";
import { ReviewCard } from "@/components/reviews/review-card";
import type { Review } from "@/lib/interfaces/reviews";
import { useApiServices } from "~/lib/hooks/useApiServices";
import { useNormalizePaginationPage, usePaginationParams } from "~/lib/hooks/usePaginationParams";
import { getPaginationWithFallback } from "~/lib/pagination";

const reviewsPageSize = 4;

type ReviewsSummary = { totalReviews: number; averageRating: number };

type RoomReviewsSectionProps = {
    reviewsUrl?: string;
    onReviewsLoaded?: (reviews: Review[]) => void;
    onSummaryLoaded?: (summary: ReviewsSummary) => void;
};

export function RoomReviewsSection({ reviewsUrl, onReviewsLoaded, onSummaryLoaded }: RoomReviewsSectionProps) {
    const { t } = useTranslation();
    const { reviewService, userService } = useApiServices();
    const {
        currentPage: currentReviewsPage,
        setCurrentPage: setCurrentReviewsPage,
    } = usePaginationParams("reviewsPage");
    const [reviews, setReviews] = useState<Review[]>([]);

    const reviewsQuery = reviewService.useGetReviews(reviewsUrl, currentReviewsPage, reviewsPageSize);
    const reviewData = useMemo(() => reviewsQuery.data?.data ?? [], [reviewsQuery.data]);
    const reviewerQueries = userService.useGetPublicUsersByUrls(
        reviewData.map((review: Review) => review.reviewerName ? undefined : review.reviewerUrl),
    );
    const reviewPagination = getPaginationWithFallback(reviewsQuery.data?.pagination, currentReviewsPage);
    const reviewTotalPages = reviewPagination.totalPages;
    useNormalizePaginationPage(reviewsQuery.data?.pagination, currentReviewsPage, setCurrentReviewsPage);
    const reviewersLoading = reviewerQueries.some((query, index) => (
        !reviewData[index]?.reviewerName && (query.isLoading || query.isFetching)
    ));
    const reviewerNamesKey = reviewerQueries
        .map((query, index) => reviewData[index]?.reviewerName || (query.data as { name?: string } | undefined)?.name || "")
        .join("|");
    const reviewsLoading = reviewsQuery.isLoading || reviewsQuery.isFetching || reviewersLoading;

    useEffect(() => {
        if (!reviewsUrl || reviewsQuery.isError) {
            setReviews([]);
            onReviewsLoaded?.([]);
            return;
        }

        if (!reviewsQuery.data || reviewersLoading) return;

        const reviewsWithUsers = reviewData.map((review: Review, index: number) => {
            if (review.reviewerName) {
                return review;
            }

            return {
                ...review,
                reviewerName: (reviewerQueries[index].data as { name?: string } | undefined)?.name || t("roomDetails.fallback.guest"),
            };
        });

        setReviews(reviewsWithUsers);
        onReviewsLoaded?.(reviewsWithUsers);
    }, [onReviewsLoaded, reviewData, reviewerNamesKey, reviewersLoading, reviewsQuery.data, reviewsQuery.isError, reviewsUrl, t]);

    const summaryTotal = reviewsQuery.data?.totalReviews;
    const summaryAverage = reviewsQuery.data?.averageRating;
    useEffect(() => {
        if (summaryTotal === undefined && summaryAverage === undefined) return;
        onSummaryLoaded?.({ totalReviews: summaryTotal ?? 0, averageRating: summaryAverage ?? 0 });
    }, [onSummaryLoaded, summaryTotal, summaryAverage]);

    return (
        <div>
            <h2 className="text-2xl font-semibold mb-4">{t("roomDetails.reviewsTitle")}</h2>
            {reviewsLoading ? (
                <p className="text-muted-foreground">{t("roomDetails.loading")}</p>
            ) : reviews.length ? (
                <>
                    <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                        {reviews.map((review) => (
                            <ReviewCard key={review.id} review={review} showRoomLink={false} />
                        ))}
                    </div>

                    {reviewTotalPages > 1 && (
                        <PagingBar
                            currentPage={reviewPagination.currentPage}
                            totalPages={reviewTotalPages}
                            links={reviewPagination.links}
                            onPageChange={setCurrentReviewsPage}
                        />
                    )}
                </>
            ) : (
                <p className="text-muted-foreground">{t("roomDetails.noReviews")}</p>
            )}
        </div>
    );
}
