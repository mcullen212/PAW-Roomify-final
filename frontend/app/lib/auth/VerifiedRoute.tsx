import { Outlet } from "react-router"
import { useTranslation } from "react-i18next"
import { VerificationRequiredPopup } from "~/components/auth/verification-required-popup"
import type { VerificationPopupCopyKey } from "~/lib/interfaces/auth"
import { useAuth } from "./useAuth"

export type { VerificationPopupCopyKey } from "~/lib/interfaces/auth"

interface VerifiedRouteProps {
  copyKey: VerificationPopupCopyKey
}

export function VerifiedRoute({ copyKey }: VerifiedRouteProps) {
  const { t } = useTranslation()
  const { loading, verified } = useAuth()

  if (loading) return null
  if (verified) return <Outlet />

  return (
    <VerificationRequiredPopup
      title={t(`${copyKey}.title`)}
      description={t(`${copyKey}.description`)}
    >
      <Outlet />
    </VerificationRequiredPopup>
  )
}
