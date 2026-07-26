import type { ComponentProps, ReactNode } from "react"
import { useState } from "react"
import { format } from "date-fns"
import { enUS, es } from "date-fns/locale"
import { Calendar as CalendarIcon } from "lucide-react"
import { useTranslation } from "react-i18next"
import type { DateRange, PropsBase, PropsRange } from "react-day-picker"

import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { cn } from "@/lib/utils"

type RangeCalendarProps = PropsBase & PropsRange & {
    buttonVariant?: ComponentProps<typeof Button>["variant"]
}

interface DateRangePickerProps {
    selectedRange?: DateRange
    onSelectRange: (range: DateRange | undefined) => void
    placeholder: string
    variant?: "search" | "form"
    resetLabel?: string
    error?: string
    open?: boolean
    onOpenChange?: (open: boolean) => void
    onResetDates?: () => void
    dateFormat?: string
    closeOnCompleteSelection?: boolean
    buttonClassName?: string
    iconClassName?: string
    labelClassName?: string
    popoverContentClassName?: string
    popoverAlign?: ComponentProps<typeof PopoverContent>["align"]
    calendarProps?: Omit<RangeCalendarProps, "mode" | "selected" | "onSelect" | "footer">
    footer?: ReactNode
}

export function DateRangePicker({
    selectedRange,
    onSelectRange,
    placeholder,
    resetLabel,
    error,
    open,
    onOpenChange,
    onResetDates,
    dateFormat = "LLL dd",
    variant = "search",
    closeOnCompleteSelection = true,
    buttonClassName,
    iconClassName,
    labelClassName,
    popoverContentClassName,
    popoverAlign = "start",
    calendarProps,
    footer,
}: DateRangePickerProps) {
    const { i18n } = useTranslation()
    const [internalOpen, setInternalOpen] = useState(false)
    const popoverOpen = open ?? internalOpen
    const setPopoverOpen = onOpenChange ?? setInternalOpen
    const dateLocale = i18n.language?.startsWith("es") ? es : enUS

    const selectedRangeLabel = selectedRange?.from
        ? selectedRange.to
            ? `${format(selectedRange.from, dateFormat, { locale: dateLocale })} - ${format(selectedRange.to, dateFormat, { locale: dateLocale })}`
            : format(selectedRange.from, dateFormat, { locale: dateLocale })
        : placeholder
    const isSearchVariant = variant === "search"

    const resetFooter = resetLabel && onResetDates ? (
        <div className="flex justify-end border-t border-border p-3">
            <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={onResetDates}
                className="text-xs text-muted-foreground hover:text-primary"
            >
                {resetLabel}
            </Button>
        </div>
    ) : null

    return (
        <>
            <Popover open={popoverOpen} onOpenChange={setPopoverOpen}>
                <PopoverTrigger asChild>
                    <Button
                        type="button"
                        variant="outline"
                        className={cn(
                            "w-full h-12 justify-start text-left font-normal",
                            isSearchVariant
                                ? "relative border-0 bg-muted/50 pl-12 hover:bg-muted/70"
                                : "gap-2 border bg-background px-4 hover:border-primary/50 hover:bg-muted/50 hover:text-foreground",
                            !selectedRange?.from && "text-muted-foreground",
                            buttonClassName
                        )}
                    >
                        <CalendarIcon
                            className={cn(
                                isSearchVariant
                                    ? "absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground"
                                    : "h-4 w-4 shrink-0 text-muted-foreground",
                                iconClassName
                            )}
                        />
                        <span className={cn("truncate", isSearchVariant && "pl-8", labelClassName)}>
                            {selectedRangeLabel}
                        </span>
                    </Button>
                </PopoverTrigger>
                <PopoverContent className={cn("w-auto p-0", popoverContentClassName)} align={popoverAlign}>
                    <Calendar
                        autoFocus
                        mode="range"
                        selected={selectedRange}
                        onSelect={(range) => {
                            onSelectRange(range)
                            if (
                                closeOnCompleteSelection
                                && range?.from
                                && range?.to
                                && range.from.getTime() !== range.to.getTime()
                            ) {
                                setTimeout(() => setPopoverOpen(false), 200)
                            }
                        }}
                        footer={footer ?? resetFooter}
                        {...calendarProps}
                    />
                </PopoverContent>
            </Popover>
            {error && (
                <p className="mt-2 text-sm text-destructive" role="alert">
                    {error}
                </p>
            )}
        </>
    )
}
