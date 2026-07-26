export interface Review {
    id: number;
    comment: string;
    rating: number;
    date: string;
    reviewerId: number;
    reviewerUrl: string;
    reviewerName?: string;
    roomId?: number;
    roomTitle?: string;
    roomUrl?: string;
}

export interface ReviewsResponse {
    reviews: Review[];
    totalReviews: number;
    averageRating: number;
}
