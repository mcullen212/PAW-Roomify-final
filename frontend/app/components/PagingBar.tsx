import { Button } from "@/components/ui/button";
import { useTranslation } from "react-i18next";

interface PagingBarProps {
    currentPage: number
    totalPages: number
    links?: Record<string, string>
    onPageChange: (page: number) => void
}

export function PagingBar({ currentPage, totalPages, links, onPageChange }: PagingBarProps) {
    const { t } = useTranslation()
    const safeTotalPages = Math.max(totalPages || 1, 1)
    if (safeTotalPages <= 1) {
        return null
    }

    const hasPreviousPage = currentPage > 1 && Boolean(links?.prev)
    const hasNextPage = currentPage < safeTotalPages && Boolean(links?.next)

    return (
        <div className="flex justify-center items-center gap-4 mt-12">
            <Button
                disabled={!hasPreviousPage}
                onClick={() => onPageChange(currentPage - 1)}
            >
                {t("pagination.previous")}
            </Button>

            <span className="text-sm font-medium">
                {t("pagination.pageOf", { currentPage, totalPages: safeTotalPages })}
            </span>

            <Button
                disabled={!hasNextPage}
                onClick={() => onPageChange(currentPage + 1)}
            >
                {t("pagination.next")}
            </Button>
        </div>
    );
}
