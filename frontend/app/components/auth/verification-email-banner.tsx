import { AlertCircle } from "lucide-react"
import { useTranslation } from "react-i18next"
import { useAuth } from "~/lib/auth/useAuth"

interface VerificationEmailBannerProps {
  className?: string
}

export function VerificationEmailBanner({ className = "" }: VerificationEmailBannerProps) {
  const { t } = useTranslation()
  const { authenticated, loading, verified } = useAuth()

  const showBanner = !loading && authenticated && !verified

  if (!showBanner) return null

  return (
    <section className={`border-b border-amber-200 bg-amber-50 ${className}`}>
      <div className="container mx-auto flex flex-col gap-4 px-4 py-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-3">
          <AlertCircle className="h-5 w-5 shrink-0 text-amber-600" aria-hidden="true" />
          <div>
            <p className="font-medium text-amber-950">
              {t("exploreVerification.title")}
            </p>
            <p className="text-sm text-amber-900">
              {t("exploreVerification.description")}
            </p>
          </div>
        </div>
      </div>
    </section>
  )
}
