import { useCallback, useEffect } from "react";
import { useSearchParams } from "react-router";
import type { PaginationState } from "~/lib/interfaces/pagination";

type SetCurrentPageOptions = {
    replace?: boolean;
};

export function getPageFromSearchParams(searchParams: URLSearchParams, paramName = "page") {
    const page = Number.parseInt(searchParams.get(paramName) ?? "", 10);
    return Number.isFinite(page) && page > 0 ? page : 1;
}

export function usePaginationParams(paramName = "page", maxPage?: number) {
    const [searchParams, setSearchParams] = useSearchParams();
    const currentPage = getPageFromSearchParams(searchParams, paramName);

    const setCurrentPage = useCallback((page: number, options?: SetCurrentPageOptions) => {
        const lowerBoundPage = Math.max(page, 1);
        const nextPage = maxPage
            ? Math.min(lowerBoundPage, Math.max(maxPage, 1))
            : lowerBoundPage;

        setSearchParams((currentParams) => {
            const nextParams = new URLSearchParams(currentParams);
            if (nextPage <= 1) {
                nextParams.delete(paramName);
            } else {
                nextParams.set(paramName, String(nextPage));
            }
            return nextParams;
        }, { replace: options?.replace });
    }, [maxPage, paramName, setSearchParams]);

    return {
        searchParams,
        setSearchParams,
        currentPage,
        setCurrentPage,
    };
}

export function useNormalizePaginationPage(
    pagination: PaginationState | undefined,
    currentPage: number,
    setCurrentPage: (page: number, options?: SetCurrentPageOptions) => void,
) {
    useEffect(() => {
        if (!pagination || currentPage <= 1) {
            return;
        }

        const hasPaginationLinks = Object.values(pagination.links).some(Boolean);
        if (!hasPaginationLinks) {
            setCurrentPage(1, { replace: true });
            return;
        }

        const safeTotalPages = Math.max(pagination.totalPages || 1, 1);
        if (currentPage > safeTotalPages) {
            setCurrentPage(safeTotalPages, { replace: true });
        }
    }, [currentPage, pagination, setCurrentPage]);
}
