import React from "react"

import { useState, useCallback } from "react"
import { Link, useNavigate } from "react-router"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Card, CardContent } from "@/components/ui/card"
import { Calendar } from "@/components/ui/calendar"
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from "~/components/ui/popover.tsx"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "~/components/ui/select.tsx"
import {
    Home,
    User,
    Users,
    Building2,
    Bed,
    BedDouble,
    BedSingle,
    Bath,
    UtensilsCrossed,
    Wifi,
    Snowflake,
    Thermometer,
    Car,
    Waves,
    Dumbbell,
    Upload,
    X,
    CalendarIcon,
    Plus,
    Trash2,
    ImageIcon,
    CheckCircle2,
    ArrowLeft,
    Info,
} from "lucide-react"
import { format } from "date-fns"
import { enUS, es } from "date-fns/locale"
import type { DateRange } from "react-day-picker"
import { useTranslation } from "react-i18next"
import { Navbar } from "~/components/Navbar.tsx"
import { useCountries } from "@/lib/hooks/useCountries"
import { cn, pageTitleKey } from "@/lib/utils";
import { toast } from "sonner"
import { useApiServices } from "~/lib/hooks/useApiServices";
import { getApiErrorMessage } from "@/lib/api/api-error-message"
import { hasOverlappingDateRanges, type ApiDateRange } from "@/lib/room-availability"
import { isAllowedRoomImageType, isRoomImageWithinSizeLimit } from "@/lib/room-images"
import { validatePostRoomFields } from "@/lib/forms/room-form-validation"

const getCreatedResourceId = (location?: string): number | undefined => {
    if (!location) {
        return undefined
    }

    const cleanLocation = location.split("?")[0].replace(/\/$/, "")
    const id = Number(cleanLocation.substring(cleanLocation.lastIndexOf("/") + 1))

    return Number.isFinite(id) ? id : undefined
}

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.postRoom") },
        { name: "description", content: pageTitleKey("pageDescriptions.postRoom") },
    ]
}

interface ImageFile {
    id: string
    file: File
    preview: string
}

interface DateRangeItem {
    id: string
    range: DateRange | undefined
}

interface RoomPayload {
    title: string
    country: string
    city: string
    description: string
    roomType: string
    bedType: string
    privateBathroom: boolean
    privateKitchen: boolean
    amenities: string[]
    dateRange: ApiDateRange[]
    dayPrice: number
}

const roomTypes = [
    { id: "home", labelKey: "enums.roomType.HOME", icon: Home },
    { id: "private", labelKey: "enums.roomType.PRIVATE", icon: User },
    { id: "shared", labelKey: "enums.roomType.SHARED", icon: Users },
    { id: "studio", labelKey: "enums.roomType.STUDIO", icon: Building2 },
]

const bedTypes = [
    { id: "twin", labelKey: "enums.bedType.TWIN", icon: BedSingle },
    { id: "queen", labelKey: "enums.bedType.QUEEN", icon: Bed },
    { id: "king", labelKey: "enums.bedType.KING", icon: BedDouble },
]

const privateAmenities = [
    { id: "bathroom", labelKey: "searchBar.privateBathroom", icon: Bath },
    { id: "kitchen", labelKey: "searchBar.privateKitchen", icon: UtensilsCrossed },
]

const amenities = [
    { id: "wifi", labelKey: "enums.amenity.WIFI", icon: Wifi },
    { id: "ac", labelKey: "enums.amenity.AC", icon: Snowflake },
    { id: "heating", labelKey: "enums.amenity.HEATING", icon: Thermometer },
    { id: "parking", labelKey: "enums.amenity.PARKING", icon: Car },
    { id: "pool", labelKey: "enums.amenity.POOL", icon: Waves },
    { id: "gym", labelKey: "enums.amenity.GYM", icon: Dumbbell },
]

export default function PostRoom() {
    const navigate = useNavigate()
    const { t, i18n } = useTranslation()
    const { roomService } = useApiServices()
    const createImageMutation = roomService.useCreateImage()
    const createRoomMutation = roomService.useCreateRoom()
    const { countries, loading: countriesLoading, error: countriesError } = useCountries()

    const [title, setTitle] = useState("")
    const [country, setCountry] = useState("")
    const [city, setCity] = useState("")
    const [description, setDescription] = useState("")
    const [price, setPrice] = useState("")
    const [selectedRoomType, setSelectedRoomType] = useState("")
    const [selectedBedType, setSelectedBedType] = useState("")
    const [selectedPrivateAmenities, setSelectedPrivateAmenities] = useState<string[]>([])
    const [selectedAmenities, setSelectedAmenities] = useState<string[]>([])
    const [images, setImages] = useState<ImageFile[]>([])
    const [dateRanges, setDateRanges] = useState<DateRangeItem[]>([
        { id: "1", range: undefined },
    ])
    const [isSubmitting, setIsSubmitting] = useState(false)
    const [isDraggingImage, setIsDraggingImage] = useState(false)
    const [error, setError] = useState("")
    const dateLocale = i18n.language?.startsWith("es") ? es : enUS
    const formatDisplayDate = (date: Date) => format(date, "MMM d, yyyy", { locale: dateLocale })

    const showSubmitError = useCallback((message: string) => {
        setError(message)
        toast.error(message)
    }, [])

    const processSelectedImages = useCallback((files: FileList | File[]) => {
        if (files.length > 1) {
            showSubmitError(t("postRoom.errors.imageLimit"))
            return
        }

        const newImages: ImageFile[] = []
        let hasOversizedImage = false
        let hasInvalidImageType = false

        for (let i = 0; i < files.length; i++) {
            const file = files[i]

            if (!isAllowedRoomImageType(file)) {
                hasInvalidImageType = true
                continue
            }

            if (!isRoomImageWithinSizeLimit(file)) {
                hasOversizedImage = true
                continue
            }

            newImages.push({
                id: `${Date.now()}-${i}`,
                file,
                preview: URL.createObjectURL(file),
            })
        }

        if (newImages.length > 0) {
            setImages([newImages[0]])
        }

        if (hasOversizedImage) {
            showSubmitError(t("postRoom.errors.imageTooLarge"))
            return
        }

        if (hasInvalidImageType) {
            showSubmitError(t("postRoom.errors.imageInvalidType"))
        }
    }, [showSubmitError, t])

    const handleImageUpload = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files
        if (files) {
            processSelectedImages(files)
        }
        e.target.value = ""
    }, [processSelectedImages])

    const handleImageDragOver = useCallback((e: React.DragEvent<HTMLLabelElement>) => {
        e.preventDefault()
        if (images.length < 1) {
            setIsDraggingImage(true)
        }
    }, [images.length])

    const handleImageDragLeave = useCallback((e: React.DragEvent<HTMLLabelElement>) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
            setIsDraggingImage(false)
        }
    }, [])

    const handleImageDrop = useCallback((e: React.DragEvent<HTMLLabelElement>) => {
        e.preventDefault()
        setIsDraggingImage(false)

        if (images.length >= 1) {
            showSubmitError(t("postRoom.errors.imageLimit"))
            return
        }

        processSelectedImages(Array.from(e.dataTransfer.files))
    }, [images.length, processSelectedImages, showSubmitError, t])

    const removeImage = (id: string) => {
        setImages((prev) => {
            const image = prev.find((img) => img.id === id)
            if (image) {
                URL.revokeObjectURL(image.preview)
            }
            return prev.filter((img) => img.id !== id)
        })
    }

    const setMainImage = (id: string) => {
        setImages((prev) => {
            const imageIndex = prev.findIndex((img) => img.id === id)
            if (imageIndex > 0) {
                const newImages = [...prev]
                const [image] = newImages.splice(imageIndex, 1)
                newImages.unshift(image)
                return newImages
            }
            return prev
        })
    }

    const togglePrivateAmenity = (id: string) => {
        setSelectedPrivateAmenities((prev) =>
            prev.includes(id) ? prev.filter((a) => a !== id) : [...prev, id]
        )
    }

    const toggleAmenity = (id: string) => {
        setSelectedAmenities((prev) =>
            prev.includes(id) ? prev.filter((a) => a !== id) : [...prev, id]
        )
    }

    const addDateRange = () => {
        setDateRanges((prev) => [
            ...prev,
            { id: `${Date.now()}`, range: undefined },
        ])
    }

    const removeDateRange = (id: string) => {
        setDateRanges((prev) => prev.filter((d) => d.id !== id))
    }

    const updateDateRange = (id: string, range: DateRange | undefined) => {
        setDateRanges((prev) =>
            prev.map((d) => (d.id === id ? { ...d, range } : d))
        )
    }

    const handleSubmit: React.FormEventHandler<HTMLFormElement> = async (e) => {
        e.preventDefault()
        setError("")

        const parsedPrice = Number(price)
        const mainImage = images[0]?.file
        const ranges = dateRanges
            .map((dateRange) => {
                if (!dateRange.range?.from || !dateRange.range?.to) return null

                return {
                    startDate: format(dateRange.range.from, "yyyy-MM-dd"),
                    endDate: format(dateRange.range.to, "yyyy-MM-dd"),
                }
            })
            .filter((range): range is ApiDateRange => range !== null)

        const validationErrorKey = validatePostRoomFields({ title, city, description, price })
        if (validationErrorKey) {
            showSubmitError(t(validationErrorKey))
            return
        }

        if (!mainImage) {
            showSubmitError(t("postRoom.errors.imageRequired"))
            return
        }

        if (!selectedBedType) {
            showSubmitError(t("postRoom.errors.bedTypeRequired"))
            return
        }

        if (!countries.some((availableCountry) => availableCountry === country)) {
            showSubmitError(t("postRoom.errors.validCountry"))
            return
        }

        if (ranges.length === 0) {
            showSubmitError(t("postRoom.errors.availabilityRequired"))
            return
        }

        if (hasOverlappingDateRanges(ranges)) {
            showSubmitError(t("postRoom.errors.overlappingDates"))
            return
        }

        const roomPayload: RoomPayload = {
            title,
            country,
            city,
            description,
            roomType: selectedRoomType.toUpperCase(),
            bedType: selectedBedType.toUpperCase(),
            privateBathroom: selectedPrivateAmenities.includes("bathroom"),
            privateKitchen: selectedPrivateAmenities.includes("kitchen"),
            amenities: selectedAmenities.map((amenity) => amenity.toUpperCase()),
            dateRange: ranges,
            dayPrice: parsedPrice,
        }

        setIsSubmitting(true)
        try {
            const imageResponse = await createImageMutation.mutateAsync(mainImage)
            const imageId = getCreatedResourceId(imageResponse.headers.location)

            if (!imageId) {
                throw new Error(t("postRoom.errors.imageConfirm"))
            }

            const payload = {
                ...roomPayload,
                imageId,
            }
            const response = await createRoomMutation.mutateAsync(payload)
            const roomId = getCreatedResourceId(response.headers.location)

            toast.success(t("postRoom.success.created"))
            navigate(roomId ? `/room/${roomId}` : "/")
        } catch (err: unknown) {
            const message = getApiErrorMessage(err, t("postRoom.errors.publish"))
            showSubmitError(message)
        } finally {
            setIsSubmitting(false)
        }
    }

    const completedSections = [
        title && country && city,
        description,
        price,
        dateRanges.some((d) => d.range?.from),
        selectedRoomType,
        selectedBedType,
        images.length > 0,
    ].filter(Boolean).length

    const totalSections = 7

    return (
        <div className="min-h-screen bg-muted/30">
            <Navbar />
            <header className="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
                <div className="mx-auto flex h-16 max-w-5xl items-center justify-between px-4">
                    <div className="flex items-center gap-4">
                        <Link
                            to="/"
                            className="flex items-center gap-2 text-muted-foreground transition-colors hover:text-foreground"
                        >
                            <ArrowLeft className="h-5 w-5" />
                            <span className="hidden sm:inline">{t("roomDetails.backToExplore")}</span>
                        </Link>
                    </div>
                    <Link to="/" className="flex items-center gap-2">
                        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary">
                            <Home className="h-5 w-5 text-primary-foreground" />
                        </div>
                        <span className="text-xl font-bold text-primary">Roomify</span>
                    </Link>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <CheckCircle2 className="h-4 w-4 text-primary" />
                        {t("postRoom.progress", { completed: completedSections, total: totalSections })}
                    </div>
                </div>
            </header>

            <main className="mx-auto max-w-3xl px-4 py-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-foreground">{t("postRoom.title")}</h1>
                    <p className="mt-2 text-muted-foreground">
                        {t("postRoom.subtitle")}
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-8">
                    {/* Basic Information */}
                    <Card>
                        <CardContent className="p-6">
                            <div className="mb-6 flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                                    <Info className="h-5 w-5 text-primary" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold text-foreground">{t("postRoom.sections.basic.title")}</h2>
                                    <p className="text-sm text-muted-foreground">{t("postRoom.sections.basic.description")}</p>
                                </div>
                            </div>

                            <div className="space-y-4">
                                <div>
                                    <Label htmlFor="title" className="text-foreground">{t("postRoom.fields.title")}</Label>
                                    <Input
                                        id="title"
                                        placeholder={t("postRoom.placeholders.title")}
                                        value={title}
                                        onChange={(e) => setTitle(e.target.value)}
                                        className="mt-1.5"
                                    />
                                </div>

                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div>
                                        <Label htmlFor="country" className="text-foreground">{t("postRoom.fields.country")}</Label>
                                        <Select value={country} onValueChange={setCountry} disabled={countriesLoading || Boolean(countriesError)}>
                                            <SelectTrigger className="mt-1.5 cursor-pointer transition-colors hover:border-primary/50 hover:bg-primary/5 hover:text-primary">
                                                <SelectValue placeholder={countriesLoading ? t("postRoom.loadingCountries") : t("postRoom.placeholders.country")} />
                                            </SelectTrigger>
                                            <SelectContent>
                                                {countries.map((c) => (
                                                    <SelectItem className="cursor-pointer hover:bg-primary/5 hover:text-primary focus:bg-primary/5 focus:text-primary" key={c} value={c}>
                                                        {c}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                        {countriesError && (
                                            <p className="mt-1.5 text-xs text-red-600">{countriesError}</p>
                                        )}
                                    </div>
                                    <div>
                                        <Label htmlFor="city" className="text-foreground">{t("postRoom.fields.city")}</Label>
                                        <Input
                                            id="city"
                                            placeholder={t("postRoom.placeholders.city")}
                                            value={city}
                                            onChange={(e) => setCity(e.target.value)}
                                            className="mt-1.5"
                                        />
                                    </div>
                                </div>

                                <div>
                                    <Label htmlFor="description" className="text-foreground">{t("postRoom.fields.description")}</Label>
                                    <Textarea
                                        id="description"
                                        placeholder={t("postRoom.placeholders.description")}
                                        value={description}
                                        onChange={(e) => setDescription(e.target.value)}
                                        className="mt-1.5 min-h-[120px] resize-none"
                                    />
                                    <p className="mt-1.5 text-xs text-muted-foreground">
                                        {t("postRoom.characterCount", { count: description.length, max: 500 })}
                                    </p>
                                </div>

                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div>
                                        <Label htmlFor="price" className="text-foreground">{t("postRoom.fields.pricePerDayUsd")}</Label>
                                        <div className="relative mt-1.5">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                        $
                      </span>
                                            <Input
                                                id="price"
                                                type="number"
                                                min="0"
                                                placeholder="0"
                                                value={price}
                                                onChange={(e) => setPrice(e.target.value)}
                                                className="pl-7"
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    {/* Availability */}
                    <Card>
                        <CardContent className="p-6">
                            <div className="mb-6 flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                                    <CalendarIcon className="h-5 w-5 text-primary" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold text-foreground">{t("postRoom.sections.availability.title")}</h2>
                                    <p className="text-sm text-muted-foreground">{t("postRoom.sections.availability.description")}</p>
                                </div>
                            </div>

                            <div className="space-y-3">
                                {dateRanges.map((dateRange, index) => (
                                    <div key={dateRange.id} className="flex items-center gap-2">
                                        <Popover>
                                            <PopoverTrigger asChild>
                                                <Button
                                                    variant="outline"
                                                    className={cn(
                                                        "flex-1 justify-start text-left font-normal",
                                                        !dateRange.range && "text-muted-foreground"
                                                    )}
                                                >
                                                    <CalendarIcon className="mr-2 h-4 w-4" />
                                                    {dateRange.range?.from ? (
                                                        dateRange.range.to ? (
                                                            <>
                                                                {formatDisplayDate(dateRange.range.from)} -{" "}
                                                                {formatDisplayDate(dateRange.range.to)}
                                                            </>
                                                        ) : (
                                                            formatDisplayDate(dateRange.range.from)
                                                        )
                                                    ) : (
                                                        t("postRoom.placeholders.dateRange")
                                                    )}
                                                </Button>
                                            </PopoverTrigger>
                                            <PopoverContent className="w-auto p-0" align="start">
                                                <Calendar
                                                    mode="range"
                                                    selected={dateRange.range}
                                                    onSelect={(range) => updateDateRange(dateRange.id, range)}
                                                    numberOfMonths={2}
                                                    disabled={(date) => date < new Date()}
                                                />
                                            </PopoverContent>
                                        </Popover>
                                        {index > 0 && (
                                            <Button
                                                type="button"
                                                variant="ghost"
                                                size="icon"
                                                onClick={() => removeDateRange(dateRange.id)}
                                                className="shrink-0 text-muted-foreground hover:text-destructive"
                                            >
                                                <Trash2 className="h-4 w-4" />
                                            </Button>
                                        )}
                                    </div>
                                ))}
                                <Button
                                    type="button"
                                    variant="outline"
                                    onClick={addDateRange}
                                    className="w-full border-dashed bg-transparent"
                                >
                                    <Plus className="mr-2 h-4 w-4" />
                                    {t("postRoom.actions.addMoreDates")}
                                </Button>
                            </div>
                        </CardContent>
                    </Card>

                    {/* Room Type */}
                    <Card>
                        <CardContent className="p-6">
                            <div className="mb-6 flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                                    <Home className="h-5 w-5 text-primary" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold text-foreground">{t("postRoom.sections.roomType.title")}</h2>
                                    <p className="text-sm text-muted-foreground">{t("postRoom.sections.roomType.description")}</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                                {roomTypes.map((type) => (
                                    <button
                                        key={type.id}
                                        type="button"
                                        onClick={() => setSelectedRoomType(type.id)}
                                        className={cn(
                                            "group flex cursor-pointer flex-col items-center gap-2 rounded-lg border-2 p-4 transition-all focus-visible:border-ring focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                            selectedRoomType === type.id
                                                ? "border-primary bg-primary/10 hover:bg-primary/20 hover:shadow-md"
                                                : "border-border bg-background hover:border-primary hover:bg-secondary hover:shadow-md"
                                        )}
                                    >
                                        <type.icon
                                            className={cn(
                                                "h-6 w-6 transition-colors",
                                                selectedRoomType === type.id
                                                    ? "text-primary"
                                                    : "text-muted-foreground group-hover:text-primary"
                                            )}
                                        />
                                        <span
                                            className={cn(
                                                "text-sm font-medium transition-colors",
                                                selectedRoomType === type.id
                                                    ? "text-primary"
                                                    : "text-foreground group-hover:text-primary"
                                            )}
                                        >
                      {t(type.labelKey)}
                    </span>
                                    </button>
                                ))}
                            </div>
                        </CardContent>
                    </Card>

                    {/* Bed Type */}
                    <Card>
                        <CardContent className="p-6">
                            <div className="mb-6 flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                                    <Bed className="h-5 w-5 text-primary" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold text-foreground">{t("postRoom.sections.bedType.title")}</h2>
                                    <p className="text-sm text-muted-foreground">{t("postRoom.sections.bedType.description")}</p>
                                </div>
                            </div>

                            <div className="grid grid-cols-3 gap-3">
                                {bedTypes.map((type) => (
                                    <button
                                        key={type.id}
                                        type="button"
                                        onClick={() => setSelectedBedType(type.id)}
                                        className={cn(
                                            "group flex cursor-pointer flex-col items-center gap-2 rounded-lg border-2 p-4 transition-all focus-visible:border-ring focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                            selectedBedType === type.id
                                                ? "border-primary bg-primary/10 hover:bg-primary/20 hover:shadow-md"
                                                : "border-border bg-background hover:border-primary hover:bg-secondary hover:shadow-md"
                                        )}
                                    >
                                        <type.icon
                                            className={cn(
                                                "h-6 w-6 transition-colors",
                                                selectedBedType === type.id
                                                    ? "text-primary"
                                                    : "text-muted-foreground group-hover:text-primary"
                                            )}
                                        />
                                        <span
                                            className={cn(
                                                "text-sm font-medium transition-colors",
                                                selectedBedType === type.id
                                                    ? "text-primary"
                                                    : "text-foreground group-hover:text-primary"
                                            )}
                                        >
                      {t(type.labelKey)}
                    </span>
                                    </button>
                                ))}
                            </div>
                        </CardContent>
                    </Card>

                    {/* Amenities */}
                    <Card>
                        <CardContent className="p-6">
                            <div className="mb-6 flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                                    <Wifi className="h-5 w-5 text-primary" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold text-foreground">{t("postRoom.sections.amenities.title")}</h2>
                                    <p className="text-sm text-muted-foreground">{t("postRoom.sections.amenities.description")}</p>
                                </div>
                            </div>

                            <div className="space-y-6">
                                <div>
                                    <h3 className="mb-3 text-sm font-medium text-foreground">{t("postRoom.sections.amenities.private")}</h3>
                                    <div className="grid grid-cols-2 gap-3">
                                        {privateAmenities.map((amenity) => (
                                            <button
                                                key={amenity.id}
                                                type="button"
                                                onClick={() => togglePrivateAmenity(amenity.id)}
                                                className={cn(
                                                    "group flex cursor-pointer items-center gap-3 rounded-lg border-2 p-3 transition-all focus-visible:border-ring focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                                    selectedPrivateAmenities.includes(amenity.id)
                                                        ? "border-primary bg-primary/10 hover:bg-primary/20 hover:shadow-md"
                                                        : "border-border bg-background hover:border-primary hover:bg-secondary hover:shadow-md"
                                                )}
                                            >
                                                <amenity.icon
                                                    className={cn(
                                                        "h-5 w-5 transition-colors",
                                                        selectedPrivateAmenities.includes(amenity.id)
                                                            ? "text-primary"
                                                            : "text-muted-foreground group-hover:text-primary"
                                                    )}
                                                />
                                                <span
                                                    className={cn(
                                                        "text-sm font-medium transition-colors",
                                                        selectedPrivateAmenities.includes(amenity.id)
                                                            ? "text-primary"
                                                            : "text-foreground group-hover:text-primary"
                                                    )}
                                                >
                          {t(amenity.labelKey)}
                        </span>
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div>
                                    <h3 className="mb-3 text-sm font-medium text-foreground">{t("postRoom.sections.amenities.general")}</h3>
                                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
                                        {amenities.map((amenity) => (
                                            <button
                                                key={amenity.id}
                                                type="button"
                                                onClick={() => toggleAmenity(amenity.id)}
                                                className={cn(
                                                    "group flex cursor-pointer items-center gap-3 rounded-lg border-2 p-3 transition-all focus-visible:border-ring focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                                    selectedAmenities.includes(amenity.id)
                                                        ? "border-primary bg-primary/10 hover:bg-primary/20 hover:shadow-md"
                                                        : "border-border bg-background hover:border-primary hover:bg-secondary hover:shadow-md"
                                                )}
                                            >
                                                <amenity.icon
                                                    className={cn(
                                                        "h-5 w-5 transition-colors",
                                                        selectedAmenities.includes(amenity.id)
                                                            ? "text-primary"
                                                            : "text-muted-foreground group-hover:text-primary"
                                                    )}
                                                />
                                                <span
                                                    className={cn(
                                                        "text-sm font-medium transition-colors",
                                                        selectedAmenities.includes(amenity.id)
                                                            ? "text-primary"
                                                            : "text-foreground group-hover:text-primary"
                                                    )}
                                                >
                          {t(amenity.labelKey)}
                        </span>
                                            </button>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    {/* Photos */}
                    <Card>
                        <CardContent className="p-6">
                            <div className="mb-6 flex items-center gap-3">
                                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10">
                                    <ImageIcon className="h-5 w-5 text-primary" />
                                </div>
                                <div>
                                    <h2 className="text-lg font-semibold text-foreground">{t("postRoom.sections.photos.title")}</h2>
                                    <p className="text-sm text-muted-foreground">
                                        {t("postRoom.sections.photos.description")}
                                    </p>
                                </div>
                            </div>

                            <div className="space-y-4">
                                {/* Upload Area */}
                                <label
                                    htmlFor="image-upload"
                                    onDragOver={handleImageDragOver}
                                    onDragLeave={handleImageDragLeave}
                                    onDrop={handleImageDrop}
                                    className={cn(
                                        "flex cursor-pointer flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed border-border bg-muted/30 p-8 transition-colors hover:border-primary/50 hover:bg-muted/50",
                                        isDraggingImage && "border-primary bg-primary/10"
                                    )}
                                >
                                    <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary/10">
                                        <Upload className="h-6 w-6 text-primary" />
                                    </div>
                                    <div className="text-center">
                                        <p className="text-sm font-medium text-foreground">
                                            {t("postRoom.actions.upload")}
                                        </p>
                                    </div>
                                    <input
                                        id="image-upload"
                                        type="file"
                                        accept="image/jpeg,image/png,image/webp"
                                        onChange={handleImageUpload}
                                        className="hidden"
                                        disabled={images.length >= 1}
                                    />
                                </label>

                                {/* Image Preview Grid */}
                                {images.length > 0 && (
                                    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
                                        {images.map((image, index) => (
                                            <div
                                                key={image.id}
                                                className={cn(
                                                    "group relative aspect-square overflow-hidden rounded-lg border-2",
                                                    index === 0 ? "border-primary" : "border-border"
                                                )}
                                            >
                                                <img
                                                    src={image.preview || "/placeholder.svg"}
                                                    alt={t("postRoom.photoAlt", { number: index + 1 })}
                                                    className="h-full w-full object-cover"
                                                />
                                                {index === 0 && (
                                                    <div className="absolute left-2 top-2 rounded bg-primary px-2 py-0.5 text-xs font-medium text-primary-foreground">
                                                        {t("postRoom.photoMain")}
                                                    </div>
                                                )}
                                                <div className="absolute inset-0 flex items-center justify-center gap-2 bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
                                                    {index !== 0 && (
                                                        <Button
                                                            type="button"
                                                            size="sm"
                                                            variant="secondary"
                                                            onClick={() => setMainImage(image.id)}
                                                            className="h-8 text-xs"
                                                        >
                                                            {t("postRoom.actions.setMain")}
                                                        </Button>
                                                    )}
                                                    <Button
                                                        type="button"
                                                        size="icon"
                                                        variant="destructive"
                                                        onClick={() => removeImage(image.id)}
                                                        className="h-8 w-8"
                                                    >
                                                        <X className="h-4 w-4" />
                                                    </Button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </CardContent>
                    </Card>

                    {/* Submit */}
                    {error && (
                        <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
                            {error}
                        </div>
                    )}
                    <div className="flex flex-col gap-4 sm:flex-row sm:justify-end">
                        <Button
                            type="button"
                            variant="outline"
                            asChild
                            className="sm:order-1 bg-transparent"
                        >
                            <Link to="/">{t("button.cancel")}</Link>
                        </Button>
                        <Button
                            type="submit"
                            disabled={
                                isSubmitting ||
                                countriesLoading ||
                                Boolean(countriesError) ||
                                !title ||
                                !country ||
                                !city ||
                                !selectedRoomType ||
                                !selectedBedType
                            }
                            className="sm:order-2"
                        >
                            {isSubmitting ? t("postRoom.actions.publishing") : t("postRoom.actions.publish")}
                        </Button>
                    </div>
                </form>
            </main>
        </div>
    )
}
