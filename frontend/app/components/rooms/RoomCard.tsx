import { Link, useLocation } from "react-router";
import { MapPin, Star, Trash2, Pencil } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Badge } from "~/components/ui/badge.tsx";
import { Button } from "~/components/ui/button.tsx";
import type {Room} from "~/types";
import { currentPath, withReturnTo } from "~/lib/navigation.ts";

interface RoomCardProps {
    room: Room;
    onDelete?: (room: Room) => void;
    onEdit?: (room: Room) => void;
    checkIn?: string;
    checkOut?: string;
}

function withSearchDates(destination: string, currentSearch: string, explicitDates?: { checkIn?: string; checkOut?: string }) {
    const currentParams = new URLSearchParams(currentSearch)
    const checkIn = explicitDates?.checkIn || currentParams.get("checkIn")
    const checkOut = explicitDates?.checkOut || currentParams.get("checkOut")

    if (!checkIn || !checkOut) {
        return destination
    }

    const [pathname, query = ""] = destination.split("?")
    const destinationParams = new URLSearchParams(query)
    destinationParams.set("checkIn", checkIn)
    destinationParams.set("checkOut", checkOut)

    return `${pathname}?${destinationParams.toString()}`
}

export function RoomCard({ room, onDelete, onEdit, checkIn, checkOut }: RoomCardProps) {
    const location = useLocation()
    const { t, i18n } = useTranslation()
    const detailsUrl = withSearchDates(
        withReturnTo(`/room/${room.id}`, currentPath(location)),
        location.search,
        { checkIn, checkOut },
    )
    const totalReviews = room.totalReviews ?? 0
    const averageRating = room.averageRating
    const hasRating = totalReviews > 0 && typeof averageRating === "number"
    const formattedPrice = new Intl.NumberFormat(i18n.language || "en", {
        style: "currency",
        currency: "USD",
        maximumFractionDigits: Number.isInteger(Number(room.dayPrice)) ? 0 : 2,
    }).format(Number(room.dayPrice))

    return (
        <div className="relative group bg-card border border-border rounded-xl overflow-hidden hover:shadow-xl transition-all duration-300 hover:-translate-y-1">

            {onDelete && (
                <div className="absolute top-3 left-3 z-10 flex gap-2">
                    <Button
                        size="icon"
                        variant="destructive"
                        aria-label={t("myRooms.delete.actionLabel", { room: room.title })}
                        className="h-8 w-8 rounded-md shadow-lg hover:scale-105 hover:shadow-xl"
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            onDelete?.(room);
                        }}
                    >
                        <Trash2 className="h-4 w-4" />
                    </Button>
                </div>
            )}

            {onEdit && (
                <div className="absolute top-3 right-3 z-10">
                    <Button
                        size="icon"
                        className="h-8 w-8 rounded-md bg-[#2563eb] text-white shadow-lg hover:scale-105 hover:bg-[#1d4ed8] hover:shadow-xl"
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            onEdit?.(room);
                        }}
                    >
                        <Pencil className="h-4 w-4" />
                    </Button>
                </div>
            )}

            <Link to={detailsUrl} className="block">
                <div className="relative aspect-[4/3] overflow-hidden bg-muted">
                    <img
                        src={room.imageUrl || "placeholder.svg"}
                        alt={room.title}
                        className="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
                    />

                    {hasRating && (
                        <div className="absolute bottom-3 left-3">
                            <Badge
                                className="bg-background/90 backdrop-blur text-primary border-0 shadow-lg flex items-center gap-1 px-2 py-1"
                                aria-label={t("roomDetails.ratingStarsLabel", { rating: averageRating.toFixed(1) })}
                            >
                                <Star className="w-3 h-3 fill-primary" />
                                <span className="font-bold text-xs">{averageRating.toFixed(1)}</span>
                            </Badge>
                        </div>
                    )}

                    <div className="absolute bottom-3 right-3">
                        <Badge className="bg-background/90 backdrop-blur text-foreground border-0 shadow-lg flex items-baseline gap-1 px-2 py-1">
                            <span className="font-semibold text-xs">{formattedPrice}</span>
                            <span className="text-[11px] text-muted-foreground">{t("roomDetails.pricePerDay")}</span>
                        </Badge>
                    </div>
                </div>

                <div className="p-4">
                    <h3 className="font-semibold text-lg text-foreground mb-2 text-pretty group-hover:text-primary transition-colors">
                        {room.title}
                    </h3>
                    <p className="text-sm text-muted-foreground mb-3 flex items-center gap-1">
                        <MapPin className="w-4 h-4" />
                        {room.country}, {room.city}
                    </p>
                </div>
            </Link>
        </div>
    );
}
