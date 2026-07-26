import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
    DialogFooter,
} from "~/components/ui/dialog.tsx"
import { Button } from "~/components/ui/button.tsx"
import { AlertCircle, AlertTriangle } from "lucide-react"
import { useTranslation } from "react-i18next"

interface ConfirmDeleteModalProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    onConfirm: () => void
    title: string
    description: string
    itemName?: string
    isLoading?: boolean
    errorMessage?: string
}

export function ConfirmDeleteModal({
                                       open,
                                       onOpenChange,
                                       onConfirm,
                                       title,
                                       description,
                                   itemName,
                                   isLoading = false,
                                   errorMessage,
                                   }: ConfirmDeleteModalProps) {
    const { t } = useTranslation()

    return (
        <Dialog open={open} onOpenChange={(nextOpen) => {
            if (!isLoading) {
                onOpenChange(nextOpen)
            }
        }}>
            <DialogContent className="sm:max-w-[425px]">
                <DialogHeader>
                    <div className="flex items-center gap-2 text-destructive mb-2">
                        <AlertTriangle className="h-5 w-5" />
                        <DialogTitle>{title}</DialogTitle>
                    </div>
                    <DialogDescription>
                        {description}
                        {itemName && (
                            <span className="block mt-2 font-semibold text-foreground">
                "{itemName}"
              </span>
                        )}
                    </DialogDescription>
                </DialogHeader>
                {errorMessage && (
                    <div
                        className="flex gap-2 rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive"
                        role="alert"
                        aria-live="polite"
                    >
                        <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
                        <p>{errorMessage}</p>
                    </div>
                )}
                <DialogFooter className="gap-2 sm:gap-0">
                    <Button
                        variant="ghost"
                        onClick={() => onOpenChange(false)}
                        disabled={isLoading}
                        className="rounded-full hover:bg-secondary hover:text-primary"
                    >
                        {t("button.cancel")}
                    </Button>
                    <Button
                        variant="destructive"
                        onClick={() => {
                            onConfirm();
                        }}
                        disabled={isLoading}
                        className="rounded-full px-6 hover:shadow-md"
                    >
                        {isLoading ? t("button.deleting") : t("button.delete")}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}
