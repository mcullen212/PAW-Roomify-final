import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PublicUserReviewsSection } from "../public-user-reviews-section";
import { UserReviewsSection } from "../user-reviews-section";

const reviewServiceMock = vi.hoisted(() => ({
    useGetReviewsByRoomOwner: vi.fn(),
    useGetReviewsByUser: vi.fn(),
    useDeleteReview: vi.fn(),
}));

vi.mock("~/lib/hooks/useApiServices", () => ({
    useApiServices: () => ({
        reviewService: reviewServiceMock,
    }),
}));

vi.mock("react-i18next", async (importOriginal) => ({
    ...(await importOriginal<typeof import("react-i18next")>()),
    useTranslation: () => ({
        t: (key: string, options?: Record<string, number>) => {
            const messages: Record<string, string> = {
                "publicProfile.reviews.title": "Reviews",
                "publicProfile.reviews.empty": "No reviews yet",
                "publicProfile.reviews.error": "Reviews error",
                "profile.loading": "Loading",
                "pagination.previous": "Previous",
                "pagination.next": "Next",
                "pagination.pageOf": `Page ${options?.currentPage} of ${options?.totalPages}`,
            };

            return messages[key] ?? key;
        },
    }),
}));

vi.mock("@/components/reviews/review-card", () => ({
    ReviewCard: ({
        review,
        onDelete,
    }: {
        review: { comment: string };
        onDelete?: () => void;
    }) => (
        <article>
            <p>{review.comment}</p>
            {onDelete ? <button type="button">Delete review</button> : null}
        </article>
    ),
}));

const paginationLinks = {
    first: "/reviews?roomOwnerId=7&page=1&pageSize=12",
    prev: "",
    next: "/reviews?roomOwnerId=7&page=2&pageSize=12",
    last: "/reviews?roomOwnerId=7&page=3&pageSize=12",
};

function renderSection(initialEntry = "/users/7") {
    return render(
        <MemoryRouter initialEntries={[initialEntry]}>
            <PublicUserReviewsSection userId={7} />
        </MemoryRouter>,
    );
}

beforeEach(() => {
    vi.clearAllMocks();
    reviewServiceMock.useDeleteReview.mockReturnValue({
        mutateAsync: vi.fn(),
        isPending: false,
    });
    reviewServiceMock.useGetReviewsByUser.mockReturnValue({
        data: undefined,
        isError: false,
        isFetching: false,
        isLoading: false,
    });
});

describe("PublicUserReviewsSection", () => {
    it("requests and renders the reviews page from the reviewsPage search param", () => {
        const queryResultsByPage = new Map<number, unknown>();
        reviewServiceMock.useGetReviewsByRoomOwner.mockImplementation((
            _userId: number,
            page: number,
            _pageSize: number,
        ) => {
            if (!queryResultsByPage.has(page)) {
                queryResultsByPage.set(page, {
                    data: {
                        data: [{
                            id: page,
                            comment: `Review page ${page}`,
                            rating: 5,
                            date: "2026-07-01",
                            reviewerUrl: "/users/22",
                        }],
                        pagination: {
                            currentPage: page,
                            totalPages: 3,
                            links: {
                                ...paginationLinks,
                                prev: page > 1 ? `/reviews?roomOwnerId=7&page=${page - 1}&pageSize=12` : "",
                                next: page < 3 ? `/reviews?roomOwnerId=7&page=${page + 1}&pageSize=12` : "",
                            },
                        },
                    },
                    isError: false,
                    isFetching: false,
                    isLoading: false,
                });
            }

            return queryResultsByPage.get(page);
        });

        renderSection("/users/7?reviewsPage=2");

        expect(reviewServiceMock.useGetReviewsByRoomOwner).toHaveBeenCalledWith(7, 2, 12);
        expect(screen.getByText("Review page 2")).toBeTruthy();
        expect(screen.getByText("Page 2 of 3")).toBeTruthy();
        expect(screen.queryByRole("button", { name: "Delete review" })).toBeNull();
    });

    it("updates the requested reviews page when paging controls are used", async () => {
        const queryResultsByPage = new Map<number, unknown>();
        reviewServiceMock.useGetReviewsByRoomOwner.mockImplementation((
            _userId: number,
            page: number,
            _pageSize: number,
        ) => {
            if (!queryResultsByPage.has(page)) {
                queryResultsByPage.set(page, {
                    data: {
                        data: [{
                            id: page,
                            comment: `Review page ${page}`,
                            rating: 5,
                            date: "2026-07-01",
                            reviewerUrl: "/users/22",
                        }],
                        pagination: {
                            currentPage: page,
                            totalPages: 2,
                            links: {
                                first: "/reviews?roomOwnerId=7&page=1&pageSize=12",
                                prev: page > 1 ? "/reviews?roomOwnerId=7&page=1&pageSize=12" : "",
                                next: page < 2 ? "/reviews?roomOwnerId=7&page=2&pageSize=12" : "",
                                last: "/reviews?roomOwnerId=7&page=2&pageSize=12",
                            },
                        },
                    },
                    isError: false,
                    isFetching: false,
                    isLoading: false,
                });
            }

            return queryResultsByPage.get(page);
        });

        renderSection();

        fireEvent.click(screen.getByRole("button", { name: "Next" }));

        await waitFor(() => {
            expect(reviewServiceMock.useGetReviewsByRoomOwner).toHaveBeenCalledWith(7, 2, 12);
        });
        expect(screen.getByText("Review page 2")).toBeTruthy();
    });

    it("does not show the paging bar when there are no reviews", () => {
        reviewServiceMock.useGetReviewsByRoomOwner.mockReturnValue({
            data: {
                data: [],
                pagination: {
                    currentPage: 1,
                    totalPages: 3,
                    links: paginationLinks,
                },
            },
            isError: false,
            isFetching: false,
            isLoading: false,
        });

        renderSection();

        expect(screen.getByText("No reviews yet")).toBeTruthy();
        expect(screen.queryByRole("button", { name: "Previous" })).toBeNull();
        expect(screen.queryByRole("button", { name: "Next" })).toBeNull();
    });
});

describe("UserReviewsSection", () => {
    it("requests written reviews from the page search param and allows deleting own reviews", () => {
        reviewServiceMock.useGetReviewsByUser.mockReturnValue({
            data: {
                data: [{
                    id: 3,
                    comment: "Written review",
                    rating: 5,
                    date: "2026-07-01",
                    reviewerId: 7,
                    reviewerUrl: "/users/7",
                }],
                pagination: {
                    currentPage: 3,
                    totalPages: 3,
                    links: {
                        first: "/reviews?userId=7&page=1&pageSize=12",
                        prev: "/reviews?userId=7&page=2&pageSize=12",
                        next: "",
                        last: "/reviews?userId=7&page=3&pageSize=12",
                    },
                },
            },
            isError: false,
            isFetching: false,
            isLoading: false,
        });
        reviewServiceMock.useGetReviewsByRoomOwner.mockReturnValue({
            data: undefined,
            isError: false,
            isFetching: false,
            isLoading: false,
        });

        render(
            <MemoryRouter initialEntries={["/profile/reviews?view=written&page=3"]}>
                <UserReviewsSection
                    userId={7}
                    currentUserId={7}
                    source="written"
                    allowDeleteOwnReviews
                    pageParamName="page"
                    title="Reviews written"
                    emptyMessage="No written reviews"
                    errorMessage="Reviews error"
                />
            </MemoryRouter>,
        );

        expect(reviewServiceMock.useGetReviewsByUser).toHaveBeenCalledWith(7, 3, 12);
        expect(reviewServiceMock.useGetReviewsByRoomOwner).toHaveBeenCalledWith(undefined, 3, 12);
        expect(screen.getByText("Written review")).toBeTruthy();
        expect(screen.getByRole("button", { name: "Delete review" })).toBeTruthy();
    });
});
