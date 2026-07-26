import { Repeat, ArrowUpCircle, ArrowDownCircle, RefreshCw } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/components/ui/card"

interface SwapStatsProps {
  totalEarned: number
  totalSpent: number
  completedSwaps: number
}

export function SwapStats({ totalEarned, totalSpent, completedSwaps }: SwapStatsProps) {
  const { i18n, t } = useTranslation()

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat(i18n.language || "en", {
      style: "currency",
      currency: "USD",
      minimumFractionDigits: 2,
    }).format(amount)
  }

  return (
    <Card className="p-6">
      <div className="mb-6 flex items-center gap-2">
        <Repeat className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-semibold text-foreground">{t("profile.stats.title")}</h2>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-xl border border-border bg-background p-4">
          <div className="flex flex-col gap-2">
            <span className="text-sm text-muted-foreground">{t("profile.stats.totalEarned")}</span>
            <div className="flex items-center gap-2">
              <ArrowUpCircle className="h-5 w-5 text-green-500" />
              <span className="text-xl font-bold text-green-500">
                {formatCurrency(totalEarned)}
              </span>
            </div>
          </div>
        </div>

        <div className="rounded-xl border border-border bg-background p-4">
          <div className="flex flex-col gap-2">
            <span className="text-sm text-muted-foreground">{t("profile.stats.totalSpent")}</span>
            <div className="flex items-center gap-2">
              <ArrowDownCircle className="h-5 w-5 text-primary" />
              <span className="text-xl font-bold text-primary">
                {formatCurrency(totalSpent)}
              </span>
            </div>
          </div>
        </div>

        <div className="rounded-xl border border-border bg-background p-4">
          <div className="flex flex-col gap-2">
            <span className="text-sm text-muted-foreground">{t("profile.stats.totalSwaps")}</span>
            <div className="flex items-center gap-2">
              <RefreshCw className="h-5 w-5 text-foreground" />
              <span className="text-xl font-bold text-foreground">{completedSwaps}</span>
            </div>
          </div>
        </div>
      </div>
    </Card>
  )
}
