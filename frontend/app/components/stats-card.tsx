import { Card } from "@/components/ui/card"
import type { LucideIcon } from "lucide-react"

interface StatsCardProps {
    label: string
    value: string | number
    icon?: LucideIcon
    badge?: string
    badgeColor?: "green" | "blue" | "amber"
    suffix?: string
}

const badgeColors = {
    green: "bg-emerald-100 text-emerald-700",
    blue: "bg-[#2563eb]/10 text-[#2563eb]",
    amber: "bg-amber-100 text-amber-700",
}

export function StatsCard({ label, value, icon: Icon, badge, badgeColor = "green", suffix }: StatsCardProps) {
    return (
        <Card className="p-4 bg-card border border-border">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                {label}
            </p>
            <div className="flex items-center gap-3">
                <span className="text-3xl font-bold text-foreground">{value}</span>
                {badge && (
                    <span className={`${badgeColors[badgeColor]} px-2 py-0.5 rounded-full text-xs font-medium`}>
            {badge}
          </span>
                )}
                {Icon && <Icon className="h-5 w-5 text-muted-foreground" />}
                {suffix && <span className="text-amber-500 text-lg">{suffix}</span>}
            </div>
        </Card>
    )
}
