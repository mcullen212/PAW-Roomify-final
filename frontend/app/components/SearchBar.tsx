import { useEffect, useId, useMemo, useState, type KeyboardEvent } from "react"
import {
    Search,
    MapPin,
    SlidersHorizontal,
    Bath,
    UtensilsCrossed,
    Wifi,
    Snowflake,
    Thermometer,
    Car,
    Waves,
    Dumbbell,
} from "lucide-react"
import { DateRangePicker } from "@/components/DateRangePicker"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select"
import { cn } from "@/lib/utils"
import { countryFlagEmoji } from "@/lib/countries"
import { useCountries } from "@/lib/hooks/useCountries"
import type { DateRange } from "react-day-picker";
import {
    formatRoomSearchDate,
    getSearchToday,
    normalizeRoomSearchFilters,
    parseRoomSearchDate,
} from "@/lib/room-search-params";
import type { RoomSearchFilters } from "@/lib/interfaces/room-search";
import { useTranslation } from "react-i18next";

interface SearchBarProps {
    initialFilters?: RoomSearchFilters
    onSearch: (filters: RoomSearchFilters) => void
    onClear: () => void
}

const roomTypes = [
    { value: "HOME" },
    { value: "PRIVATE" },
    { value: "SHARED" },
    { value: "STUDIO" },
]

const bedTypes = [
    { value: "TWIN" },
    { value: "QUEEN" },
    { value: "KING" },
]

const amenities = [
    { value: "WIFI", icon: Wifi },
    { value: "AC", icon: Snowflake },
    { value: "HEATING", icon: Thermometer },
    { value: "PARKING", icon: Car },
    { value: "POOL", icon: Waves },
    { value: "GYM", icon: Dumbbell },
]

const MAX_COUNTRY_SUGGESTIONS = 8

function normalizeCountrySearch(value: string) {
    return value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .trim()
        .toLowerCase()
}

function getFilterDateRange(filters: RoomSearchFilters, today: Date): DateRange | undefined {
    const normalizedFilters = normalizeRoomSearchFilters(filters, today)
    const from = parseRoomSearchDate(normalizedFilters.checkIn)
    const to = parseRoomSearchDate(normalizedFilters.checkOut)

    return from && to ? { from, to } : undefined
}

function getValidDateRange(range: DateRange | undefined, today: Date): DateRange | undefined {
    if (!range?.from || !range.to) return undefined

    return getFilterDateRange({
        checkIn: formatRoomSearchDate(range.from),
        checkOut: formatRoomSearchDate(range.to),
    }, today)
}

export function SearchBar({ initialFilters = {}, onSearch, onClear }: SearchBarProps) {
    const { t } = useTranslation()
    const today = useMemo(() => getSearchToday(), [])
    const destinationListboxId = useId()
    const { countries, loading: countriesLoading, error: countriesError } = useCountries()
    const [destination, setDestination] = useState(initialFilters.destination || "")
    const [destinationSuggestionsOpen, setDestinationSuggestionsOpen] = useState(false)
    const [highlightedCountryIndex, setHighlightedCountryIndex] = useState(0)
    const [datePopoverOpen, setDatePopoverOpen] = useState(false)
    const [dateRange, setDateRange] = useState<DateRange | undefined>(() => getFilterDateRange(initialFilters, today))
    const [dateError, setDateError] = useState("")
    const [filtersOpen, setFiltersOpen] = useState(false)
    const [roomType, setRoomType] = useState(initialFilters.roomType || "ANY")
    const [bedType, setBedType] = useState(initialFilters.bedType || "ANY")
    const [privateBathroom, setPrivateBathroom] = useState(Boolean(initialFilters.privateBathroom))
    const [privateKitchen, setPrivateKitchen] = useState(Boolean(initialFilters.privateKitchen))
    const [selectedAmenities, setSelectedAmenities] = useState<string[]>(initialFilters.amenities || [])

    const amenitiesKey = initialFilters.amenities?.join(",") || ""

    useEffect(() => {
        const normalizedFilters = normalizeRoomSearchFilters(initialFilters, today)
        setDestination(initialFilters.destination || "")
        setDateRange(getFilterDateRange(initialFilters, today))
        setRoomType(normalizedFilters.roomType || "ANY")
        setBedType(normalizedFilters.bedType || "ANY")
        setPrivateBathroom(Boolean(normalizedFilters.privateBathroom))
        setPrivateKitchen(Boolean(normalizedFilters.privateKitchen))
        setSelectedAmenities(normalizedFilters.amenities || [])
    }, [
        initialFilters.destination,
        initialFilters.checkIn,
        initialFilters.checkOut,
        initialFilters.roomType,
        initialFilters.bedType,
        initialFilters.privateBathroom,
        initialFilters.privateKitchen,
        amenitiesKey,
    ])

    const activeFiltersCount = [
        roomType !== "ANY",
        bedType !== "ANY",
        privateBathroom,
        privateKitchen,
    ].filter(Boolean).length + selectedAmenities.length

    const destinationQuery = destination.trim()
    const countrySuggestions = useMemo(() => {
        const normalizedQuery = normalizeCountrySearch(destinationQuery)
        if (!normalizedQuery) return []

        return countries
            .filter((country) => normalizeCountrySearch(country).includes(normalizedQuery))
            .sort((a, b) => {
                const normalizedA = normalizeCountrySearch(a)
                const normalizedB = normalizeCountrySearch(b)
                const aStartsWith = normalizedA.startsWith(normalizedQuery)
                const bStartsWith = normalizedB.startsWith(normalizedQuery)
                if (aStartsWith !== bStartsWith) return aStartsWith ? -1 : 1
                return a.localeCompare(b)
            })
            .slice(0, MAX_COUNTRY_SUGGESTIONS)
    }, [countries, destinationQuery])

    const showDestinationSuggestions = destinationSuggestionsOpen
        && destinationQuery.length > 0
        && (countriesLoading || Boolean(countriesError) || countrySuggestions.length > 0)

    useEffect(() => {
        setHighlightedCountryIndex(0)
    }, [destinationQuery, countrySuggestions.length])

    const selectDestinationCountry = (country: string) => {
        setDestination(country)
        setDestinationSuggestionsOpen(false)
        setHighlightedCountryIndex(0)
    }

    const handleDestinationKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
        if (!showDestinationSuggestions || countrySuggestions.length === 0) {
            return
        }

        if (event.key === "ArrowDown") {
            event.preventDefault()
            setHighlightedCountryIndex((current) => (current + 1) % countrySuggestions.length)
        } else if (event.key === "ArrowUp") {
            event.preventDefault()
            setHighlightedCountryIndex((current) => (
                current === 0 ? countrySuggestions.length - 1 : current - 1
            ))
        } else if (event.key === "Enter") {
            event.preventDefault()
            selectDestinationCountry(countrySuggestions[highlightedCountryIndex])
        } else if (event.key === "Escape") {
            setDestinationSuggestionsOpen(false)
        }
    }

    const handleApply = () => {
        const hasPartialDateRange = Boolean(dateRange?.from) !== Boolean(dateRange?.to)

        if (hasPartialDateRange) {
            setDateError(t("searchBar.errors.dateRangeRequired"))
            return
        }

        const validDateRange = getValidDateRange(dateRange, today)
        if (dateRange?.from && dateRange?.to && !validDateRange) {
            setDateRange(undefined)
        }

        setDateError("")
        onSearch({
            destination: destination.trim() || undefined,
            checkIn: validDateRange?.from ? formatRoomSearchDate(validDateRange.from) : undefined,
            checkOut: validDateRange?.to ? formatRoomSearchDate(validDateRange.to) : undefined,
            roomType: roomType !== "ANY" ? roomType : undefined,
            bedType: bedType !== "ANY" ? bedType : undefined,
            privateBathroom: privateBathroom || undefined,
            privateKitchen: privateKitchen || undefined,
            amenities: selectedAmenities.length > 0 ? selectedAmenities : undefined,
        })
        setFiltersOpen(false)
    }

    const handleReset = () => {
        setDestination("")
        setDateRange(undefined)
        setDateError("")
        clearAdvancedFilters()
        onClear()
        setFiltersOpen(false)
    }

    const clearAdvancedFilters = () => {
        setRoomType("ANY")
        setBedType("ANY")
        setPrivateBathroom(false)
        setPrivateKitchen(false)
        setSelectedAmenities([])
    }

    const handleResetDates = () => {
        setDateRange(undefined)
        setDateError("")
    }

    const toggleAmenity = (value: string) => {
        setSelectedAmenities((current) =>
            current.includes(value)
                ? current.filter((amenity) => amenity !== value)
                : [...current, value]
        )
    }

    return (
        <form
            className="max-w-5xl mx-auto bg-card border border-border rounded-2xl shadow-lg p-4"
            onSubmit={(event) => {
                event.preventDefault()
                handleApply()
            }}
        >
            <div className="flex flex-col md:flex-row gap-4">
                {/* Destination Input */}
                <div className="flex-1">
                    <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wide mb-2 block">
                        {t("searchBar.destination")}
                    </Label>
                    <div
                        className="relative"
                        onBlur={(event) => {
                            if (!event.currentTarget.contains(event.relatedTarget as Node | null)) {
                                setDestinationSuggestionsOpen(false)
                            }
                        }}
                    >
                        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                        <Input
                            role="combobox"
                            aria-autocomplete="list"
                            aria-expanded={showDestinationSuggestions}
                            aria-controls={showDestinationSuggestions ? destinationListboxId : undefined}
                            aria-activedescendant={
                                showDestinationSuggestions && countrySuggestions[highlightedCountryIndex]
                                    ? `${destinationListboxId}-${highlightedCountryIndex}`
                                    : undefined
                            }
                            placeholder={t("searchBar.destinationPlaceholder")}
                            value={destination}
                            onChange={(e) => {
                                setDestination(e.target.value)
                                setDestinationSuggestionsOpen(true)
                            }}
                            onFocus={() => setDestinationSuggestionsOpen(true)}
                            onKeyDown={handleDestinationKeyDown}
                            className="pl-11 h-12 border-0 bg-muted/50 focus:bg-background transition-colors"
                        />
                        {showDestinationSuggestions && (
                            <div
                                id={destinationListboxId}
                                role="listbox"
                                className="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-50 max-h-72 overflow-y-auto rounded-md border border-border bg-popover p-1 text-popover-foreground shadow-lg"
                            >
                                {countriesLoading ? (
                                    <div className="px-3 py-2 text-sm text-muted-foreground" role="status">
                                        Loading countries...
                                    </div>
                                ) : countriesError ? (
                                    <div className="px-3 py-2 text-sm text-red-600" role="alert">
                                        {countriesError}
                                    </div>
                                ) : (
                                    countrySuggestions.map((country, index) => {
                                        const selected = index === highlightedCountryIndex
                                        return (
                                            <button
                                                key={country}
                                                id={`${destinationListboxId}-${index}`}
                                                type="button"
                                                role="option"
                                                aria-selected={selected}
                                                onMouseEnter={() => setHighlightedCountryIndex(index)}
                                                onMouseDown={(event) => event.preventDefault()}
                                                onClick={() => selectDestinationCountry(country)}
                                                className={cn(
                                                    "flex w-full items-center gap-3 rounded-sm px-3 py-2 text-left text-sm outline-none",
                                                    selected ? "bg-accent text-accent-foreground" : "hover:bg-accent hover:text-accent-foreground"
                                                )}
                                            >
                                                <span className="w-6 text-lg leading-none" aria-hidden="true">
                                                    {countryFlagEmoji(country) || "--"}
                                                </span>
                                                <span className="min-w-0 flex-1 truncate">{country}</span>
                                            </button>
                                        )
                                    })
                                )}
                            </div>
                        )}
                    </div>
                </div>

                {/* Dates */}
                <div className="flex-1">
                    <Label className="text-xs font-semibold text-muted-foreground uppercase mb-2 block">{t("searchBar.dates")}</Label>
                    <DateRangePicker
                        selectedRange={dateRange}
                        onSelectRange={(range) => {
                            setDateRange(range)
                            setDateError("")
                        }}
                        placeholder={t("searchBar.selectDates")}
                        resetLabel={t("searchBar.resetDates")}
                        error={dateError}
                        open={datePopoverOpen}
                        onOpenChange={setDatePopoverOpen}
                        onResetDates={handleResetDates}
                        calendarProps={{
                            disabled: { before: today },
                            excludeDisabled: true,
                            min: 1,
                            defaultMonth: dateRange?.from ?? today,
                            startMonth: today,
                        }}
                    />
                </div>

                <div className="flex items-end gap-2">
                    <Dialog open={filtersOpen} onOpenChange={setFiltersOpen}>
                        <DialogTrigger asChild>
                            <Button type="button" size="lg" variant="outline" className="h-12 gap-2 bg-transparent">
                                <SlidersHorizontal className="w-5 h-5" />
                                {t("searchBar.filters")}{activeFiltersCount > 0 ? ` (${activeFiltersCount})` : ""}
                            </Button>
                        </DialogTrigger>
                        <DialogContent className="sm:max-w-[620px]">
                            <DialogHeader>
                                <DialogTitle>{t("searchBar.filters")}</DialogTitle>
                            </DialogHeader>

                            <div className="space-y-6 py-2">
                                <div className="grid gap-4 sm:grid-cols-2">
                                    <div className="space-y-2">
                                        <Label>{t("searchBar.roomType")}</Label>
                                        <Select value={roomType} onValueChange={setRoomType}>
                                            <SelectTrigger className="h-11 w-full cursor-pointer transition-colors hover:border-primary/50 hover:bg-primary/5 hover:text-primary">
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem className="cursor-pointer hover:bg-primary/5 hover:text-primary focus:bg-primary/5 focus:text-primary" value="ANY">{t("searchBar.any")}</SelectItem>
                                                {roomTypes.map((type) => (
                                                    <SelectItem className="cursor-pointer hover:bg-primary/5 hover:text-primary focus:bg-primary/5 focus:text-primary" key={type.value} value={type.value}>
                                                        {t(`enums.roomType.${type.value}`)}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>

                                    <div className="space-y-2">
                                        <Label>{t("searchBar.bedType")}</Label>
                                        <Select value={bedType} onValueChange={setBedType}>
                                            <SelectTrigger className="h-11 w-full cursor-pointer transition-colors hover:border-primary/50 hover:bg-primary/5 hover:text-primary">
                                                <SelectValue />
                                            </SelectTrigger>
                                            <SelectContent>
                                                <SelectItem className="cursor-pointer hover:bg-primary/5 hover:text-primary focus:bg-primary/5 focus:text-primary" value="ANY">{t("searchBar.any")}</SelectItem>
                                                {bedTypes.map((type) => (
                                                    <SelectItem className="cursor-pointer hover:bg-primary/5 hover:text-primary focus:bg-primary/5 focus:text-primary" key={type.value} value={type.value}>
                                                        {t(`enums.bedType.${type.value}`)}
                                                    </SelectItem>
                                                ))}
                                            </SelectContent>
                                        </Select>
                                    </div>
                                </div>

                                <div className="grid gap-3 sm:grid-cols-2">
                                    <label className="group flex h-12 cursor-pointer items-center gap-3 rounded-md border px-3 transition-all hover:border-primary hover:bg-primary/5 hover:text-primary hover:shadow-sm focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50">
                                        <Checkbox
                                            checked={privateBathroom}
                                            onCheckedChange={(checked) => setPrivateBathroom(checked === true)}
                                            className="group-hover:border-primary"
                                        />
                                        <Bath className="h-4 w-4 text-muted-foreground transition-colors group-hover:text-primary" />
                                        <span className="text-sm font-medium">{t("searchBar.privateBathroom")}</span>
                                    </label>
                                    <label className="group flex h-12 cursor-pointer items-center gap-3 rounded-md border px-3 transition-all hover:border-primary hover:bg-primary/5 hover:text-primary hover:shadow-sm focus-within:border-ring focus-within:ring-[3px] focus-within:ring-ring/50">
                                        <Checkbox
                                            checked={privateKitchen}
                                            onCheckedChange={(checked) => setPrivateKitchen(checked === true)}
                                            className="group-hover:border-primary"
                                        />
                                        <UtensilsCrossed className="h-4 w-4 text-muted-foreground transition-colors group-hover:text-primary" />
                                        <span className="text-sm font-medium">{t("searchBar.privateKitchen")}</span>
                                    </label>
                                </div>

                                <div className="space-y-3">
                                    <Label>{t("searchBar.amenities")}</Label>
                                    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                                        {amenities.map((amenity) => {
                                            const Icon = amenity.icon
                                            const selected = selectedAmenities.includes(amenity.value)

                                            return (
                                                <button
                                                    key={amenity.value}
                                                    type="button"
                                                    onClick={() => toggleAmenity(amenity.value)}
                                                    className={cn(
                                                        "flex h-12 cursor-pointer items-center gap-3 rounded-md border px-3 text-left text-sm font-medium transition-all focus-visible:border-ring focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50",
                                                        selected
                                                            ? "border-primary bg-primary/10 text-primary hover:bg-primary/20 hover:shadow-md"
                                                            : "border-border bg-background text-foreground hover:border-primary hover:bg-secondary hover:text-primary hover:shadow-md"
                                                    )}
                                                >
                                                    <Icon className="h-4 w-4 transition-colors" />
                                                    {t(`enums.amenity.${amenity.value}`)}
                                                </button>
                                            )
                                        })}
                                    </div>
                                </div>

                                <div className="flex justify-end gap-3 border-t pt-4">
                                    <Button type="button" variant="outline" onClick={handleReset}>
                                        {t("searchBar.clearFilters")}
                                    </Button>
                                    <Button type="button" onClick={handleApply}>
                                        {t("searchBar.apply")}
                                    </Button>
                                </div>
                            </div>
                        </DialogContent>
                    </Dialog>
                    <Button type="submit" size="lg" className="h-12 px-8 gap-2">
                        <Search className="w-5 h-5" />
                        {t("searchBar.search")}
                    </Button>
                </div>
            </div>
        </form>
    )
}
