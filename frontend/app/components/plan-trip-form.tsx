import { useState } from "react"
import { useTranslation } from "react-i18next"
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogFooter,
} from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export interface PlanTripFormData {
    name: string
}

interface PlanTripFormProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    onSubmit: (data: PlanTripFormData) => void | Promise<void>
    submitting?: boolean
}

export function PlanTripForm({ open, onOpenChange, onSubmit, submitting = false }: PlanTripFormProps) {
    const { t } = useTranslation()
    const [error, setError] = useState("")
    const [formData, setFormData] = useState({
        name: "",
        travelers: 1,
        destinations: []
    })

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!formData.name.trim()) {
            setError(t("planTripForm.errors.nameRequired"))
            return
        }

        setError("")
        await onSubmit({
            name: formData.name,
        })
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <DialogTitle>{t("planTripForm.title")}</DialogTitle>
                </DialogHeader>
                <form onSubmit={handleSubmit} className="space-y-4 py-4">
                    {error && (
                        <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
                            {error}
                        </div>
                    )}
                    <div className="space-y-2">
                        <Label htmlFor="name">{t("planTripForm.fields.name")}</Label>
                        <Input
                            id="name"
                            placeholder={t("planTripForm.placeholders.name")}
                            value={formData.name}
                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                            required
                        />
                    </div>
                    <DialogFooter>
                        <Button type="submit" disabled={submitting} className="w-full bg-[#2563eb]">
                            {submitting ? t("planTripForm.actions.creating") : t("planTripForm.actions.create")}
                        </Button>
                    </DialogFooter>
                </form>
            </DialogContent>
        </Dialog>
    )
}
