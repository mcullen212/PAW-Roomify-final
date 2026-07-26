import type { AxiosInstance } from "axios";
import api from "./api";
import { VndType } from "./vndTypes";

export const createReviewsAPI = (client: AxiosInstance) => {
    const getReviews = (url: string, page?: number, pageSize?: number) => {
        return client.get(url, {
            headers: {
                Accept: VndType.APPLICATION_REVIEWS,
            },
            params: {
                page,
                pageSize,
            },
        });
    };

    const getReviewsByRoomOwner = (roomOwnerId: number, page?: number, pageSize?: number) => {
        return client.get("/reviews", {
            headers: {
                Accept: VndType.APPLICATION_REVIEWS,
            },
            params: {
                roomOwnerId,
                page,
                pageSize,
            },
        });
    };

    const getReviewsByUser = (userId: number, page?: number, pageSize?: number) => {
        return client.get("/reviews", {
            headers: {
                Accept: VndType.APPLICATION_REVIEWS,
            },
            params: {
                userId,
                page,
                pageSize,
            },
        });
    };

    const getReviewById = (id: number) => {
        return client.get(`/reviews/${id}`, {
            headers: {
                Accept: VndType.APPLICATION_REVIEW_DETAIL,
            },
        });
    };

    const createReview = (contactId: number, reviewerId: number, rating: number, comment: string) => {
        return client.post("/reviews", {
            contactId,
            reviewerId,
            rating,
            comment,
        }, {
            headers: {
                "Content-Type": VndType.APPLICATION_REVIEW_DETAIL,
                 Accept: VndType.APPLICATION_REVIEW_DETAIL,
            },
        });
    };

    const deleteReview = (id: number) => {
        return client.delete(`/reviews/${id}`);
    };
    
    return {
        getReviews,
        getReviewsByRoomOwner,
        getReviewsByUser,
        getReviewById,
        createReview,
        deleteReview,
    };
};

const reviewsApi = createReviewsAPI(api);

export default reviewsApi;
