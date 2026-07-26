import { useCallback, useEffect, useMemo, useState } from "react"
import { CalendarDays, CheckCircle2, ClipboardList, Plus } from "lucide-react"
import { useTranslation } from "react-i18next"
import { useNavigate, useSearchParams } from "react-router"
import { Navbar } from "@/components/Navbar"
import { TripCard, type Destination, type Trip, type TripStatus } from "@/components/trip-card"
import { PlanTripForm, type PlanTripFormData } from "@/components/plan-trip-form"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { PagingBar } from "@/components/PagingBar"
import { type GroupTripDTO, type TripDTO } from "@/lib/interfaces/trips"
import { countryFlagEmoji } from "@/lib/countries"
import { useApiServices } from "@/lib/hooks/useApiServices"
import { cn, pageTitleKey } from "@/lib/utils"
import {
    countDays,
    countDaysUntil,
    formatApiDateRangeWithMonthText,
    formatApiDateWithMonthText,
} from "@/lib/datesUtils"
import { useAuth } from "@/lib/auth/useAuth"
import { getApiErrorMessage } from "@/lib/api/api-error-message"
import { compactLinks } from "@/lib/pagination"
import { getApiErrorPage } from "~/routes/errors/api-error-page"
import { useNormalizePaginationPage } from "~/lib/hooks/usePaginationParams"

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.trips") },
        { name: "description", content: pageTitleKey("pageDescriptions.trips") },
    ]
}

const PAGE_SIZE = 6

type TripTabId = "planning" | "upcoming" | "completed"

const tripTabs = [
    {
        id: "planning",
        labelKey: "trips.tabs.planning.label",
        summaryKey: "trips.summary.planning",
        status: "PLANNING",
        emptyMessageKey: "trips.tabs.planning.emptyMessage",
        emptyActionKey: "trips.tabs.planning.emptyAction",
        icon: ClipboardList,
    },
    {
        id: "upcoming",
        labelKey: "trips.tabs.upcoming.label",
        summaryKey: "trips.summary.upcoming",
        status: "UPCOMING",
        emptyMessageKey: "trips.tabs.upcoming.emptyMessage",
        emptyActionKey: "trips.tabs.upcoming.emptyAction",
        icon: CalendarDays,
    },
    {
        id: "completed",
        labelKey: "trips.tabs.completed.label",
        summaryKey: "trips.summary.completed",
        status: "DONE",
        emptyMessageKey: "trips.tabs.completed.emptyMessage",
        emptyActionKey: "trips.tabs.completed.emptyAction",
        icon: CheckCircle2,
    },
] as const

function getIdFromLocation(location?: string) {
    if (!location) return null
    const match = location.match(/\/(\d+)(?:\?.*)?$/)
    if (!match) return null

    const id = Number.parseInt(match[1], 10)
    return Number.isFinite(id) ? id : null
}

function mapStatus(status?: string): TripStatus {
    switch (status) {
        case "UPCOMING":
            return "confirmed"
        case "DONE":
            return "completed"
        case "CANCELLED":
            return "pending"
        case "PLANNING":
        default:
            return "planning"
    }
}

function mapDestination(destination: TripDTO): Destination {
    const days = countDays(destination.startDate, destination.endDate)
    return {
        id: String(destination.id),
        name: destination.country,
        countryCode: destination.countryCode || "",
        days,
        flagEmoji: countryFlagEmoji(destination.country, destination.countryCode) || "--",
    }
}

function mapTrip(groupTrip: GroupTripDTO, destinations: TripDTO[], t: (key: string, options?: Record<string, unknown>) => string): Trip {
    const mappedDestinations = destinations.map(mapDestination)
    const location = mappedDestinations.length > 0
        ? mappedDestinations.map((destination) => destination.name).join(", ")
        : t("trips.card.noDestinations")

    return {
        id: String(groupTrip.id),
        title: groupTrip.title,
        location,
        status: mapStatus(groupTrip.status),
        startDate: formatApiDateWithMonthText(groupTrip.startDate || "", t),
        endDate: formatApiDateWithMonthText(groupTrip.endDate || "", t),
        dateRange: formatApiDateRangeWithMonthText(groupTrip.startDate || "", groupTrip.endDate || "", t),
        destinations: mappedDestinations,
        daysUntil: countDaysUntil(groupTrip.startDate),
    }
}

function getTripTabFromSearch(searchParams: URLSearchParams): TripTabId {
    const view = searchParams.get("view")
    return tripTabs.some((tab) => tab.id === view) ? view as TripTabId : "planning"
}

interface TripsTabsProps {
    activeTab: TripTabId
    onTabChange: (tab: TripTabId) => void
}

function TripsTabs({ activeTab, onTabChange }: TripsTabsProps) {
    const { t } = useTranslation()

    return (
        <div
            aria-label={t("trips.tabs.ariaLabel")}
            className="flex gap-2 overflow-x-auto rounded-xl bg-muted/40 p-1"
            role="tablist"
        >
            {tripTabs.map((tab) => {
                const isActive = activeTab === tab.id
                const Icon = tab.icon

                return (
                    <button
                        aria-controls="trips-panel"
                        aria-selected={isActive}
                        className={cn(
                            "flex min-w-fit cursor-pointer items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
                            isActive
                                ? "border-[#2563eb] bg-[#2563eb] text-white shadow-sm hover:bg-[#1d4ed8] hover:shadow-md"
                                : "border-transparent bg-transparent text-muted-foreground hover:border-[#2563eb]/20 hover:bg-background hover:text-[#2563eb] hover:shadow-sm",
                        )}
                        id={`trips-tab-${tab.id}`}
                        key={tab.id}
                        onClick={() => onTabChange(tab.id)}
                        role="tab"
                        type="button"
                    >
                        <Icon className="h-4 w-4" aria-hidden="true" />
                        <span>{t(tab.labelKey)}</span>
                    </button>
                )
            })}
        </div>
    )
}

export default function TripsPage() {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const { userId } = useAuth()
    const { tripService } = useApiServices()
    const [searchParams, setSearchParams] = useSearchParams()
    const [error, setError] = useState("")
    const [isFormOpen, setIsFormOpen] = useState(false)

    const pageParam = Number.parseInt(searchParams.get("page") || "1", 10)
    const currentPage = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1
    const activeTab = getTripTabFromSearch(searchParams)
    const activeTabConfig = tripTabs.find((tab) => tab.id === activeTab) ?? tripTabs[0]
    const groupTripsQuery = tripService.useGetMyGroupTrips(
        userId ?? undefined,
        currentPage,
        PAGE_SIZE,
        activeTabConfig.status,
    )
    const groupTrips = groupTripsQuery.data?.data ?? []
    const destinationsQueries = tripService.useGetDestinationsList(
        groupTrips.map((groupTrip) => groupTrip.id),
        1,
        6,
    )
    const createGroupTripMutation = tripService.useCreateGroupTrip()
    const loading = groupTripsQuery.isLoading || destinationsQueries.some((query) => query.isLoading)
    const loadError = groupTripsQuery.error ?? destinationsQueries.find((query) => query.error)?.error
    const paginationLinks = compactLinks(groupTripsQuery.data?.pagination.links ?? {})
    const totalPages = groupTripsQuery.data?.pagination.totalPages ?? 1
    const trips = useMemo(
        () => groupTrips.map((groupTrip, index) => mapTrip(
            groupTrip,
            destinationsQueries[index]?.data?.data ?? [],
            t,
        )),
        [destinationsQueries, groupTrips, t],
    )

    const updateTripsSearch = useCallback((tab: TripTabId, page: number, replace = false) => {
        setSearchParams((currentParams) => {
            const nextParams = new URLSearchParams(currentParams)
            nextParams.set("view", tab)
            nextParams.set("page", String(Math.max(page, 1)))
            return nextParams
        }, { replace })
    }, [setSearchParams])

    const setCurrentPage = (page: number) => {
        updateTripsSearch(activeTab, page)
    }

    useNormalizePaginationPage(groupTripsQuery.data?.pagination, currentPage, (page, options) => {
        updateTripsSearch(activeTab, page, options?.replace)
    })

    useEffect(() => {
        const viewParam = searchParams.get("view")
        const pageParam = searchParams.get("page")

        if (viewParam !== activeTab || pageParam !== String(currentPage)) {
            updateTripsSearch(activeTab, currentPage, true)
        }
    }, [activeTab, currentPage, searchParams, updateTripsSearch])

    useEffect(() => {
        if (!userId) {
            setError(t("trips.errors.signInView"))
            return
        }

    }, [t, userId])

    const handleTabChange = (tab: TripTabId) => {
        updateTripsSearch(tab, 1)
    }

    const handleCreateTrip = async (formData: PlanTripFormData) => {
        setError("")

        try {
            if (!userId) {
                throw new Error(t("trips.errors.signInCreate"))
            }

            const response = await createGroupTripMutation.mutateAsync({
                ownerId: userId,
                title: formData.name,
            })

            setIsFormOpen(false)

            const createdId = getIdFromLocation(response.headers.location)
            if (createdId) {
                navigate(`/trips/${createdId}`)
                return
            }
        } catch (err) {
            setError(getApiErrorMessage(err, t("trips.errors.load")))
        }
    }

    const upcomingSchedule = useMemo(
        () => trips
            .filter((trip) => trip.startDate)
            .slice(0, 3)
            .map((trip) => {
                const [day = "--", month = "---"] = trip.startDate.split(" ")
                return {
                    month,
                    day,
                    title: trip.title,
                    subtitle: trip.location,
                }
            }),
        [trips]
    )

    const apiErrorPage = getApiErrorPage(loadError, {
        notFoundTitleKey: "tripDetails.notFound.title",
        notFoundDescriptionKey: "error404.tripDescription",
    })
    if (apiErrorPage) return apiErrorPage

    return (
        <div className="min-h-screen bg-[#f8fafc]">
            <Navbar />

            <PlanTripForm
                open={isFormOpen}
                onOpenChange={setIsFormOpen}
                onSubmit={handleCreateTrip}
                submitting={createGroupTripMutation.isPending}
            />

            <div className="bg-gradient-to-r from-[#3b82f6] to-[#60a5fa] py-12 px-4 sm:px-6 lg:px-8">
                <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                    <div>
                        <h1 className="text-4xl font-bold text-white italic mb-2">{t("trips.title")}</h1>
                        <p className="text-white/90 text-lg">{t(activeTabConfig.summaryKey, { count: trips.length })}</p>
                    </div>
                    <Button
                        onClick={() => setIsFormOpen(true)}
                        size="lg"
                    >
                        <Plus className="h-4 w-4 mr-2" />
                        {t("trips.actions.planTrip")}
                    </Button>
                </div>
            </div>

            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {error && (
                    <Card className="mb-6 p-4 border-red-200 bg-red-50 text-red-700">
                        {error}
                    </Card>
                )}

                <Card className="overflow-hidden border border-border bg-card py-0 shadow-sm">
                    <div className="border-b border-border px-4 py-4 sm:px-5">
                        <TripsTabs activeTab={activeTab} onTabChange={handleTabChange} />
                    </div>

                    <div className="px-5 py-8 sm:px-8">
                        <div className="flex flex-col lg:flex-row gap-8">
                            <div className="flex-1 space-y-6" id="trips-panel" role="tabpanel" aria-labelledby={`trips-tab-${activeTab}`}>
                                {loading ? (
                                    <Card className="p-12 text-center">
                                        <p className="text-muted-foreground text-lg">{t("trips.loading")}</p>
                                    </Card>
                                ) : trips.length === 0 ? (
                                    <Card className="p-12 text-center">
                                        <p className="text-muted-foreground text-lg">
                                            {t(activeTabConfig.emptyMessageKey)}
                                        </p>
                                        <Button
                                            onClick={() => setIsFormOpen(true)}
                                            className="mt-4"
                                        >
                                            <Plus className="h-4 w-4 mr-2" />
                                            {t(activeTabConfig.emptyActionKey)}
                                        </Button>
                                    </Card>
                                ) : (
                                    trips.map((trip) => (
                                        <TripCard
                                            key={trip.id}
                                            trip={trip}
                                            onManage={() => navigate(`/trips/${trip.id}`)}
                                        />
                                    ))
                                )}

                                {!loading && trips.length > 0 && (
                                    <PagingBar
                                        currentPage={currentPage}
                                        totalPages={totalPages}
                                        links={paginationLinks}
                                        onPageChange={setCurrentPage}
                                    />
                                )}
                            </div>

                            {activeTab !== "completed" && (
                                <div className="lg:w-[320px] flex-shrink-0">
                                    <Card className="p-5 bg-card border border-border">
                                        <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-4">
                                            {t("trips.upcomingSchedule.title")}
                                        </h3>
                                        <div className="space-y-3">
                                            {upcomingSchedule.length === 0 ? (
                                                <p className="text-sm text-muted-foreground">{t("trips.upcomingSchedule.empty")}</p>
                                            ) : (
                                                upcomingSchedule.map((event, index) => (
                                                    <div
                                                        key={`${event.title}-${index}`}
                                                        className="flex items-center gap-4 p-3 rounded-xl hover:bg-muted/50 transition-colors"
                                                    >
                                                        <div className="flex flex-col items-center justify-center w-14 h-14 bg-[#2563eb]/10 rounded-xl">
                                                            <span className="text-xs font-bold text-[#2563eb] uppercase">
                                                                {event.month}
                                                            </span>
                                                            <span className="text-xl font-bold text-[#2563eb]">
                                                                {event.day}
                                                            </span>
                                                        </div>
                                                        <div className="flex-1 min-w-0">
                                                            <p className="font-semibold text-foreground truncate">
                                                                {event.title}
                                                            </p>
                                                            <p className="text-sm text-muted-foreground truncate">
                                                                {event.subtitle}
                                                            </p>
                                                        </div>
                                                    </div>
                                                ))
                                            )}
                                        </div>
                                    </Card>
                                </div>
                            )}
                        </div>
                    </div>
                </Card>
            </main>
        </div>
    )
}
