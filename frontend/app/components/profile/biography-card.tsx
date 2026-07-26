import { useEffect, useState } from "react"
import { Check, SquarePen, Users, X } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"

interface BiographyCardProps {
  biography?: string | null
  isSaving?: boolean
  onSave?: (biography: string) => void | Promise<void>
}

export function BiographyCard({ biography, isSaving = false, onSave }: BiographyCardProps) {
  const { t } = useTranslation()
  const [isEditing, setIsEditing] = useState(false)
  const [draft, setDraft] = useState(biography || "")

  useEffect(() => {
    setDraft(biography || "")
  }, [biography])

  const handleCancel = () => {
    setDraft(biography || "")
    setIsEditing(false)
  }

  const handleSave = async () => {
    await onSave?.(draft)
    setIsEditing(false)
  }

  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center gap-2 border-b border-border pb-4">
        <Users className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-semibold text-foreground">{t("profile.biography.title")}</h2>
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

      {isEditing ? (
        <Textarea
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          maxLength={500}
          disabled={isSaving}
          className="min-h-32 resize-none"
          autoFocus
        />
      ) : (
        <p className="leading-relaxed text-foreground">
          {biography || t("profile.biography.empty")}
        </p>
      )}
    </Card>
  )
}
