import type { ReactNode } from "react";
import type { AxiosInstance } from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VndType } from "~/lib/api/vndTypes.ts";
import { roomKeys } from "../services/useRooms.ts";
import { reviewKeys, useReviews } from "../services/useReviews.ts";
import { userKeys } from "../services/useUsers.ts";

function setup() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });
    const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    return { queryClient, wrapper };
}

describe("useReviews", () => {
    it("gets a paginated review list with parsed links", async () => {
        const get = vi.fn().mockResolvedValue({
            data: {
                reviews: [{ id: 10, rating: 4 }],
                totalReviews: 9,
                averageRating: 4.3,
            },
            headers: {
                link: '</rooms/1/reviews?page=2&pageSize=4>; rel="next", </rooms/1/reviews?page=3&pageSize=4>; rel="last"',
            },
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useReviews(api).useGetReviews("/rooms/1/reviews", 1, 4), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [{ id: 10, rating: 4 }],
            pagination: {
                currentPage: 1,
                totalPages: 3,
                links: {
                    first: "",
                    prev: "",
                    next: "/rooms/1/reviews?page=2&pageSize=4",
                    last: "/rooms/1/reviews?page=3&pageSize=4",
                },
            },
            totalReviews: 9,
            averageRating: 4.3,
        });
        expect(get).toHaveBeenCalledWith("/rooms/1/reviews", {
            headers: { Accept: VndType.APPLICATION_REVIEWS },
            params: { page: 1, pageSize: 4 },
        });
    });

    it("gets a review detail with the detail media type", async () => {
        const get = vi.fn().mockResolvedValue({ data: { id: 3, rating: 5 } });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useReviews(api).useGetReviewById(3), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({ id: 3, rating: 5 });
        expect(get).toHaveBeenCalledWith("/reviews/3", {
            headers: { Accept: VndType.APPLICATION_REVIEW_DETAIL },
        });
    });

    it("does not request reviews without a URL", () => {
        const get = vi.fn();
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useReviews(api).useGetReviews(), { wrapper });

        expect(result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("gets an empty review list with summary defaults from the response body", async () => {
        const get = vi.fn().mockResolvedValue({
            data: {
                reviews: [],
                totalReviews: 0,
                averageRating: 0,
            },
            headers: {},
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useReviews(api).useGetReviews("/rooms/1/reviews", 1, 4), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [],
            pagination: {
                currentPage: 1,
                totalPages: 1,
                links: {
                    first: "",
                    prev: "",
                    next: "",
                    last: "",
                },
            },
            totalReviews: 0,
            averageRating: 0,
        });
    });

    it("gets reviews by room owner", async () => {
        const get = vi.fn().mockResolvedValue({
            data: {
                reviews: [{ id: 11, rating: 5 }],
                totalReviews: 1,
                averageRating: 5,
            },
            headers: {
                link: '</reviews?roomOwnerId=7&page=2&pageSize=4>; rel="next"',
            },
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useReviews(api).useGetReviewsByRoomOwner(7, 1, 4), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [{ id: 11, rating: 5 }],
            pagination: {
                currentPage: 1,
                totalPages: 2,
                links: {
                    first: "",
                    prev: "",
                    next: "/reviews?roomOwnerId=7&page=2&pageSize=4",
                    last: "",
                },
            },
        });
        expect(get).toHaveBeenCalledWith("/reviews", {
            headers: { Accept: VndType.APPLICATION_REVIEWS },
            params: { roomOwnerId: 7, page: 1, pageSize: 4 },
        });
    });

    it("gets reviews written by a user", async () => {
        const get = vi.fn().mockResolvedValue({
            data: {
                reviews: [{ id: 12, rating: 4 }],
                totalReviews: 1,
                averageRating: 4,
            },
            headers: {
                link: '</reviews?userId=7&page=2&pageSize=4>; rel="next"',
            },
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useReviews(api).useGetReviewsByUser(7, 1, 4), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [{ id: 12, rating: 4 }],
            pagination: {
                currentPage: 1,
                totalPages: 2,
                links: {
                    first: "",
                    prev: "",
                    next: "/reviews?userId=7&page=2&pageSize=4",
                    last: "",
                },
            },
        });
        expect(get).toHaveBeenCalledWith("/reviews", {
            headers: { Accept: VndType.APPLICATION_REVIEWS },
            params: { userId: 7, page: 1, pageSize: 4 },
        });
    });

    it("creates a review and invalidates review queries", async () => {
        const post = vi.fn().mockResolvedValue({ data: { id: 4 } });
        const api = { post } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const { result } = renderHook(() => useReviews(api).useCreateReview(), { wrapper });

        await act(async () => {
            await result.current.mutateAsync({ contactId: 9, reviewerId: 3, rating: 5, comment: "Great" });
        });

        expect(post).toHaveBeenCalledWith("/reviews", {
            contactId: 9,
            reviewerId: 3,
            rating: 5,
            comment: "Great",
        }, {
            headers: {
                "Content-Type": VndType.APPLICATION_REVIEW_DETAIL,
                Accept: VndType.APPLICATION_REVIEW_DETAIL,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: reviewKeys.all });
    });

    it("deletes a review and invalidates dependent queries", async () => {
        const remove = vi.fn().mockResolvedValue({});
        const api = { delete: remove } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const { result } = renderHook(() => useReviews(api).useDeleteReview(), { wrapper });

        await act(async () => {
            await result.current.mutateAsync(3);
        });

        expect(remove).toHaveBeenCalledWith("/reviews/3");
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: reviewKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: reviewKeys.detail(3) });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: userKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: roomKeys.all });
    });
});
