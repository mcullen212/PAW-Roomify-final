import { useEffect, type ReactNode } from "react"
import { Link } from "react-router"
import { LockKeyhole } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Button } from "~/components/ui/button"
import { useAuth } from "~/lib/auth/useAuth"

interface VerificationRequiredPopupProps {
  title: string
  description: string
  children: ReactNode
}

export function VerificationRequiredPopup({
  title,
  description,
  children,
}: VerificationRequiredPopupProps) {
  const { t } = useTranslation()
  const { email } = useAuth()
  const verificationUrl = `/verify-token?${new URLSearchParams({
    type: "verify",
    ...(email ? { email } : {}),
  }).toString()}`

  useEffect(() => {
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"

    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [])

  return (
    <>
      <div className="pointer-events-none select-none blur-[2px]" aria-hidden="true">
        {children}
      </div>

      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 px-4 backdrop-blur-[2px]">
        <section
          role="dialog"
          aria-modal="true"
          aria-labelledby="verification-required-title"
          aria-describedby="verification-required-description"
          className="w-full max-w-xl rounded-2xl border border-border bg-background px-8 py-12 text-center shadow-2xl sm:px-14"
        >
          <div className="mx-auto mb-7 flex h-20 w-20 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg shadow-blue-600/25">
            <LockKeyhole className="h-9 w-9" aria-hidden="true" />
          </div>

          <h1 id="verification-required-title" className="text-3xl font-bold tracking-tight text-foreground">
            {title}
          </h1>
          <p id="verification-required-description" className="mx-auto mt-4 max-w-md text-lg leading-7 text-muted-foreground">
            {description}
          </p>

          <div className="mt-9 flex flex-col items-center gap-4">
            <Button asChild>
              <Link to={verificationUrl}>{t("popUps.verification.enter_code")}</Link>
            </Button>
            <Button variant="ghost" asChild>
              <Link to="/profile">{t("popUps.verification.go_to_profile")}</Link>
            </Button>
          </div>
        </section>
      </div>
    </>
  )
}
