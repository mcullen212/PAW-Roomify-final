import { useState } from "react"
import { AlertCircle, Check, CheckCircle2, Eye, EyeOff, SquarePen, UserCircle, X } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { getPasswordRequirements, isPasswordValid } from "@/lib/auth/password-policy"

interface AccountCardProps {
  email: string
  verified: boolean
  isSaving?: boolean
  onPasswordSave?: (oldPassword: string, newPassword: string) => void | Promise<void>
}

export function AccountCard({ email, verified, isSaving = false, onPasswordSave }: AccountCardProps) {
  const { t } = useTranslation()
  const [isEditingPassword, setIsEditingPassword] = useState(false)
  const [oldPassword, setOldPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [showOldPassword, setShowOldPassword] = useState(false)
  const [showNewPassword, setShowNewPassword] = useState(false)
  const passwordRequirements = getPasswordRequirements(newPassword)
  const canSavePassword = oldPassword.length > 0 && isPasswordValid(newPassword) && !isSaving

  const handleCancel = () => {
    setOldPassword("")
    setNewPassword("")
    setShowOldPassword(false)
    setShowNewPassword(false)
    setIsEditingPassword(false)
  }

  const handleSave = async () => {
    try {
      await onPasswordSave?.(oldPassword, newPassword)
      handleCancel()
    } catch {
      // The parent renders the error message and keeps the fields editable.
    }
  }

  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center justify-between gap-4 border-b border-border pb-4">
        <div className="flex items-center gap-2">
          <UserCircle className="h-5 w-5 text-primary" />
          <h2 className="text-lg font-semibold text-foreground">{t("profile.account.title")}</h2>
        </div>
        <span
          className={
            verified
              ? "inline-flex items-center gap-1.5 rounded-full bg-green-50 px-2.5 py-1 text-sm font-medium text-green-700"
              : "inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-2.5 py-1 text-sm font-medium text-amber-700"
          }
        >
          {verified ? (
            <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
          ) : (
            <AlertCircle className="h-4 w-4" aria-hidden="true" />
          )}
          {verified ? t("profile.account.verified") : t("profile.account.notVerified")}
        </span>
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between gap-4">
          <span className="text-sm text-muted-foreground">{t("profile.account.email")}</span>
          <span className="break-all text-right font-medium text-foreground">{email}</span>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">{t("profile.account.password")}</span>
            {isEditingPassword ? (
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  className="h-7 w-7"
                  onClick={handleCancel}
                  disabled={isSaving}
                  aria-label={t("profile.account.cancelPasswordEdit")}
                >
                  <X className="h-3.5 w-3.5" />
                </Button>
                <Button
                  size="icon"
                  className="h-7 w-7"
                  onClick={handleSave}
                  disabled={!canSavePassword}
                  aria-label={t("profile.account.savePassword")}
                >
                  <Check className="h-3.5 w-3.5" />
                </Button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <span className="font-medium text-foreground">******</span>
                <Button
                  variant="outline"
                  size="icon"
                  className="h-6 w-6"
                  onClick={() => setIsEditingPassword(true)}
                  aria-label={t("profile.account.editPassword")}
                >
                  <SquarePen className="h-3 w-3" />
                </Button>
              </div>
            )}
          </div>

          {isEditingPassword && (
            <div className="space-y-2">
              <div className="relative">
                <Input
                  type={showOldPassword ? "text" : "password"}
                  value={oldPassword}
                  onChange={(event) => setOldPassword(event.target.value)}
                  placeholder={t("profile.account.currentPassword")}
                  disabled={isSaving}
                  className="pr-10"
                  aria-invalid={oldPassword.length === 0}
                  aria-describedby="password-requirements"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-1 top-1/2 h-8 w-8 -translate-y-1/2"
                  onClick={() => setShowOldPassword((current) => !current)}
                  disabled={isSaving}
                  aria-label={showOldPassword ? t("profile.account.hideCurrentPassword") : t("profile.account.showCurrentPassword")}
                >
                  {showOldPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
              </div>
              <div className="relative">
                <Input
                  type={showNewPassword ? "text" : "password"}
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  placeholder={t("profile.account.newPassword")}
                  minLength={8}
                  disabled={isSaving}
                  className="pr-10"
                  aria-invalid={newPassword.length > 0 && !isPasswordValid(newPassword)}
                  aria-describedby="password-requirements"
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="absolute right-1 top-1/2 h-8 w-8 -translate-y-1/2"
                  onClick={() => setShowNewPassword((current) => !current)}
                  disabled={isSaving}
                  aria-label={showNewPassword ? t("profile.account.hideNewPassword") : t("profile.account.showNewPassword")}
                >
                  {showNewPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </Button>
              </div>
              <p id="password-requirements" className="text-xs text-muted-foreground">
                {t("profile.account.passwordRequirements")}
              </p>
              {newPassword.length > 0 && (
                <div className="grid grid-cols-1 gap-1.5">
                  {passwordRequirements.map((requirement) => (
                    <div key={requirement.id} className="flex items-center gap-2 text-xs">
                      <span
                        className={
                          requirement.met
                            ? "h-3.5 w-3.5 rounded-full bg-green-500"
                            : "h-3.5 w-3.5 rounded-full bg-muted-foreground/30"
                        }
                        aria-hidden="true"
                      />
                      <span className={requirement.met ? "text-foreground" : "text-muted-foreground"}>
                        {t(requirement.translationKey)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </Card>
  )
}
