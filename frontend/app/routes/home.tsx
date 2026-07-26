import { useEffect, useMemo } from "react"
import { type LoaderFunctionArgs } from "react-router";
import { SearchX } from "lucide-react"
import { useTranslation } from "react-i18next"
import { SearchBar } from "@/components/SearchBar"
import { RoomCard } from "~/components/rooms/RoomCard.tsx"
import type { Route } from "./+types/home.ts"
import roomApi from "@/lib/api/roomAPI"
import { Navbar } from "@/components/Navbar"
import { PagingBar } from "@/components/PagingBar"
import { VerificationEmailBanner } from "@/components/auth/verification-email-banner"
import { pageTitleKey } from "@/lib/utils"
import { readRoomSearchFilters, writeRoomSearchFilters } from "@/lib/room-search-params"
import type { RoomSearchFilters } from "@/lib/interfaces/room-search"
import { useApiServices } from "~/lib/hooks/useApiServices";
import { getPaginationWithFallback } from "~/lib/pagination";
import { useNormalizePaginationPage, usePaginationParams } from "~/lib/hooks/usePaginationParams";
import i18n from "~/i18n/i18n";

export function meta({}: Route.MetaArgs) {
    return [
        { title: pageTitleKey("pageTitles.home") },
        { name: "description", content: i18n.t("pageDescriptions.home") },
    ]
}

export async function clientLoader({ request }: LoaderFunctionArgs) {
    const url = new URL(request.url)
    const page = Number.parseInt(url.searchParams.get("page") || "1", 10)
    const filters = readRoomSearchFilters(url.searchParams)
    try {
        const res = await roomApi.getRooms({ ...filters, page: Number.isFinite(page) && page > 0 ? page : 1 });
        return res.data;
    } catch (err) {
        console.error("Failed to load rooms:", err);
        return [];
    }
}

export default function HomePage({ loaderData }: any) {
    const { t } = useTranslation()
    const { searchParams, setSearchParams, currentPage, setCurrentPage } = usePaginationParams()
    const { roomService } = useApiServices()

    const searchKey = searchParams.toString()
    const filters = useMemo(() => readRoomSearchFilters(searchParams), [searchKey])
    const roomsQuery = roomService.useGetRooms(filters, currentPage)
    const rooms = roomsQuery.data?.data ?? loaderData ?? []
    const pagination = getPaginationWithFallback(roomsQuery.data?.pagination, currentPage)
    useNormalizePaginationPage(roomsQuery.data?.pagination, currentPage, setCurrentPage)

    useEffect(() => {
        const currentCheckIn = searchParams.get("checkIn") || undefined
        const currentCheckOut = searchParams.get("checkOut") || undefined

        if (currentCheckIn === filters.checkIn && currentCheckOut === filters.checkOut) return

        setSearchParams((currentParams) => {
            const nextParams = new URLSearchParams(currentParams)

            if (filters.checkIn) {
                nextParams.set("checkIn", filters.checkIn)
            } else {
                nextParams.delete("checkIn")
            }

            if (filters.checkOut) {
                nextParams.set("checkOut", filters.checkOut)
            } else {
                nextParams.delete("checkOut")
            }

            return nextParams
        }, { replace: true })
    }, [filters.checkIn, filters.checkOut, searchKey, searchParams, setSearchParams])

    const handleSearch = (searchData: RoomSearchFilters) => {
        setSearchParams(writeRoomSearchFilters(searchData))
    }

    const handleClear = () => {
        setSearchParams(new URLSearchParams())
    }

    return (
        <div className="min-h-screen bg-background">
            <Navbar />

            <VerificationEmailBanner />

            <section className="py-12 bg-muted/30">
                <div className="container mx-auto">
                    <SearchBar initialFilters={filters} onSearch={handleSearch} onClear={handleClear} />
                </div>
            </section>

            <section className="container mx-auto py-12">
                {rooms.length === 0 ? (
                    <div className="mx-auto flex max-w-xl flex-col items-center rounded-lg border border-border bg-card px-6 py-12 text-center shadow-sm">
                        <div className="mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                            <SearchX className="h-6 w-6" aria-hidden="true" />
                        </div>
                        <h2 className="text-xl font-semibold text-foreground">
                            {t("home.emptyResults.title")}
                        </h2>
                        <p className="mt-2 text-sm text-muted-foreground">
                            {t("home.emptyResults.description")}
                        </p>
                    </div>
                ) : (
                    <>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                            {rooms.map((room: any) => (
                                <RoomCard key={room.id} room={room} />
                            ))}
                        </div>

                        <PagingBar
                            currentPage={pagination.currentPage}
                            totalPages={pagination.totalPages}
                            links={pagination.links}
                            onPageChange={setCurrentPage}
                        />
                    </>
                )}
            </section>
        </div>
    )
}
