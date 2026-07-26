import { useMemo } from "react"
import { useNavigate, useParams, useSearchParams } from "react-router"
import { useTranslation } from "react-i18next"
import { UserRound } from "lucide-react"
import { Navbar } from "@/components/Navbar"
import { Card } from "@/components/ui/card"
import { PagingBar } from "@/components/PagingBar"
import { GroupTripHeader } from "@/components/trips/group-trip-header"
import { TripContactCard } from "@/components/trips/trip-contact-card"
import { useApiServices } from "@/lib/hooks/useApiServices"
import { pageTitleKey } from "@/lib/utils"
import { formatApiDateRangeWithMonthText } from "@/lib/datesUtils"
import type { TripDTO } from "@/lib/interfaces/trips"
import { getApiErrorMessage } from "@/lib/api/api-error-message"
import { compactLinks } from "@/lib/pagination"
import NotFound from "~/routes/errors/not-found-page"
import { getApiErrorPage } from "~/routes/errors/api-error-page"
import { useNormalizePaginationPage } from "~/lib/hooks/usePaginationParams"

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.tripDetails") },
        { name: "description", content: pageTitleKey("pageDescriptions.tripDestinationContacts") },
    ]
}

const DESTINATIONS_PAGE_SIZE = 100
const CONTACTS_PAGE_SIZE = 6

function findDestination(destinations: TripDTO[], destinationId?: number) {
    return destinations.find((destination) => destination.id === destinationId) ?? null
}

export default function TripDestinationContactsPage() {
    const { t } = useTranslation()
    const params = useParams()
    const navigate = useNavigate()
    const [searchParams, setSearchParams] = useSearchParams()
    const groupTripId = Number.parseInt(params.id || "", 10)
    const destinationId = Number.parseInt(searchParams.get("destinationId") || "", 10)
    const validGroupTripId = Number.isFinite(groupTripId) ? groupTripId : undefined
    const validDestinationId = Number.isFinite(destinationId) ? destinationId : undefined
    const { tripService } = useApiServices()

    const pageParam = Number.parseInt(searchParams.get("page") || "1", 10)
    const currentPage = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1
    const groupTripQuery = tripService.useGetGroupTrip(validGroupTripId)
    const destinationsQuery = tripService.useGetDestinations(validGroupTripId, 1, DESTINATIONS_PAGE_SIZE)
    const destinations = destinationsQuery.data?.data ?? []
    const destination = useMemo(
        () => findDestination(destinations, validDestinationId),
        [destinations, validDestinationId],
    )
    const contactsQuery = tripService.useGetDestinationContacts(
        validGroupTripId,
        destination?.id,
        currentPage,
        CONTACTS_PAGE_SIZE,
    )
    const contacts = contactsQuery.data?.data ?? []
    const paginationLinks = compactLinks(contactsQuery.data?.pagination.links ?? {})
    const totalPages = contactsQuery.data?.pagination.totalPages ?? 1
    const groupTrip = groupTripQuery.data ?? null
    const queryError = groupTripQuery.error ?? destinationsQuery.error ?? contactsQuery.error
    const loading = groupTripQuery.isLoading || destinationsQuery.isLoading || contactsQuery.isLoading

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

    useNormalizePaginationPage(contactsQuery.data?.pagination, currentPage, setCurrentPage)

    if (loading) {
        return (
            <div className="min-h-screen bg-[#f8fafc]">
                <Navbar />
                <div className="max-w-7xl mx-auto px-4 py-20 text-center">
                    <p className="text-muted-foreground text-lg">{t("tripDestinationContacts.loading")}</p>
                </div>
            </div>
        )
    }

    const apiErrorPage = getApiErrorPage(queryError, {
        badRequest: !validGroupTripId || !validDestinationId,
        notFoundTitleKey: "tripDestinationContacts.notFound",
        notFoundDescriptionKey: "error404.destinationDescription",
    })
    if (apiErrorPage) return apiErrorPage
    if (queryError) {
        return (
            <div className="min-h-screen bg-[#f8fafc]">
                <Navbar />
                <div className="mx-auto max-w-7xl px-4 py-20 text-center">
                    <p className="text-red-600">{getApiErrorMessage(queryError, t("tripDestinationContacts.errors.load"))}</p>
                </div>
            </div>
        )
    }
    if (!groupTrip || !destination) {
        return <NotFound titleKey="tripDestinationContacts.notFound" descriptionKey="error404.destinationDescription" />
    }

    return (
        <div className="min-h-screen bg-[#f8fafc]">
            <Navbar />
            <GroupTripHeader
                groupTrip={groupTrip}
                destinationCount={destinations.length}
                backLabel={t("tripDestinationContacts.backToTrip", { title: groupTrip.title })}
                title={t("tripDestinationContacts.title", { country: destination.country })}
                dateRangeLabel={formatApiDateRangeWithMonthText(destination.startDate, destination.endDate, t)}
                destinationLabel=""
                onBack={() => navigate(`/trips/${groupTrip.id}`)}
            />

            <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
                {contacts.length === 0 ? (
                    <Card className="p-12 text-center">
                        <UserRound className="mx-auto mb-3 h-8 w-8 text-muted-foreground" />
                        <p className="text-muted-foreground text-lg">{t("tripDestinationContacts.empty")}</p>
                    </Card>
                ) : (
                    <>
                        <div className="grid gap-4">
                            {contacts.map((contact) => (
                                <TripContactCard key={contact.id} contact={contact} />
                            ))}
                        </div>
                        <PagingBar
                            currentPage={currentPage}
                            totalPages={totalPages}
                            links={paginationLinks}
                            onPageChange={setCurrentPage}
                        />
                    </>
                )}
            </main>
        </div>
    )
}
