import { useEffect, useState, type FormEventHandler } from "react"
import { useTranslation } from "react-i18next"
import {
    ArrowLeft,
    BedDouble,
    Building2,
    Car,
    DollarSign,
    Dumbbell,
    Heater,
    Loader2,
    MapPin,
    Save,
    Snowflake,
    Waves,
    Wifi,
} from "lucide-react"
import { Link } from "react-router"
import { Button } from "~/components/ui/button.tsx"
import { Card, CardContent } from "~/components/ui/card.tsx"
import { Input } from "~/components/ui/input.tsx"
import { Label } from "~/components/ui/label.tsx"
import { Textarea } from "~/components/ui/textarea.tsx"
import { Badge } from "~/components/ui/badge.tsx"
import { Separator } from "~/components/ui/separator.tsx"
import { cn } from "~/lib/utils.ts"
import type { Amenity, Room } from "~/types"
import type { RoomUpdatePayload } from "~/lib/interfaces/rooms"

const AMENITIES: { id: Amenity; labelKey: string; icon: typeof Wifi }[] = [
    { id: "WIFI", labelKey: "enums.amenity.WIFI", icon: Wifi },
    { id: "AC", labelKey: "enums.amenity.AC", icon: Snowflake },
    { id: "HEATING", labelKey: "enums.amenity.HEATING", icon: Heater },
    { id: "PARKING", labelKey: "enums.amenity.PARKING", icon: Car },
    { id: "POOL", labelKey: "enums.amenity.POOL", icon: Waves },
    { id: "GYM", labelKey: "enums.amenity.GYM", icon: Dumbbell },
]

interface EditRoomFormProps {
    room: Room
    isSubmitting: boolean
    error?: string
    backTo: string
    onSave: (payload: RoomUpdatePayload) => void
}

export function EditRoomForm({ room, isSubmitting, error, backTo, onSave }: EditRoomFormProps) {
    const { t } = useTranslation()
    const [title, setTitle] = useState("")
    const [description, setDescription] = useState("")
    const [price, setPrice] = useState("")
    const [selectedAmenities, setSelectedAmenities] = useState<Amenity[]>([])

    useEffect(() => {
        setTitle(room.title || "")
        setDescription(room.description || "")
        setPrice(room.dayPrice?.toString() || "")
        setSelectedAmenities(room.amenities || [])
    }, [room])

    const toggleAmenity = (id: Amenity) => {
        setSelectedAmenities((current) =>
            current.includes(id)
                ? current.filter((amenity) => amenity !== id)
                : [...current, id]
        )
    }

    const handleSubmit: FormEventHandler<HTMLFormElement> = (event) => {
        event.preventDefault()

        onSave({
            title,
            description,
            amenities: selectedAmenities,
            dayPrice: Number(price),
        })
    }

    const canSubmit = title.trim().length > 0 && Number(price) > 0 && !isSubmitting

    return (
        <form onSubmit={handleSubmit} className="space-y-6">
            <Card className="overflow-hidden rounded-lg border shadow-sm">
                <div className="relative aspect-[16/7] min-h-[220px] bg-muted">
                    <img
                        src={room.imageUrl || "/placeholder.svg"}
                        alt={room.title}
                        className="h-full w-full object-cover"
                    />
                    <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/70 to-transparent p-6">
                        <Badge className="mb-3 bg-background/90 text-foreground">
                            <MapPin className="mr-1 h-3.5 w-3.5" />
                            {room.country}, {room.city}
                        </Badge>
                        <h1 className="max-w-3xl text-3xl font-bold text-white sm:text-4xl">
                            {t("editRoom.form.title")}
                        </h1>
                    </div>
                </div>

                <CardContent className="space-y-8 p-6">
                    <div className="flex flex-wrap gap-2">
                        {room.roomType && (
                            <Badge variant="secondary" className="gap-1 px-3 py-1.5">
                                <Building2 className="h-4 w-4" />
                                {t(`enums.roomType.${room.roomType}`)}
                            </Badge>
                        )}
                        {room.bedType && (
                            <Badge variant="secondary" className="gap-1 px-3 py-1.5">
                                <BedDouble className="h-4 w-4" />
                                {t(`enums.bedType.${room.bedType}`)}
                            </Badge>
                        )}
                        {room.privateBathroom && (
                            <Badge variant="secondary" className="gap-1 px-3 py-1.5">
                                {t("searchBar.privateBathroom")}
                            </Badge>
                        )}
                        {room.privateKitchen && (
                            <Badge variant="secondary" className="gap-1 px-3 py-1.5">
                                {t("searchBar.privateKitchen")}
                            </Badge>
                        )}
                    </div>

                    <div className="grid gap-5">
                        <div className="space-y-2">
                            <Label htmlFor="title">{t("postRoom.fields.title")}</Label>
                            <Input
                                id="title"
                                value={title}
                                onChange={(event) => setTitle(event.target.value)}
                                maxLength={50}
                                required
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="description">{t("postRoom.fields.description")}</Label>
                            <Textarea
                                id="description"
                                value={description}
                                onChange={(event) => setDescription(event.target.value)}
                                maxLength={500}
                                className="min-h-[140px] resize-none"
                            />
                            <p className="text-xs text-muted-foreground">
                                {t("postRoom.characterCount", { count: description.length, max: 500 })}
                            </p>
                        </div>

                        <div className="space-y-2 sm:max-w-xs">
                            <Label htmlFor="dayPrice">{t("editRoom.form.pricePerDay")}</Label>
                            <div className="relative">
                                <DollarSign className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                                <Input
                                    id="dayPrice"
                                    type="number"
                                    min="0.01"
                                    step="0.01"
                                    value={price}
                                    onChange={(event) => setPrice(event.target.value)}
                                    className="pl-9"
                                    required
                                />
                            </div>
                        </div>
                    </div>

                    <Separator />

                    <div className="space-y-3">
                        <div>
                            <h2 className="text-lg font-semibold">{t("postRoom.sections.amenities.title")}</h2>
                            <p className="text-sm text-muted-foreground">
                                {t("editRoom.form.amenitiesDescription")}
                            </p>
                        </div>
                        <div className="grid grid-cols-2 gap-3 md:grid-cols-3">
                            {AMENITIES.map((amenity) => {
                                const Icon = amenity.icon
                                const selected = selectedAmenities.includes(amenity.id)

                                return (
                                    <button
                                        key={amenity.id}
                                        type="button"
                                        onClick={() => toggleAmenity(amenity.id)}
                                        className={cn(
                                            "flex min-h-24 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 p-4 text-center transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
                                            selected
                                                ? "border-primary bg-primary/5 text-primary hover:bg-primary/10 hover:shadow-sm"
                                                : "border-border bg-background text-muted-foreground hover:border-primary/50 hover:bg-muted/40 hover:text-foreground hover:shadow-sm"
                                        )}
                                    >
                                        <Icon className="h-5 w-5" />
                                        <span className="text-sm font-medium leading-tight">{t(amenity.labelKey)}</span>
                                    </button>
                                )
                            })}
                        </div>
                    </div>

                    {error && (
                        <div className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-sm text-destructive">
                            {error}
                        </div>
                    )}

                    <div className="flex flex-col gap-3 border-t pt-6 sm:flex-row sm:justify-between">
                        <Button type="button" variant="outline" asChild>
                            <Link to={backTo}>
                                <ArrowLeft className="mr-2 h-4 w-4" />
                                {t("editRoom.actions.backToRooms")}
                            </Link>
                        </Button>
                        <Button
                            type="submit"
                            disabled={!canSubmit}
                            className="bg-[#2563eb] text-white hover:bg-[#1d4ed8] hover:shadow-md"
                        >
                            {isSubmitting ? (
                                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                            ) : (
                                <Save className="mr-2 h-4 w-4" />
                            )}
                            {t("editRoom.actions.saveChanges")}
                        </Button>
                    </div>
                </CardContent>
            </Card>
        </form>
    )
}
