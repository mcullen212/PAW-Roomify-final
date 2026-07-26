import { Link } from "react-router"
import { useTranslation } from "react-i18next"
import { Calendar, DollarSign, Home, RefreshCw, UserRound } from "lucide-react"
import { Badge } from "@/components/ui/badge"
import type { Contact } from "@/lib/interfaces/contacts"
import {
    formatContactDate,
    formatMoney,
    formatRange,
    formatStatus,
    statusClass,
} from "@/lib/swaps/swaps-utils"

export function TripContactCard({ contact }: { contact: Contact }) {
    const { t, i18n } = useTranslation()
    const contactType = contact.isSwap ? t("swaps.contactType.swap") : t("swaps.contactType.money")
    const primaryParticipant = contact.offerUserName || contact.roomRequestedOwnerName || t("swaps.cards.participantUnavailable")
    const locale = i18n.language || "en"

    return (
        <article className="rounded-lg border border-border bg-background p-4 shadow-sm">
            <div className="flex flex-col gap-4">
                <div className="flex flex-wrap items-center gap-2">
                    <Badge className={statusClass(contact.status)}>
                        {formatStatus(contact.status, t)}
                    </Badge>
                    <Badge variant="outline">{contactType}</Badge>
                </div>

                <div className="grid gap-3 md:grid-cols-2">
                    <ContactRoomBlock
                        icon={Home}
                        label={t("swaps.dialog.requestedRoom")}
                        ownerName={contact.roomRequestedOwnerName}
                        range={formatRange(contact.requestedRange, t, locale)}
                        roomId={contact.roomRequestedId}
                    />
                    {contact.isSwap ? (
                        <ContactRoomBlock
                            icon={RefreshCw}
                            label={t("swaps.dialog.offeredRoom")}
                            ownerName={contact.roomOfferedOwnerName}
                            range={formatRange(contact.offeredRange, t, locale)}
                            roomId={contact.roomOfferedId}
                        />
                    ) : (
                        <div className="rounded-md border border-border bg-muted/30 px-4 py-3">
                            <div className="flex items-start gap-3">
                                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-emerald-100 text-emerald-700">
                                    <DollarSign className="h-5 w-5" aria-hidden="true" />
                                </div>
                                <div className="min-w-0">
                                    <p className="text-xs font-semibold uppercase text-muted-foreground">{t("swaps.contactType.money")}</p>
                                    <p className="mt-1 text-base font-semibold text-foreground">
                                        {formatMoney(contact.moneyOffer)}
                                    </p>
                                    <p className="mt-2 truncate text-sm text-muted-foreground">
                                        {contact.offerUserName ? t("swaps.cards.madeBy", { name: contact.offerUserName }) : t("tripDestinationContacts.offerUserUnavailable")}
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}
                </div>

                <div className="flex flex-wrap items-center gap-4 rounded-md bg-muted/40 px-4 py-3 text-sm text-muted-foreground">
                    <span className="flex min-w-0 items-center gap-2">
                        <UserRound className="h-4 w-4 shrink-0" aria-hidden="true" />
                        <span className="truncate">{primaryParticipant}</span>
                    </span>
                    <span className="flex items-center gap-2">
                        <Calendar className="h-4 w-4 shrink-0" aria-hidden="true" />
                        {formatContactDate(contact.contactDate, t, locale)}
                    </span>
                </div>
            </div>
        </article>
    )
}

function ContactRoomBlock({
    icon: Icon,
    label,
    ownerName,
    range,
    roomId,
}: {
    icon: typeof Home
    label: string
    ownerName?: string | null
    range: string
    roomId: number | null
}) {
    const { t } = useTranslation()
    const content = (
        <div className="flex items-start gap-3 rounded-md border border-border bg-card px-4 py-3 transition-colors hover:border-[#2563eb]/50 hover:bg-[#f8fafc]">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-[#2563eb]/10 text-[#2563eb]">
                <Icon className="h-5 w-5" aria-hidden="true" />
            </div>
            <div className="min-w-0">
                <p className="text-xs font-semibold uppercase text-muted-foreground">{label}</p>
                <p className="mt-1 truncate text-base font-semibold text-foreground">
                    {roomId ? t("swaps.cards.roomFallback", { id: roomId }) : t("swaps.dialog.roomUnavailable")}
                </p>
                <p className="truncate text-sm text-muted-foreground">
                    {ownerName ? t("tripDestinationContacts.owner", { name: ownerName }) : t("tripDestinationContacts.ownerUnavailable")}
                </p>
                <p className="mt-2 text-sm text-muted-foreground">{range}</p>
            </div>
        </div>
    )

    if (!roomId) return content

    return (
        <Link to={`/room/${roomId}`} className="block">
            {content}
        </Link>
    )
}
