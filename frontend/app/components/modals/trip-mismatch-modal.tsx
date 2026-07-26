import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from "~/components/ui/dialog.tsx"
import { Button } from "~/components/ui/button.tsx"
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "~/components/ui/select.tsx"
import { CalendarPlus } from "lucide-react"
import type { GroupTripAssociationDTO } from "~/lib/interfaces/trips.ts"

interface TripMismatchModalProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    onConfirm: () => void
    title: string
    description: string
    tripLabel?: string
    confirmLabel: string
    cancelLabel: string
    matchingTrips?: GroupTripAssociationDTO[]
    selectedTripId?: number
    onSelectedTripIdChange?: (tripId: number) => void
}

export function TripMismatchModal({
                                      open,
                                      onOpenChange,
                                      onConfirm,
                                      title,
                                      description,
                                      tripLabel,
                                      confirmLabel,
                                      cancelLabel,
                                      matchingTrips = [],
                                      selectedTripId,
                                      onSelectedTripIdChange,
                                  }: TripMismatchModalProps) {
    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[440px]">
                <DialogHeader>
                    <div className="flex items-center gap-2 text-primary mb-2">
                        <CalendarPlus className="h-5 w-5" />
                        <DialogTitle>{title}</DialogTitle>
                    </div>
                    <DialogDescription>
                        {description}
                        {tripLabel && (
                            <span className="block mt-2 font-semibold text-foreground">
                                {tripLabel}
                            </span>
                        )}
                    </DialogDescription>
                </DialogHeader>
                {matchingTrips.length > 1 && (
                    <Select
                        value={selectedTripId ? String(selectedTripId) : undefined}
                        onValueChange={(value) => onSelectedTripIdChange?.(Number(value))}
                    >
                        <SelectTrigger className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            {matchingTrips.map((trip) => (
                                <SelectItem key={trip.tripId} value={String(trip.tripId)}>
                                    {trip.title} · {trip.country}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                )}
                <DialogFooter className="gap-2 sm:gap-0">
                    <Button
                        variant="ghost"
                        onClick={() => onOpenChange(false)}
                        className="rounded-full hover:bg-secondary hover:text-primary"
                    >
                        {cancelLabel}
                    </Button>
                    <Button
                        onClick={onConfirm}
                        className="rounded-full px-6 hover:shadow-md"
                    >
                        {confirmLabel}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
