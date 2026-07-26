import type { ReactNode } from "react"
import { useTranslation } from "react-i18next"
import { ArrowLeft, Calendar, MapPin } from "lucide-react"
import { Button } from "@/components/ui/button"
import { formatApiDateRangeWithMonthText } from "@/lib/datesUtils"
import type { GroupTripDTO } from "@/lib/interfaces/trips"

export const groupTripStatusConfig: Record<string, { labelKey: string; bgColor: string; textColor: string }> = {
    PLANNING: { labelKey: "trips.status.planning", bgColor: "bg-[#2563eb]", textColor: "text-white" },
    UPCOMING: { labelKey: "trips.status.upcoming", bgColor: "bg-emerald-500", textColor: "text-white" },
    DONE: { labelKey: "trips.status.done", bgColor: "bg-gray-500", textColor: "text-white" },
    CANCELLED: { labelKey: "trips.status.cancelled", bgColor: "bg-amber-500", textColor: "text-white" },
}

interface GroupTripHeaderProps {
    groupTrip: GroupTripDTO
    destinationCount?: number
    backLabel?: string
    title?: string
    dateRangeLabel?: string
    destinationLabel?: string
    onBack: () => void
    action?: ReactNode
}

export function GroupTripHeader({
    groupTrip,
    destinationCount = 0,
    backLabel,
    title,
    dateRangeLabel,
    destinationLabel,
    onBack,
    action,
}: GroupTripHeaderProps) {
    const { t } = useTranslation()
    const status = groupTripStatusConfig[groupTrip.status || "PLANNING"] || groupTripStatusConfig.PLANNING
    const displayedTitle = title || groupTrip.title
    const displayedDateRange = dateRangeLabel || formatApiDateRangeWithMonthText(groupTrip.startDate || "", groupTrip.endDate || "", t)
    const displayedBackLabel = backLabel || t("trips.actions.backToTrips")
    const displayedDestinationLabel = destinationLabel ?? t("tripDetails.destinationCount", { count: destinationCount })

    return (
        <div className="bg-gradient-to-r from-[#2563eb] to-[#60a5fa] px-4 py-8 sm:px-6 lg:px-8">
            <div className="max-w-7xl mx-auto">
                <Button
                    variant="ghost"
                    onClick={onBack}
                    className="mb-6 bg-white/15 text-white hover:bg-white/25 rounded-full"
                >
                    <ArrowLeft className="h-4 w-4 mr-2" />
                    {displayedBackLabel}
                </Button>

                <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
                    <div>
                        <span className={`${status.bgColor} ${status.textColor} inline-flex px-4 py-1.5 rounded-full text-sm font-semibold uppercase tracking-wide mb-4`}>
                            {t(status.labelKey)}
                        </span>
                        <h1 className="text-3xl md:text-4xl font-bold text-white mb-3">{displayedTitle}</h1>
                        <div className="flex flex-wrap items-center gap-4 text-white/90">
                            <div className="flex items-center gap-2">
                                <Calendar className="h-4 w-4" />
                                <span>{displayedDateRange}</span>
                            </div>
                            {displayedDestinationLabel && (
                                <div className="flex items-center gap-2">
                                    <MapPin className="h-4 w-4" />
                                    <span>{displayedDestinationLabel}</span>
                                </div>
                            )}
                        </div>
                    </div>

                    {action}
                </div>
            </div>
        </div>
    )
}
