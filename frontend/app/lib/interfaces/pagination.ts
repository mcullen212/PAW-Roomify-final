export type PaginationLinks = {
    first: string;
    prev: string;
    next: string;
    last: string;
};

export type PaginationState = {
    currentPage: number;
    totalPages: number;
    links: PaginationLinks;
};
