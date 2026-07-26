import { useEffect, useState } from "react"
import { Check, Info, Languages, SquarePen, X } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

interface LanguagesCardProps {
  primaryLanguage: string
  isSaving?: boolean
  onSave?: (locale: string) => void | Promise<void>
}

export function LanguagesCard({ primaryLanguage, isSaving = false, onSave }: LanguagesCardProps) {
  const { t } = useTranslation()
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState(primaryLanguage || "en")

  useEffect(() => {
    setDraft(primaryLanguage || "en")
  }, [primaryLanguage])

  const handleCancel = () => {
    setDraft(primaryLanguage || "en")
    setIsEditing(false)
  }

  const handleSave = async () => {
    await onSave?.(draft)
    setIsEditing(false)
  }

  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center gap-2 border-b border-border pb-4">
        <Languages className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-semibold text-foreground">{t("profile.languages.title")}</h2>
        {isEditing ? (
          <div className="ml-auto flex gap-2">
            <Button variant="outline" size="icon" className="h-8 w-8" onClick={handleCancel} disabled={isSaving}>
              <X className="h-4 w-4" />
            </Button>
            <Button size="icon" className="h-8 w-8" onClick={handleSave} disabled={isSaving}>
              <Check className="h-4 w-4" />
            </Button>
          </div>
        ) : (
          <Button variant="outline" size="icon" className="ml-auto h-8 w-8" onClick={() => setIsEditing(true)}>
            <SquarePen className="h-4 w-4" />
          </Button>
        )}
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between rounded-lg border border-border bg-background px-4 py-3">
          <span className="text-sm text-foreground">{t("profile.languages.primaryUiLanguage")}</span>
          {isEditing ? (
            <Select value={draft} onValueChange={setDraft} disabled={isSaving}>
              <SelectTrigger className="w-36">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="en">{t("profile.languages.english")}</SelectItem>
                <SelectItem value="es">{t("profile.languages.spanish")}</SelectItem>
              </SelectContent>
            </Select>
          ) : (
            <span className="font-medium text-foreground">
              {primaryLanguage.startsWith("es") ? t("profile.languages.spanish") : t("profile.languages.english")}
            </span>
          )}
        </div>

        <div className="flex items-start gap-2 text-sm text-muted-foreground">
          <Info className="mt-0.5 h-4 w-4 shrink-0" />
          <p>
            {t("profile.languages.description")}
          </p>
        </div>
      </div>
    </Card>
  )
}
