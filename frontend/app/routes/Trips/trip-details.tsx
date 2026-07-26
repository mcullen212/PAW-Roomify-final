import { useMemo, useState, type FormEvent } from "react"
import { useNavigate, useParams, useSearchParams } from "react-router"
import { useTranslation } from "react-i18next"
import {
    CheckCircle2,
    Plus,
    Search,
    UserRound,
} from "lucide-react"
import { Navbar } from "@/components/Navbar"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { PagingBar } from "@/components/PagingBar"
import { type TripDTO } from "@/lib/interfaces/trips"
import { countryFlagEmoji } from "@/lib/countries"
import { useCountries } from "@/lib/hooks/useCountries"
import { useApiServices } from "@/lib/hooks/useApiServices"
import { pageTitleKey } from "@/lib/utils"
import {
    countDays,
    formatApiDateRangeWithMonthText,
    formatDateForApi,
    getTodayDate,
    isBeforeToday,
} from "@/lib/datesUtils"
import { GroupTripHeader } from "@/components/trips/group-trip-header"
import { writeRoomSearchFilters } from "@/lib/room-search-params"
import { DateRangePicker } from "@/components/DateRangePicker"
import type { DateRange } from "react-day-picker"
import { getApiErrorMessage } from "@/lib/api/api-error-message"
import { compactLinks } from "@/lib/pagination"
import NotFound from "~/routes/errors/not-found-page"
import { getApiErrorPage } from "~/routes/errors/api-error-page"
import { useNormalizePaginationPage } from "~/lib/hooks/usePaginationParams"

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.tripDetails") },
        { name: "description", content: pageTitleKey("pageDescriptions.tripDetails") },
    ]
}

const PAGE_SIZE = 6

function dateRangeContainsDate(startDate: string, endDate: string, date: Date) {
    const candidate = formatDateForApi(date)

    return startDate <= candidate && candidate <= endDate
}

function dateRangeOverlapsDestination(range: DateRange | undefined, destination: TripDTO) {
    if (!range?.from || !range.to) return false

    const startDate = formatDateForApi(range.from)
    const endDate = formatDateForApi(range.to)

    return startDate <= destination.endDate && endDate >= destination.startDate
}

function isDateInDestinations(date: Date, destinations: TripDTO[]) {
    return destinations.some((destination) => (
        dateRangeContainsDate(destination.startDate, destination.endDate, date)
    ))
}

function formatDestinationRange(destination: TripDTO, t: (key: string) => string) {
    return formatApiDateRangeWithMonthText(destination.startDate, destination.endDate, t)
}

export default function TripDetailPage() {
    const { t } = useTranslation()
    const params = useParams()
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const tripId = Number.parseInt(params.id || "", 10)
    const groupTripId = Number.isFinite(tripId) ? tripId : undefined
    const { tripService } = useApiServices()
    const { countries, loading: countriesLoading, error: countriesError } = useCountries()

    const [error, setError] = useState("")
    const [destinationForm, setDestinationForm] = useState({
        country: "",
        dateRange: undefined as DateRange | undefined,
    })
    const [isDestinationFormOpen, setIsDestinationFormOpen] = useState(false)

    const pageParam = Number.parseInt(searchParams.get("page") || "1", 10)
    const currentPage = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1
    const groupTripQuery = tripService.useGetGroupTrip(groupTripId)
    const destinationsQuery = tripService.useGetDestinations(groupTripId, currentPage, PAGE_SIZE)
    const destinations = destinationsQuery.data?.data ?? []
    const createDestinationMutation = tripService.useCreateDestination()
    const completePlanningMutation = tripService.useCompletePlanning()
    const groupTrip = groupTripQuery.data ?? null
    const paginationLinks = compactLinks(destinationsQuery.data?.pagination.links ?? {})
    const totalPages = destinationsQuery.data?.pagination.totalPages ?? 1
    const queryError = groupTripQuery.error
        ?? destinationsQuery.error
    const loading = groupTripQuery.isLoading
        || destinationsQuery.isLoading
    const displayError = error

    const setCurrentPage = (page: number, options?: { replace?: boolean }) => {
        setSearchParams((currentParams) => {
            const nextParams = new URLSearchParams(currentParams)
            if (page <= 1) {
                nextParams.delete("page")
            } else {
                nextParams.set("page", String(page))
            }
            return nextParams
        }, { replace: options?.replace })
    }

    useNormalizePaginationPage(destinationsQuery.data?.pagination, currentPage, setCurrentPage)

    const totalDays = useMemo(
        () => countDays(groupTrip?.startDate, groupTrip?.endDate),
        [groupTrip?.startDate, groupTrip?.endDate]
    )

    const roomsSearchCountry = destinations[0]?.country || ""
    const selectedDestinationCountryFlag = countryFlagEmoji(destinationForm.country)
    const todayDate = useMemo(() => getTodayDate(), [])
    const tripHasEnded = isBeforeToday(groupTrip?.endDate)
    const canPlanTrip = groupTrip?.status === "PLANNING" && !tripHasEnded
    const groupTripStatusLabel = groupTrip?.status ? t(`trips.groupStatus.${groupTrip.status}`) : ""

    const handleDestinationDateRangeChange = (range: DateRange | undefined) => {
        setError("")
        setDestinationForm({
            ...destinationForm,
            dateRange: range,
        })
    }

    const handleExploreRooms = (destination: TripDTO) => {
        const nextSearchParams = writeRoomSearchFilters({
            destination: destination.country,
            checkIn: destination.startDate,
            checkOut: destination.endDate,
        })

        navigate({
            pathname: "/",
            search: `?${nextSearchParams.toString()}`,
        })
    }

    const handleCreateDestination = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        if (!groupTrip) return

        if (!countries.some((country) => country === destinationForm.country)) {
            setError(t("tripDetails.errors.validCountry"))
            return
        }
        const selectedDateRange = destinationForm.dateRange

        if (!selectedDateRange?.from || !selectedDateRange.to) {
            setError(t("tripDetails.errors.dateRangeRequired"))
            return
        }
        if (selectedDateRange.from < todayDate) {
            setError(t("tripDetails.errors.startDateToday"))
            return
        }
        if (selectedDateRange.to <= selectedDateRange.from) {
            setError(t("tripDetails.errors.endDateAfterStart"))
            return
        }
        if (destinations.some((destination) => dateRangeOverlapsDestination(selectedDateRange, destination))) {
            setError(t("tripDetails.errors.dateOverlap"))
            return
        }

        setError("")

        try {
            await createDestinationMutation.mutateAsync({
                groupTripId: groupTrip.id,
                payload: {
                    country: destinationForm.country,
                    dateRange: selectedDateRange,
                },
            })
            setDestinationForm({ country: "", dateRange: undefined })
            setIsDestinationFormOpen(false)
        } catch (err) {
            setError(getApiErrorMessage(err, t("tripDetails.errors.load")))
        }
    }

    const handleCompletePlanning = async () => {
        if (!groupTrip) return

        setError("")

        try {
            await completePlanningMutation.mutateAsync(groupTrip.id)
        } catch (err) {
            setError(getApiErrorMessage(err, t("tripDetails.errors.load")))
        }
    }

    if (loading) {
        return (
            <div className="min-h-screen bg-[#f8fafc]">
                <Navbar />
                <div className="max-w-7xl mx-auto px-4 py-20 text-center">
                    <p className="text-muted-foreground text-lg">{t("tripDetails.loading")}</p>
                </div>
            </div>
        )
    }

    const apiErrorPage = getApiErrorPage(queryError, {
        badRequest: !groupTripId,
        notFoundTitleKey: "tripDetails.notFound.title",
        notFoundDescriptionKey: "error404.tripDescription",
    })
    if (apiErrorPage) return apiErrorPage
    if (queryError) {
        return (
            <div className="min-h-screen bg-[#f8fafc]">
                <Navbar />
                <div className="mx-auto max-w-7xl px-4 py-20 text-center">
                    <p className="text-red-600">{getApiErrorMessage(queryError, t("tripDetails.errors.load"))}</p>
                </div>
            </div>
        )
    }
    if (!groupTrip) return <NotFound titleKey="tripDetails.notFound.title" descriptionKey="error404.tripDescription" />

    return (
        <div className="min-h-screen bg-[#f8fafc]">
            <Navbar />

            <GroupTripHeader
                groupTrip={groupTrip}
                destinationCount={destinations.length}
                onBack={() => navigate("/trips")}
                action={canPlanTrip ? (
                    <Button
                        onClick={handleCompletePlanning}
                        disabled={completePlanningMutation.isPending}
                        size="lg"
                    >
                        <CheckCircle2 className="h-4 w-4 mr-2" />
                        {completePlanningMutation.isPending ? t("tripDetails.actions.completingPlanning") : t("tripDetails.actions.completePlanning")}
                    </Button>
                ) : null}
            />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {displayError && (
                    <Card className="mb-6 p-4 border-red-200 bg-red-50 text-red-700">
                        {displayError}
                    </Card>
                )}
                {tripHasEnded && groupTrip.status === "PLANNING" && (
                    <Card className="mb-6 p-4 border-amber-200 bg-amber-50 text-amber-800">
                        {t("tripDetails.pastPlanningWarning")}
                    </Card>
                )}

                <Dialog open={isDestinationFormOpen} onOpenChange={setIsDestinationFormOpen}>
                    <DialogContent className="sm:max-w-[520px]">
                        <DialogHeader>
                            <DialogTitle>{t("tripDetails.dialog.addDestinationTitle")}</DialogTitle>
                        </DialogHeader>
                        <form onSubmit={handleCreateDestination} className="space-y-4 py-4">
                            {error && (
                                <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                                    {error}
                                </div>
                            )}
                            <div className="space-y-2">
                                <Label htmlFor="destination-country">{t("postRoom.fields.country")}</Label>
                                <Select
                                    value={destinationForm.country}
                                    onValueChange={(country) => setDestinationForm({ ...destinationForm, country })}
                                    disabled={countriesLoading || Boolean(countriesError)}
                                    required
                                >
                                    <SelectTrigger id="destination-country">
                                        {destinationForm.country ? (
                                            <span className="flex min-w-0 items-center gap-2">
                                                <span className="text-lg leading-none" aria-hidden="true">
                                                    {selectedDestinationCountryFlag || "--"}
                                                </span>
                                                <span className="truncate">{destinationForm.country}</span>
                                            </span>
                                        ) : (
                                            <SelectValue placeholder={countriesLoading ? t("postRoom.loadingCountries") : t("postRoom.placeholders.country")} />
                                        )}
                                    </SelectTrigger>
                                    <SelectContent>
                                        {countries.map((country) => (
                                            <SelectItem key={country} value={country}>
                                                <span className="flex min-w-0 items-center gap-2">
                                                    <span className="text-lg leading-none" aria-hidden="true">
                                                        {countryFlagEmoji(country) || "--"}
                                                    </span>
                                                    <span className="truncate">{country}</span>
                                                </span>
                                            </SelectItem>
                                        ))}
                                    </SelectContent>
                                </Select>
                                {countriesError && (
                                    <p className="text-xs text-red-600">{countriesError}</p>
                                )}
                            </div>
                            <div className="space-y-2">
                                <Label>{t("tripDetails.dialog.dates")}</Label>
                                <DateRangePicker
                                    selectedRange={destinationForm.dateRange}
                                    onSelectRange={handleDestinationDateRangeChange}
                                    placeholder={t("searchBar.selectDates")}
                                    resetLabel={t("searchBar.resetDates")}
                                    dateFormat="LLL dd, yyyy"
                                    variant="form"
                                    onResetDates={() => handleDestinationDateRangeChange(undefined)}
                                    calendarProps={{
                                        disabled: (date) => date < todayDate || isDateInDestinations(date, destinations),
                                        excludeDisabled: true,
                                        min: 1,
                                        defaultMonth: destinationForm.dateRange?.from ?? todayDate,
                                        startMonth: todayDate,
                                    }}
                                />
                            </div>
                            <DialogFooter>
                                <Button
                                    type="submit"
                                    disabled={createDestinationMutation.isPending || countriesLoading || Boolean(countriesError)}
                                    className="w-full sm:w-auto"
                                >
                                    {createDestinationMutation.isPending ? t("tripDetails.actions.addingDestination") : t("tripDetails.actions.addDestination")}
                                </Button>
                            </DialogFooter>
                        </form>
                    </DialogContent>
                </Dialog>

                <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-8">
                    <Card className="p-4 bg-card border border-border">
                        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">
                            {t("tripDetails.stats.totalDays")}
                        </p>
                        <p className="text-2xl font-bold text-foreground">{totalDays}</p>
                    </Card>
                    <Card className="p-4 bg-card border border-border">
                        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">
                            {t("tripDetails.stats.destinations")}
                        </p>
                        <p className="text-2xl font-bold text-foreground">{destinations.length}</p>
                    </Card>
                    <Card className="p-4 bg-card border border-border">
                        <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">
                            {t("tripDetails.stats.status")}
                        </p>
                        <p className="text-2xl font-bold text-foreground">{groupTripStatusLabel}</p>
                    </Card>
                </div>

                <Card className="overflow-hidden border border-border bg-card py-0 shadow-sm">
                    <div className="border-b border-border px-5 py-5 sm:px-8">
                        <div className="flex items-center justify-between gap-4">
                            <div>
                                <h2 className="text-xl font-bold text-foreground">{t("tripDetails.destinations.title")}</h2>
                                <p className="text-muted-foreground text-sm">
                                    {roomsSearchCountry ? t("tripDetails.destinations.findRoomsAround", { country: roomsSearchCountry }) : t("tripDetails.destinations.emptyHelper")}
                                </p>
                            </div>
                            {canPlanTrip && (
                                <Button
                                    onClick={() => {
                                        setError("")
                                        setIsDestinationFormOpen(true)
                                    }}
                                >
                                    <Plus className="h-4 w-4 mr-2" />
                                    {t("tripDetails.actions.addDestination")}
                                </Button>
                            )}
                        </div>
                    </div>

                    <div className="px-5 py-8 sm:px-8">
                        <div className="space-y-6">
                            {destinations.length === 0 ? (
                                <Card className="p-12 text-center">
                                    <p className="text-muted-foreground text-lg">{t("tripDetails.destinations.empty")}</p>
                                </Card>
                            ) : (
                                destinations.map((destination) => {
                                    const canExploreRooms = !isBeforeToday(destination.endDate)
                                    const destinationDays = countDays(destination.startDate, destination.endDate)

                                    return (
                                        <Card
                                            key={destination.id}
                                            className="overflow-hidden bg-card border border-border"
                                        >
                                            <div className="bg-gradient-to-r from-[#3b82f6] to-[#60a5fa] p-5">
                                                <div className="flex items-center justify-between gap-4">
                                                    <div className="flex items-center gap-4 min-w-0">
                                                        <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl bg-white/20 text-4xl leading-none text-white">
                                                            {countryFlagEmoji(destination.country, destination.countryCode) || "--"}
                                                        </span>
                                                        <div className="min-w-0">
                                                            <h3 className="text-xl font-bold text-white truncate">{destination.country}</h3>
                                                            <p className="text-white/80 text-sm">{formatDestinationRange(destination, t)}</p>
                                                        </div>
                                                    </div>
                                                    <span className="bg-white/20 text-white px-3 py-1 rounded-full text-sm font-medium">
                                                        {t("tripDetails.destinationDuration", { count: destinationDays })}
                                                    </span>
                                                </div>
                                            </div>

                                            <div className="p-5">
                                                <div className={`grid gap-3 ${canExploreRooms ? "sm:grid-cols-2" : ""}`}>
                                                    {canExploreRooms && (
                                                        <Button
                                                            onClick={() => handleExploreRooms(destination)}
                                                            className="w-full"
                                                        >
                                                            <Search className="h-4 w-4 mr-2" />
                                                            {t("tripDetails.actions.viewRooms", { country: destination.country })}
                                                        </Button>
                                                    )}
                                                    <Button
                                                        variant="outline"
                                                        onClick={() => navigate(`/trips/${groupTrip.id}/contacts?destinationId=${destination.id}`)}
                                                        className="w-full"
                                                    >
                                                        <UserRound className="h-4 w-4 mr-2" />
                                                        {t("tripDetails.actions.viewDestinationContacts")}
                                                    </Button>
                                                </div>
                                            </div>
                                        </Card>
                                    )
                                })
                            )}
                        </div>

                        {destinations.length > 0 && (
                            <PagingBar
                                currentPage={currentPage}
                                totalPages={totalPages}
                                links={paginationLinks}
                                onPageChange={setCurrentPage}
                            />
                        )}
                    </div>
                </Card>
            </main>
        </div>
    )
}
