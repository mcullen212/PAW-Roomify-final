import { Link, useLocation, useNavigate, useParams, useSearchParams } from "react-router"
import { ArrowLeft, Building2, MapPin, Star, User, UtensilsCrossed } from "lucide-react"
import { type ReactNode, useMemo, useState } from "react"
import { Button } from "@/components/ui/button.tsx"
import { Badge } from "@/components/ui/badge.tsx"
import { Separator } from "@/components/ui/separator.tsx"
import { Navbar } from "@/components/Navbar.tsx"
import { readReturnTo } from "@/lib/navigation"
import { pageTitleKey } from "@/lib/utils"
import {
    AMENITY_MAP,
    BED_TYPE_MAP,
    DEFAULT_AMENITY_ICON,
    DEFAULT_BED_TYPE_ICON,
    DEFAULT_ROOM_TYPE_ICON,
    ROOM_TYPE_MAP,
} from "@/lib/room-details"
import { useTranslation } from "react-i18next"
import { useAuth } from "~/lib/auth/useAuth"
import { RoomReviewsSection } from "@/components/rooms/room-reviews-section"
import { RoomSwapsCard } from "@/components/rooms/room-swaps-card"
import { useRoomRequestForm } from "~/lib/forms/useRoomRequestForm.ts"
import { useApiServices } from "~/lib/hooks/useApiServices"
import { parseRoomSearchDate, readRoomSearchFilters, type RoomSearchFilters, writeRoomSearchFilters } from "~/lib/room-search-params"
import { getApiErrorMessage } from "~/lib/api/api-error-message"
import NotFound from "~/routes/errors/not-found-page"
import { getApiErrorPage } from "~/routes/errors/api-error-page"

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.roomDetails") },
        { name: "description", content: pageTitleKey("pageDescriptions.roomDetails") },
    ]
}

function buildRoomsSearchUrl(filters: RoomSearchFilters) {
    const params = writeRoomSearchFilters(filters)
    const search = params.toString()

    return search ? `/?${search}` : "/"
}

function LinkBadge({ children, compact = false, to }: { children: ReactNode; compact?: boolean; to: string }) {
    return (
        <Badge
            asChild
            variant="secondary"
            className={compact
                ? "px-3 py-1.5 text-sm hover:text-primary"
                : "rounded-full px-4 py-2 text-base font-normal text-foreground hover:text-primary"
            }
        >
            <Link to={to}>{children}</Link>
        </Badge>
    )
}

export default function RoomDetails() {
    const { t } = useTranslation()
    const { id } = useParams()
    const navigate = useNavigate()
    const location = useLocation()
    const [searchParams] = useSearchParams()
    const returnTo = readReturnTo(searchParams)
    const { authenticated, loading: authLoading, userId, verified, email } = useAuth()
    const { roomService, userService } = useApiServices()
    const [reviewSummary, setReviewSummary] = useState<{ totalReviews: number; averageRating: number }>({ totalReviews: 0, averageRating: 0 })
    const searchedDateFilters = useMemo(() => readRoomSearchFilters(searchParams), [searchParams])
    const searchedCheckIn = searchedDateFilters.checkIn
    const searchedCheckOut = searchedDateFilters.checkOut
    const searchedDateRange = useMemo(() => {
        const from = parseRoomSearchDate(searchedCheckIn)
        const to = parseRoomSearchDate(searchedCheckOut)

        return from && to ? { from, to } : undefined
    }, [searchedCheckIn, searchedCheckOut])
    const tripIdParam = Number.parseInt(searchParams.get("tripId") || "", 10)
    const requestTripId = Number.isFinite(tripIdParam) && tripIdParam > 0 ? tripIdParam : undefined
    const currentRoomId = Number(id)
    const safeRoomId = Number.isFinite(currentRoomId) ? currentRoomId : undefined
    const roomQuery = roomService.useGetRoomById(safeRoomId)
    const room = roomQuery.data ?? null
    const ownerQuery = userService.useGetPublicUserByUrl(room?.owner)
    const hostName = ownerQuery.data?.name || t("roomDetails.fallback.host")
    const roomSearchUrls = useMemo(() => {
        const withSearchedDates = (filters: RoomSearchFilters) => buildRoomsSearchUrl({
            ...filters,
            checkIn: searchedCheckIn,
            checkOut: searchedCheckOut,
        })

        return {
            country: withSearchedDates({ destination: room?.country }),
            city: withSearchedDates({ destination: room?.city }),
            bedType: withSearchedDates({ bedType: room?.bedType }),
            roomType: withSearchedDates({ roomType: room?.roomType }),
            privateKitchen: withSearchedDates({ privateKitchen: true }),
        }
    }, [room?.country, room?.city, room?.bedType, room?.roomType, searchedCheckIn, searchedCheckOut])

    const goToLogin = () => {
        navigate("/login", {
            state: { returnTo: `${location.pathname}${location.search}` },
        })
    }

    const requestForm = useRoomRequestForm({
        authenticated,
        authLoading,
        goToLogin,
        initialDateRange: searchedDateRange,
        requestTripId,
        room,
        roomId: safeRoomId,
        t,
        userId,
        verified,
    })

    if (roomQuery.isLoading) return <div className="min-h-screen flex items-center justify-center">{t("roomDetails.loading")}</div>
    const apiErrorPage = getApiErrorPage(roomQuery.error, {
        badRequest: !safeRoomId,
        notFoundTitleKey: "roomDetails.notFound",
        notFoundDescriptionKey: "error404.roomDescription",
    })
    if (apiErrorPage) return apiErrorPage
    if (roomQuery.isError) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex min-h-[16rem] items-center justify-center px-4 text-center text-red-500">
                    {getApiErrorMessage(roomQuery.error, t("error.network_error"))}
                </div>
            </div>
        )
    }
    if (!room) return <NotFound titleKey="roomDetails.notFound" descriptionKey="error404.roomDescription" />

    const formatEnum = (str?: string) => {
        if (!str) return ""
        return str.charAt(0) + str.slice(1).toLowerCase().replace("_", " ")
    }

    const translateEnum = (key: string, value?: string) => (
        value ? t(`enums.${key}.${value}`, { defaultValue: formatEnum(value) }) : ""
    )

    const totalReviews = reviewSummary.totalReviews
    const averageRating = reviewSummary.averageRating

    return (
        <div className="min-h-screen bg-background">
            <Navbar />
            <div className="container mx-auto px-4 py-4">
                <Button variant="ghost" asChild className="gap-2">
                    <Link to={returnTo}><ArrowLeft className="w-4 h-4" />{t("roomDetails.backToExplore")}</Link>
                </Button>
            </div>

            <div className="container mx-auto px-4 pb-8">
                <div className="max-h-[500px] overflow-hidden rounded-2xl">
                    <img
                        src={room.imageUrl || "/placeholder.svg"}
                        alt={room.title}
                        className="h-full w-full object-cover"
                    />
                </div>
            </div>

            <div className="container mx-auto px-4 pb-16">
                <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
                    <div className="space-y-8 lg:col-span-2">
                        <div>
                            <h1 className="mb-3 text-3xl font-bold md:text-4xl">{room.title}</h1>
                            <div className="flex flex-wrap items-center gap-4">
                                <LinkBadge to={roomSearchUrls.country}>
                                    <MapPin className="mr-2 h-5 w-5 text-primary" />
                                    {room.country}
                                </LinkBadge>
                                <LinkBadge to={roomSearchUrls.city}>
                                    <Building2 className="mr-2 h-5 w-5 text-primary" />
                                    {room.city}
                                </LinkBadge>
                                <Badge variant="secondary" className="rounded-full px-4 py-2 text-base font-normal text-foreground">
                                    <Star className="mr-2 h-5 w-5 fill-amber-400 text-amber-400" />
                                    {totalReviews ? averageRating.toFixed(1) : t("roomDetails.newRating")} ({totalReviews} {totalReviews === 1 ? t("roomDetails.reviewSingular") : t("roomDetails.reviewPlural")})
                                </Badge>
                                {requestForm.ownerId ? (
                                    <LinkBadge to={`/users/${requestForm.ownerId}`}>
                                        <User className="mr-2 h-5 w-5 text-primary" />
                                        {t("roomDetails.hostedBy", { hostName: hostName || t("roomDetails.fallback.host") })}
                                    </LinkBadge>
                                ) : (
                                    <Badge variant="secondary" className="rounded-full px-4 py-2 text-base font-normal text-foreground">
                                        <User className="mr-2 h-5 w-5 text-primary" />
                                        {t("roomDetails.hostedBy", { hostName: hostName || t("roomDetails.fallback.host") })}
                                    </Badge>
                                )}
                            </div>
                        </div>

                        <div className="flex flex-wrap items-center gap-4">
                            <LinkBadge compact to={roomSearchUrls.bedType}>
                                {BED_TYPE_MAP[room.bedType] || DEFAULT_BED_TYPE_ICON}
                                {translateEnum("bedType", room.bedType)}
                            </LinkBadge>

                            <LinkBadge compact to={roomSearchUrls.roomType}>
                                {ROOM_TYPE_MAP[room.roomType] || DEFAULT_ROOM_TYPE_ICON}
                                {translateEnum("roomType", room.roomType)}
                            </LinkBadge>

                            {room.privateKitchen && (
                                <LinkBadge compact to={roomSearchUrls.privateKitchen}>
                                    <UtensilsCrossed className="mr-1 h-4 w-4" />
                                    {t("searchBar.privateKitchen")}
                                </LinkBadge>
                            )}
                        </div>

                        <Separator />
                        <h2 className="mb-4 text-2xl font-semibold">{t("roomDetails.aboutTitle")}</h2>
                        <p className="leading-relaxed text-muted-foreground">{room.description}</p>
                        <Separator />

                        <div>
                            <h2 className="mb-4 text-2xl font-semibold">{t("roomDetails.amenitiesTitle")}</h2>
                            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                                {room.amenities?.map((amenity: string) => (
                                    <div key={amenity} className="flex items-center gap-3">
                                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-muted">
                                            {AMENITY_MAP[amenity] || DEFAULT_AMENITY_ICON}
                                        </div>
                                        <span>{translateEnum("amenity", amenity)}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        <Separator />

                        <RoomReviewsSection reviewsUrl={room.reviews} onSummaryLoaded={setReviewSummary} />
                    </div>

                    <RoomSwapsCard
                        authLoading={authLoading}
                        authenticated={authenticated}
                        email={email}
                        goToLogin={goToLogin}
                        requestForm={requestForm}
                        roomDayPrice={room.dayPrice}
                        verified={verified}
                    />
                </div>
            </div>
        </div>
    )
}
