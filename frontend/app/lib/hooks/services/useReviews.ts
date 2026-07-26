import type { AxiosInstance } from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createReviewsAPI } from "~/lib/api/reviewsAPI";
import { getPaginationFromLinkHeader } from "~/lib/pagination";
import { roomKeys } from "~/lib/hooks/services/useRooms";
import { userKeys } from "~/lib/hooks/services/useUsers";
import type { ReviewsResponse } from "~/lib/interfaces/reviews";

export const reviewKeys = {
    all: ["reviews"] as const,
    list: (url?: string, page?: number, pageSize?: number) =>
        ["reviews", url, page, pageSize] as const,
    roomOwnerList: (roomOwnerId?: number, page?: number, pageSize?: number) =>
        ["reviews", "roomOwner", roomOwnerId, page, pageSize] as const,
    userList: (userId?: number, page?: number, pageSize?: number) =>
        ["reviews", "user", userId, page, pageSize] as const,
    detail: (reviewId?: number) => ["reviews", reviewId] as const,
};

export function useReviews(api: AxiosInstance) {
    const reviewsApi = createReviewsAPI(api);

    function useGetReviews(url?: string, page?: number, pageSize?: number) {
        return useQuery({
            queryKey: reviewKeys.list(url, page, pageSize),
            queryFn: async () => {
                const currentPage = page ?? 1;
                const response = await reviewsApi.getReviews(url!, currentPage, pageSize);
                const reviewsResponse = response.data as ReviewsResponse;
                return {
                    data: reviewsResponse.reviews,
                    pagination: getPaginationFromLinkHeader(response.headers?.link, currentPage),
                    totalReviews: reviewsResponse.totalReviews,
                    averageRating: reviewsResponse.averageRating,
                };
            },
            enabled: !!url,
        });
    }

    function useGetReviewsByRoomOwner(roomOwnerId?: number, page?: number, pageSize?: number) {
        return useQuery({
            queryKey: reviewKeys.roomOwnerList(roomOwnerId, page, pageSize),
            queryFn: async () => {
                const currentPage = page ?? 1;
                const response = await reviewsApi.getReviewsByRoomOwner(roomOwnerId!, currentPage, pageSize);
                const reviewsResponse = response.data as ReviewsResponse;
                return {
                    data: reviewsResponse.reviews,
                    pagination: getPaginationFromLinkHeader(response.headers?.link, currentPage),
                };
            },
            enabled: !!roomOwnerId,
        });
    }

    function useGetReviewsByUser(userId?: number, page?: number, pageSize?: number) {
        return useQuery({
            queryKey: reviewKeys.userList(userId, page, pageSize),
            queryFn: async () => {
                const currentPage = page ?? 1;
                const response = await reviewsApi.getReviewsByUser(userId!, currentPage, pageSize);
                const reviewsResponse = response.data as ReviewsResponse;
                return {
                    data: reviewsResponse.reviews,
                    pagination: getPaginationFromLinkHeader(response.headers?.link, currentPage),
                };
            },
            enabled: !!userId,
        });
    }

    function useGetReviewById(reviewId?: number) {
        return useQuery({
            queryKey: reviewKeys.detail(reviewId),
            queryFn: async () => {
                const response = await reviewsApi.getReviewById(reviewId!);
                return response.data;
            },
            enabled: !!reviewId,
        });
    }

    function useCreateReview() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: ({
                contactId,
                reviewerId,
                rating,
                comment,
            }: {
                contactId: number;
                reviewerId: number;
                rating: number;
                comment: string;
            }) => reviewsApi.createReview(contactId, reviewerId, rating, comment),
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: reviewKeys.all });
            },
        });
    }

    function useDeleteReview() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: (reviewId: number) => reviewsApi.deleteReview(reviewId),
            onSuccess: (_, reviewId) => {
                queryClient.invalidateQueries({ queryKey: reviewKeys.all });
                queryClient.invalidateQueries({ queryKey: reviewKeys.detail(reviewId) });
                queryClient.invalidateQueries({ queryKey: userKeys.all });
                queryClient.invalidateQueries({ queryKey: roomKeys.all });
            },
        });
    }

    return {
        useGetReviews,
        useGetReviewsByRoomOwner,
        useGetReviewsByUser,
        useGetReviewById,
        useCreateReview,
        useDeleteReview,
    };
}
