import { Link } from "react-router";
import {
    CalendarDays,
    CheckCircle2,
    DollarSign,
    Home,
    RefreshCw,
    Star,
    UserRound,
    XCircle,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { Contact } from "@/lib/interfaces/contacts";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import type { RoomSummary, SwapTabConfig, SwapTabId } from "~/lib/interfaces/swaps";
import {
    canCancelContact,
    canReviewContact,
    formatContactDate,
    formatMoney,
    formatRange,
    formatStatus,
    getOtherUser,
    isReceivedPendingOwner,
    statusClass,
} from "~/lib/swaps/swaps-utils";
import { cn } from "@/lib/utils";
import { withReturnTo } from "@/lib/navigation";

export function SwapsHero() {
    const { t } = useTranslation();

    return (
        <section className="bg-gradient-to-r from-[#3b82f6] to-[#60a5fa] px-4 py-12 sm:px-6 lg:px-8">
            <div className="mx-auto max-w-7xl">
                <h1 className="text-4xl font-bold italic tracking-tight text-white">
                    {t("swaps.hero.title")}
                </h1>
                <p className="mt-4 max-w-4xl text-lg leading-8 text-white/90">
                    {t("swaps.hero.description")}
                </p>
            </div>
        </section>
    );
}

interface SwapsTabsProps {
    activeTab: SwapTabId;
    tabs: SwapTabConfig[];
    onTabChange: (tab: SwapTabId) => void;
}

export function SwapsTabs({ activeTab, tabs, onTabChange }: SwapsTabsProps) {
    const { t } = useTranslation();

    return (
        <div
            aria-label={t("swaps.tabs.ariaLabel")}
            className="flex gap-2 overflow-x-auto rounded-xl bg-muted/40 p-1"
            role="tablist"
        >
            {tabs.map((tab) => {
                const isActive = activeTab === tab.id;
                const Icon = tab.icon;

                return (
                    <button
                        aria-controls="swaps-panel"
                        aria-selected={isActive}
                        className={cn(
                            "flex min-w-fit cursor-pointer items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
                            isActive
                                ? "border-[#2563eb] bg-[#2563eb] text-white shadow-sm hover:bg-[#1d4ed8] hover:shadow-md"
                                : "border-transparent bg-transparent text-muted-foreground hover:border-[#2563eb]/20 hover:bg-background hover:text-[#2563eb] hover:shadow-sm",
                        )}
                        id={`swaps-tab-${tab.id}`}
                        key={tab.id}
                        onClick={() => onTabChange(tab.id)}
                        role="tab"
                        type="button"
                    >
                        <Icon className="h-4 w-4" aria-hidden="true" />
                        <span>{t(tab.label)}</span>
                    </button>
                );
            })}
        </div>
    );
}

interface SwapsEmptyStateProps {
    icon: LucideIcon;
    message: string;
}

export function SwapsEmptyState({ icon: Icon, message }: SwapsEmptyStateProps) {
    const { t } = useTranslation();

    return (
        <div className="mt-8 flex min-h-[300px] items-center justify-center rounded-xl border border-border bg-[#f8fafc] px-4 py-12">
            <div className="max-w-md text-center">
                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-2xl bg-[#2563eb]/10 text-[#2563eb]">
                    <Icon className="h-8 w-8" aria-hidden="true" />
                </div>
                <p className="mt-5 text-lg font-semibold text-foreground">
                    {message}
                </p>
                <p className="mt-2 text-sm leading-6 text-muted-foreground">
                    {t("swaps.empty.helper")}
                </p>
            </div>
        </div>
    );
}

export function SwapsLoadingState() {
    return (
        <div className="mt-8 grid gap-4">
            {[0, 1, 2].map((item) => (
                <div
                    className="h-36 animate-pulse rounded-lg border border-border bg-muted/40"
                    key={item}
                />
            ))}
        </div>
    );
}

interface SwapsErrorStateProps {
    message: string;
    onRetry: () => void;
}

export function SwapsErrorState({ message, onRetry }: SwapsErrorStateProps) {
    const { t } = useTranslation();

    return (
        <div className="mt-8 flex min-h-[260px] items-center justify-center rounded-xl border border-destructive/30 bg-destructive/5 px-4 py-12">
            <div className="max-w-md text-center">
                <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
                    <XCircle className="h-7 w-7" aria-hidden="true" />
                </div>
                <p className="mt-5 text-lg font-semibold text-foreground">{message}</p>
                <Button className="mt-5" onClick={onRetry} type="button" variant="outline">
                    <RefreshCw className="h-4 w-4" aria-hidden="true" />
                    {t("button.retry")}
                </Button>
            </div>
        </div>
    );
}

interface SwapCardProps {
    contact: Contact;
    requestedRoom?: RoomSummary;
    offeredRoom?: RoomSummary;
    activeTab: SwapTabId;
    userId?: number;
    isSubmitting: boolean;
    returnTo?: string;
    onAccept: (contact: Contact) => void;
    onReject: (contact: Contact) => void;
    onCancel: (contact: Contact) => void;
    onReview: (contact: Contact) => void;
}

export function SwapCard({
    contact,
    requestedRoom,
    offeredRoom,
    activeTab,
    userId,
    isSubmitting,
    returnTo,
    onAccept,
    onReject,
    onCancel,
    onReview,
}: SwapCardProps) {
    const { t } = useTranslation();
    const requestedRange = formatRange(contact.requestedRange);
    const offeredRange = formatRange(contact.offeredRange);
    const statusLabel = formatStatus(contact.status);
    const contactType = contact.isSwap ? t("swaps.contactType.swap") : t("swaps.contactType.money");
    const otherUser = getOtherUser(contact, requestedRoom, userId ?? null);
    const isRequestedRoomOwner = Boolean(
        userId && (contact.roomRequestedOwnerId ?? requestedRoom?.ownerId) === userId,
    );
    const canManageRequest = isReceivedPendingOwner(contact, requestedRoom, activeTab, userId);
    const canCancelRequest = canCancelContact(contact, requestedRoom, activeTab, userId);
    const canLeaveReview = canReviewContact(contact, requestedRoom, activeTab, userId);
    const requestedRoomLabel = isRequestedRoomOwner ? t("swaps.cards.yourRoom") : t("swaps.cards.roomYouRequested");
    const offeredRoomLabel = isRequestedRoomOwner ? t("swaps.cards.theirOfferedRoom") : t("swaps.cards.yourOfferedRoom");
    const moneyOfferLabel = isRequestedRoomOwner ? t("swaps.cards.theirMoneyOffer") : t("swaps.cards.yourMoneyOffer");

    return (
        <article className="rounded-lg border border-border bg-background p-5 shadow-sm transition-shadow hover:shadow-md">
            <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                        <Badge className={cn("px-2.5 py-1", statusClass(contact.status))}>
                            {statusLabel}
                        </Badge>
                        <Badge variant="outline" className="px-2.5 py-1">
                            {contactType}
                        </Badge>
                    </div>

                    <div className="mt-4 grid gap-3 md:grid-cols-2">
                        <RoomBlock
                            icon={Home}
                            label={requestedRoomLabel}
                            room={requestedRoom}
                            fallbackId={contact.roomRequestedId}
                            range={requestedRange}
                            returnTo={returnTo}
                        />
                        {contact.isSwap ? (
                            <RoomBlock
                                icon={RefreshCw}
                                label={offeredRoomLabel}
                                room={offeredRoom}
                                fallbackId={contact.roomOfferedId}
                                range={offeredRange}
                                returnTo={returnTo}
                            />
                        ) : (
                            <MoneyOfferBlock
                                amount={contact.moneyOffer}
                                label={moneyOfferLabel}
                                offerUserName={contact.offerUserName ?? undefined}
                            />
                        )}
                    </div>
                </div>

                <div className="grid gap-2 rounded-md bg-muted/40 px-4 py-3 text-sm text-muted-foreground lg:min-w-48">
                    <div className="flex items-center gap-2 font-medium text-foreground">
                        <UserRound className="h-4 w-4" aria-hidden="true" />
                        {otherUser ? (
                            <Link
                                className="min-w-0 truncate text-[#2563eb] transition-colors hover:text-[#1d4ed8] hover:underline focus-visible:rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2563eb] focus-visible:ring-offset-2"
                                to={`/users/${otherUser.id}`}
                            >
                                {t("swaps.cards.withUser", { name: otherUser.name })}
                            </Link>
                        ) : (
                            <span>{t("swaps.cards.participantUnavailable")}</span>
                        )}
                    </div>
                    <div className="flex items-center gap-2">
                        <CalendarDays className="h-4 w-4" aria-hidden="true" />
                        <span>{formatContactDate(contact.contactDate)}</span>
                    </div>
                    {canManageRequest ? (
                        <SwapActions
                            contact={contact}
                            isSubmitting={isSubmitting}
                            onAccept={onAccept}
                            onReject={onReject}
                        />
                    ) : null}
                    {canCancelRequest ? (
                        <CancelAction
                            contact={contact}
                            isSubmitting={isSubmitting}
                            onCancel={onCancel}
                        />
                    ) : null}
                    {canLeaveReview ? (
                        <ReviewAction
                            contact={contact}
                            isSubmitting={isSubmitting}
                            onReview={onReview}
                        />
                    ) : null}
                </div>
            </div>
        </article>
    );
}

interface SwapActionsProps {
    contact: Contact;
    isSubmitting: boolean;
    onAccept: (contact: Contact) => void;
    onReject: (contact: Contact) => void;
}

function SwapActions({ contact, isSubmitting, onAccept, onReject }: SwapActionsProps) {
    const { t } = useTranslation();

    return (
        <div className="mt-2 grid grid-cols-2 gap-2 border-t border-border pt-3">
            <Button
                className="bg-emerald-600 text-white hover:bg-emerald-700 hover:shadow-md"
                disabled={isSubmitting}
                onClick={() => onAccept(contact)}
                size="sm"
                type="button"
            >
                <CheckCircle2 className="h-4 w-4" aria-hidden="true" />
                {t("swapActions.accept")}
            </Button>
            <Button
                className="border-destructive/30 text-destructive hover:bg-destructive/10 hover:text-destructive hover:shadow-sm"
                disabled={isSubmitting}
                onClick={() => onReject(contact)}
                size="sm"
                type="button"
                variant="outline"
            >
                <XCircle className="h-4 w-4" aria-hidden="true" />
                {t("swapActions.reject")}
            </Button>
        </div>
    );
}

interface CancelActionProps {
    contact: Contact;
    isSubmitting: boolean;
    onCancel: (contact: Contact) => void;
}

function CancelAction({ contact, isSubmitting, onCancel }: CancelActionProps) {
    const { t } = useTranslation();

    return (
        <div className="mt-2 border-t border-border pt-3">
            <Button
                className="w-full border-destructive/30 text-destructive hover:bg-destructive/10 hover:text-destructive"
                disabled={isSubmitting}
                onClick={() => onCancel(contact)}
                size="sm"
                type="button"
                variant="outline"
            >
                <XCircle className="h-4 w-4" aria-hidden="true" />
                {t("swapActions.cancelSwap")}
            </Button>
        </div>
    );
}

interface ReviewActionProps {
    contact: Contact;
    isSubmitting: boolean;
    onReview: (contact: Contact) => void;
}

function ReviewAction({ contact, isSubmitting, onReview }: ReviewActionProps) {
    const { t } = useTranslation();

    return (
        <div className="mt-2 border-t border-border pt-3">
            <Button
                className="w-full border-[#2563eb]/30 text-[#2563eb] hover:bg-[#2563eb]/10 hover:text-[#1d4ed8]"
                disabled={isSubmitting}
                onClick={() => onReview(contact)}
                size="sm"
                type="button"
                variant="outline"
            >
                <Star className="h-4 w-4" aria-hidden="true" />
                {t("reviewActions.leaveReview")}
            </Button>
        </div>
    );
}

interface RoomBlockProps {
    icon: LucideIcon;
    label: string;
    room?: RoomSummary;
    fallbackId: number | null;
    range: string;
    returnTo?: string;
}

function RoomBlock({ icon: Icon, label, room, fallbackId, range, returnTo }: RoomBlockProps) {
    const { t } = useTranslation();
    const roomPath = fallbackId ? withReturnTo(`/room/${fallbackId}`, returnTo ?? "/swaps") : "#";

    return (
        <Link
            className="block rounded-md border border-border bg-card transition-colors hover:border-[#2563eb]/50 hover:bg-[#f8fafc]"
            to={roomPath}
        >
            <div className="flex items-stretch gap-3 p-3">
                {room?.imageUrl ? (
                    <img
                        alt={room.title}
                        className="h-24 w-28 shrink-0 rounded-md object-cover"
                        src={room.imageUrl}
                    />
                ) : (
                    <div className="flex h-24 w-28 shrink-0 items-center justify-center rounded-md bg-[#2563eb]/10 text-[#2563eb]">
                        <Icon className="h-7 w-7" aria-hidden="true" />
                    </div>
                )}
                <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase text-muted-foreground">{label}</p>
                    <h3 className="mt-1 truncate text-base font-semibold text-foreground">
                        {room?.title ?? (fallbackId ? t("swaps.cards.roomFallback", { id: fallbackId }) : t("swaps.cards.noRoomSelected"))}
                    </h3>
                    {room?.location ? (
                        <p className="truncate text-sm text-muted-foreground">{room.location}</p>
                    ) : null}
                    <p className="mt-2 text-sm text-muted-foreground">{range}</p>
                </div>
            </div>
        </Link>
    );
}

interface MoneyOfferBlockProps {
    amount: number;
    label: string;
    offerUserName?: string;
}

function MoneyOfferBlock({ amount, label, offerUserName }: MoneyOfferBlockProps) {
    const { t } = useTranslation();

    return (
        <div className="rounded-md border border-border bg-card px-4 py-3">
            <div className="flex items-start gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md bg-emerald-100 text-emerald-700">
                    <DollarSign className="h-5 w-5" aria-hidden="true" />
                </div>
                <div>
                    <p className="text-xs font-semibold uppercase text-muted-foreground">{label}</p>
                    <h3 className="mt-1 text-base font-semibold text-foreground">
                        {formatMoney(amount)}
                    </h3>
                    <p className="mt-2 text-sm text-muted-foreground">
                        {offerUserName ? t("swaps.cards.madeBy", { name: offerUserName }) : t("swaps.contactType.money")}
                    </p>
                </div>
            </div>
        </div>
    );
}
