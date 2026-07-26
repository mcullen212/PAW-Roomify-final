import { Calendar, MapPin } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"

export type TripStatus = "planning" | "confirmed" | "pending" | "completed"

export interface Destination {
    id: string
    name: string
    countryCode: string
    days: number
    flagEmoji: string
}

export interface Trip {
    id: string
    title: string
    location: string
    status: TripStatus
    startDate: string
    endDate: string
    dateRange: string
    destinations: Destination[]
    daysUntil?: number
}

interface TripCardProps {
    trip: Trip
    onManage: (id: string) => void
}

const statusConfig: Record<TripStatus, { labelKey: string; bgColor: string; textColor: string }> = {
    planning: { labelKey: "trips.status.planning", bgColor: "bg-[#2563eb]", textColor: "text-white" },
    confirmed: { labelKey: "trips.status.confirmed", bgColor: "bg-emerald-500", textColor: "text-white" },
    pending: { labelKey: "trips.status.pending", bgColor: "bg-amber-500", textColor: "text-white" },
    completed: { labelKey: "trips.status.completed", bgColor: "bg-gray-500", textColor: "text-white" },
}

export function TripCard({ trip, onManage }: TripCardProps) {
    const { t } = useTranslation()
    const status = statusConfig[trip.status]

    const handleViewItinerary = () => {
        onManage(trip.id)
    }

    return (
        <Card className="bg-card border border-border shadow-sm hover:shadow-md transition-shadow">
            <div className="p-5">
                <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                    <div className="min-w-0">
                        <div className="mb-2 flex flex-wrap items-center gap-2">
                            <h3 className="text-xl font-bold leading-tight text-foreground">{trip.title}</h3>
                            <span className={`${status.bgColor} ${status.textColor} px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wide`}>
                                {t(status.labelKey)}
                            </span>
                        </div>
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <MapPin className="h-4 w-4 flex-shrink-0" />
                            <span className="truncate">{trip.location}</span>
                        </div>
                    </div>

                    <div className="flex items-center justify-between gap-4 lg:flex-shrink-0">
                        <div className="flex items-center gap-2 whitespace-nowrap text-muted-foreground">
                            <Calendar className="h-4 w-4" />
                            <span className="text-sm font-medium">
                                {trip.dateRange}
                            </span>
                        </div>
                    </div>
                </div>

                <div className="mt-5">
                    <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                        {t("tripDetails.destinations.title")}
                    </p>
                    {trip.destinations.length === 0 ? (
                        <p className="text-sm text-muted-foreground">{t("trips.card.noDestinations")}</p>
                    ) : (
                        <div className="flex flex-wrap gap-2">
                            {trip.destinations.map((dest) => (
                                <div
                                    key={dest.id}
                                    className="flex min-w-[116px] flex-col items-center justify-center rounded-xl border border-border bg-muted/50 px-5 py-4"
                                >
                                    <span className="mb-2 text-5xl leading-none">{dest.flagEmoji}</span>
                                    <span className="max-w-28 truncate text-center text-sm font-medium text-foreground">
                                        {dest.name}
                                    </span>
                                    <span className="text-xs text-muted-foreground">{t("trips.card.days", { count: dest.days })}</span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="mt-5 flex items-center justify-between gap-4 border-t border-border pt-4">
                    <div className="flex items-center gap-2">
                        {trip.daysUntil !== undefined && trip.daysUntil > 0 && (
                            <span className="bg-emerald-100 text-emerald-700 px-2.5 py-1 rounded-md text-xs font-semibold">
                                {t("trips.card.daysUntil", { count: trip.daysUntil })}
                            </span>
                        )}
                    </div>
                    <Button
                        onClick={handleViewItinerary}
                        className="px-5"
                    >
                        {t("trips.actions.viewItinerary")}
                    </Button>
                </div>
            </div>
        </Card>
    )
}
