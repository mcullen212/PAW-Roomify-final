import { parseLinkHeader } from "~/lib/utils";
import type { PaginationLinks, PaginationState } from "~/lib/interfaces/pagination";

export type { PaginationLinks, PaginationState } from "~/lib/interfaces/pagination";

export const emptyPaginationLinks: PaginationLinks = {
    first: "",
    prev: "",
    next: "",
    last: "",
};

function getPageFromLink(link?: string) {
    if (!link) return undefined;
    const match = link.match(/[?&]page=(\d+)/);
    return match ? Number.parseInt(match[1], 10) : undefined;
}

export function getPaginationFromLinkHeader(linkHeader: string | undefined, page = 1): PaginationState {
    const parsedLinks = parseLinkHeader(linkHeader);
    const links: PaginationLinks = {
        first: parsedLinks.first ?? "",
        prev: parsedLinks.prev ?? "",
        next: parsedLinks.next ?? "",
        last: parsedLinks.last ?? "",
    };
    const currentPage = Math.max(page, 1);
    const totalPages = getPageFromLink(links.last) ?? (links.next ? currentPage + 1 : currentPage);

    return {
        currentPage,
        totalPages,
        links,
    };
}

export function getPaginationWithFallback(
    pagination: PaginationState | undefined,
    currentPage: number,
): PaginationState {
    return pagination ?? {
        currentPage,
        totalPages: 1,
        links: emptyPaginationLinks,
    };
}

export function compactLinks(links: Record<string, string | undefined>) {
    return Object.fromEntries(
        Object.entries(links).filter((entry): entry is [string, string] => typeof entry[1] === "string"),
    );
}
