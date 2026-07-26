import { Link } from "react-router"
import { MessageSquare, PenLine, Star, User, type LucideIcon } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/components/ui/card"

type ProfileHeaderMetric = {
  label: string
  value: number | string
  icon?: LucideIcon
}

interface ProfileHeaderProps {
  name: string
  rating: number
  reviewCount?: number
  writtenReviewCount?: number
  extraMetrics?: ProfileHeaderMetric[]
  title?: string
  reviewsHref?: string
}

export function ProfileHeader({
  name,
  rating,
  reviewCount,
  writtenReviewCount,
  extraMetrics,
  title,
  reviewsHref,
}: ProfileHeaderProps) {
  const { t } = useTranslation()
  const reviewLabel = t("profile.header.reviews")
  const writtenReviewLabel = t("profile.header.writtenReviews")
  const reviewAriaLabel = t("profile.header.viewReviews", { count: reviewCount ?? 0 })
  const metricClassName = "flex min-h-32 w-36 flex-col items-center justify-start rounded-xl border border-border bg-card p-4 shadow-sm"
  const linkedMetricClassName = `${metricClassName} transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2563eb] focus-visible:ring-offset-2 cursor-pointer hover:border-[#2563eb]/50 hover:bg-[#2563eb]/5 hover:shadow-md`

  function MetricContent({
    icon: Icon,
    iconClassName = "text-primary",
    label,
    value,
  }: ProfileHeaderMetric & { iconClassName?: string }) {
    const labelWords = label.trim().split(/\s+/)

    return (
      <>
        {Icon ? <Icon className={`mb-1 h-5 w-5 ${iconClassName}`} /> : null}
        <span className="text-2xl font-bold leading-tight text-foreground">{value}</span>
        <span className="text-center text-sm leading-tight text-muted-foreground">
          {labelWords.map((word, index) => (
            <span className="block" key={`${word}-${index}`}>
              {word}
            </span>
          ))}
        </span>
      </>
    )
  }

  return (
    <Card className="p-6">
      <div className="flex flex-col items-center justify-between gap-6 sm:flex-row">
        <div className="flex items-center gap-6">
          <div className="flex h-24 w-24 items-center justify-center rounded-full bg-primary/10 ring-4 ring-primary/20">
            <User className="h-12 w-12 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">
              {title || t("profile.header.welcome", { name })}
            </h1>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-4 sm:justify-end">
          <div className={metricClassName}>
            <MetricContent
              icon={Star}
              iconClassName="fill-yellow-400 text-yellow-400"
              label={t("profile.header.rating")}
              value={rating.toFixed(2)}
            />
          </div>
          
          {typeof reviewCount === "number" && reviewsHref ? (
            <Link
              to={reviewsHref}
              aria-label={reviewAriaLabel}
              title={reviewAriaLabel}
              className={linkedMetricClassName}
            >
              <MetricContent icon={MessageSquare} label={reviewLabel} value={reviewCount} />
            </Link>
          ) : typeof reviewCount === "number" ? (
            <div className={metricClassName}>
              <MetricContent icon={MessageSquare} label={reviewLabel} value={reviewCount} />
            </div>
          ) : null}

          {typeof writtenReviewCount === "number" ? (
            <div className={metricClassName}>
              <MetricContent icon={PenLine} label={writtenReviewLabel} value={writtenReviewCount} />
            </div>
          ) : null}

          {extraMetrics?.map((metric) => (
            <div className={metricClassName} key={metric.label}>
              <MetricContent {...metric} />
            </div>
          ))}
        </div>
      </div>
    </Card>
  )
}
