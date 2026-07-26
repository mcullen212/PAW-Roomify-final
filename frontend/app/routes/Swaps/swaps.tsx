import { useCallback, useEffect, useMemo, useState } from "react"
import { useLocation } from "react-router"
import type { DateRange } from "react-day-picker"
import { toast } from "sonner"
import { useTranslation } from "react-i18next"
import { Navbar } from "@/components/Navbar"
import { PagingBar } from "@/components/PagingBar"
import { Badge } from "@/components/ui/badge"
import { Card } from "@/components/ui/card"
import type { Contact } from "@/lib/interfaces/contacts"
import { ContactActionDialog } from "@/components/swaps/contact-action-dialog"
import { ReviewDialog } from "@/components/swaps/review-dialog"
import {
    SwapCard,
    SwapsEmptyState,
    SwapsErrorState,
    SwapsHero,
    SwapsLoadingState,
    SwapsTabs,
} from "@/components/swaps/swaps-list-sections"
import { useApiServices } from "~/lib/hooks/useApiServices"
import { useSwapRoomSummaries } from "~/lib/hooks/swaps/useSwapRoomSummaries"
import { useSwapReviewDialog } from "~/lib/hooks/swaps/useSwapReviewDialog"
import type {
    ContactActionMode,
    SwapTabId,
} from "~/lib/interfaces/swaps"
import { getPaginationWithFallback } from "~/lib/pagination"
import { useNormalizePaginationPage, usePaginationParams } from "~/lib/hooks/usePaginationParams"
import { useAuth } from "@/lib/auth/useAuth"
import {
    formatDateForApi,
    mapContactActionError,
    validateSwapAcceptanceRange,
} from "~/lib/swaps/swaps-utils"
import { getSwapTabFromSearch, swapTabs } from "~/lib/swaps/swaps-tabs"
import { pageTitleKey } from "@/lib/utils"
import { isRangeSelectable } from "@/lib/room-availability"
import i18n from "~/i18n/i18n"
import { currentPath } from "@/lib/navigation"

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.swaps") },
        { name: "description", content: i18n.t("pageDescriptions.swaps") },
    ]
}

const pageSize = 10

export default function SwapsPage() {
    const { searchParams, setSearchParams, currentPage } = usePaginationParams()
    const location = useLocation()
    const { api, contactService, reviewService } = useApiServices()
    const { t } = useTranslation()
    const { userId } = useAuth()
    const activeTab = getSwapTabFromSearch(searchParams)
    const contactsQuery = contactService.useGetContacts(activeTab, currentPage, pageSize)
    const updateContactMutation = contactService.useUpdateContact()
    const createReviewMutation = reviewService.useCreateReview()
    const contacts = useMemo(() => contactsQuery.data?.data ?? [], [contactsQuery.data])
    const returnTo = useMemo(() => currentPath(location), [location])
    const pagination = getPaginationWithFallback(contactsQuery.data?.pagination, currentPage)
    useNormalizePaginationPage(contactsQuery.data?.pagination, currentPage, (page, options) => {
        updateSwapsSearch(activeTab, page, options?.replace)
    })
    const isLoading = contactsQuery.isLoading || contactsQuery.isFetching
    const error = contactsQuery.error
        ? (contactsQuery.error as any)?.response?.data?.message || t("swaps.error.load")
        : null
    const roomsQuery = useSwapRoomSummaries(api, contacts, !contactsQuery.isError)
    const roomsById = roomsQuery.data ?? {}
    const [selectedContact, setSelectedContact] = useState<Contact | null>(null)
    const [actionMode, setActionMode] = useState<ContactActionMode | null>(null)
    const [selectedRange, setSelectedRange] = useState<DateRange | undefined>()
    const [datePopoverOpen, setDatePopoverOpen] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)
    const [submittingContactId, setSubmittingContactId] = useState<number | null>(null)

    const activeTabConfig = useMemo(
        () => swapTabs.find((tab) => tab.id === activeTab) ?? swapTabs[0],
        [activeTab],
    )

    const updateSwapsSearch = useCallback((tab: SwapTabId, page: number, replace = false) => {
        const nextSearchParams = new URLSearchParams(searchParams)
        nextSearchParams.set("view", tab)
        nextSearchParams.set("page", String(Math.max(page, 1)))
        setSearchParams(nextSearchParams, { replace })
    }, [searchParams, setSearchParams])

    useEffect(() => {
        const viewParam = searchParams.get("view")
        const pageParam = searchParams.get("page")

        if (viewParam !== activeTab || pageParam !== String(currentPage)) {
            updateSwapsSearch(activeTab, currentPage, true)
        }
    }, [activeTab, currentPage, searchParams, updateSwapsSearch])

    const handleTabChange = (tab: SwapTabId) => {
        updateSwapsSearch(tab, 1)
    }

    const handlePageChange = (newPage: number) => {
        updateSwapsSearch(activeTab, newPage)
    }

    const closeActionDialog = () => {
        setSelectedContact(null)
        setActionMode(null)
        setSelectedRange(undefined)
        setActionError(null)
        setDatePopoverOpen(false)
    }

    const handleAcceptClick = (contact: Contact) => {
        setSelectedContact(contact)
        setActionMode("accept")
        setSelectedRange(undefined)
        setActionError(null)
    }

    const handleRejectClick = (contact: Contact) => {
        setSelectedContact(contact)
        setActionMode("reject")
        setSelectedRange(undefined)
        setActionError(null)
    }

    const handleCancelClick = (contact: Contact) => {
        setSelectedContact(contact)
        setActionMode("cancel")
        setSelectedRange(undefined)
        setActionError(null)
    }

    const refreshAfterAction = useCallback(async () => {
        if (contacts.length === 1 && currentPage > 1) {
            updateSwapsSearch(activeTab, currentPage - 1, true)
            return
        }

        await contactsQuery.refetch()
    }, [activeTab, contacts.length, contactsQuery, currentPage, updateSwapsSearch])

    const handleContactActionError = async (actionErrorResponse: any, mode: ContactActionMode) => {
        const mappedError = mapContactActionError(
            actionErrorResponse,
            mode,
            mode === "accept" && Boolean(selectedContact?.isSwap),
            t,
        )

        setActionError(mappedError.message)

        if (mappedError.closeDialog) {
            closeActionDialog()
            toast.error(mappedError.message)
        }

        if (mappedError.refetch) {
            await refreshAfterAction()
        }
    }

    const submitAccept = async () => {
        if (!selectedContact || submittingContactId) {
            return
        }

        const isSwapAccept = selectedContact.isSwap
        const offeredRoom = selectedContact.roomOfferedId ? roomsById[selectedContact.roomOfferedId] : undefined

        if (isSwapAccept) {
            const dateValidationError = validateSwapAcceptanceRange(selectedContact, selectedRange, offeredRoom, t)
            if (dateValidationError) {
                setActionError(dateValidationError)
                return
            }
        }

        setSubmittingContactId(selectedContact.id)
        setActionError(null)

        try {
            await updateContactMutation.mutateAsync({
                contactId: selectedContact.id,
                contactData: isSwapAccept
                    ? {
                        status: "ACCEPTED",
                        checkIn: formatDateForApi(selectedRange!.from!),
                        checkOut: formatDateForApi(selectedRange!.to!),
                    }
                    : { status: "ACCEPTED" },
            })
            closeActionDialog()
            toast.success(isSwapAccept ? t("swapActions.acceptSwapSuccess") : t("swapActions.acceptMoneySuccess"))
            await refreshAfterAction()
        } catch (submitError: any) {
            await handleContactActionError(submitError, "accept")
        } finally {
            setSubmittingContactId(null)
        }
    }

    const submitReject = async () => {
        if (!selectedContact || submittingContactId) {
            return
        }

        setSubmittingContactId(selectedContact.id)
        setActionError(null)

        try {
            await updateContactMutation.mutateAsync({
                contactId: selectedContact.id,
                contactData: { status: "REJECTED" },
            })
            closeActionDialog()
            toast.success(t("swapActions.rejectSuccess"))
            await refreshAfterAction()
        } catch (submitError: any) {
            await handleContactActionError(submitError, "reject")
        } finally {
            setSubmittingContactId(null)
        }
    }

    const submitCancel = async () => {
        if (!selectedContact || submittingContactId) {
            return
        }

        setSubmittingContactId(selectedContact.id)
        setActionError(null)

        try {
            await updateContactMutation.mutateAsync({
                contactId: selectedContact.id,
                contactData: { status: "CANCELED" },
            })
            closeActionDialog()
            toast.success(t("swapActions.cancelSuccess"))
            await refreshAfterAction()
        } catch (submitError: any) {
            await handleContactActionError(submitError, "cancel")
        } finally {
            setSubmittingContactId(null)
        }
    }

    const reviewDialog = useSwapReviewDialog({
        createReviewMutation,
        reviewerId: userId,
        onReviewCreated: refreshAfterAction,
        t,
    })

    const selectedRequestedRoom = selectedContact ? roomsById[selectedContact.roomRequestedId] : undefined
    const selectedOfferedRoom = selectedContact?.roomOfferedId ? roomsById[selectedContact.roomOfferedId] : undefined
    const selectedReviewContact = reviewDialog.contact

    return (
        <div className="min-h-screen bg-[#f8fafc]">
            <Navbar />
            <SwapsHero />
            <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
                <section
                    aria-labelledby={`swaps-tab-${activeTabConfig.id}`}
                    id="swaps-panel"
                    role="tabpanel"
                >
                    <Card className="overflow-hidden border border-border bg-card py-0 shadow-sm">
                        <div className="border-b border-border px-4 py-4 sm:px-5">
                            <SwapsTabs
                                activeTab={activeTab}
                                tabs={swapTabs}
                                onTabChange={handleTabChange}
                            />
                        </div>

                        <div className="px-5 py-8 sm:px-8">
                            <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                                <div>
                                    <p className="text-sm font-semibold uppercase text-muted-foreground">
                                        {t("swaps.panel.eyebrow")}
                                    </p>
                                    <h2
                                        className="mt-2 text-3xl font-bold tracking-tight text-foreground"
                                        id="swaps-panel-title"
                                    >
                                        {t(activeTabConfig.title)}
                                    </h2>
                                </div>
                                <Badge className="bg-[#2563eb]/10 px-3 py-1 text-sm font-semibold text-[#2563eb]">
                                    {isLoading ? t("button.loading") : t("swaps.panel.requestCount", { count: contacts.length })}
                                </Badge>
                            </div>

                            {error ? (
                                <SwapsErrorState message={error} onRetry={() => contactsQuery.refetch()} />
                            ) : isLoading ? (
                                <SwapsLoadingState />
                            ) : contacts.length > 0 ? (
                                <>
                                    <div className="mt-8 grid gap-4">
                                        {contacts.map((contact) => (
                                            <SwapCard
                                                key={contact.id}
                                                contact={contact}
                                                requestedRoom={roomsById[contact.roomRequestedId]}
                                                offeredRoom={contact.roomOfferedId ? roomsById[contact.roomOfferedId] : undefined}
                                                activeTab={activeTab}
                                                userId={userId}
                                                isSubmitting={submittingContactId === contact.id || reviewDialog.submittingContactId === contact.id}
                                                returnTo={returnTo}
                                                onAccept={handleAcceptClick}
                                                onReject={handleRejectClick}
                                                onCancel={handleCancelClick}
                                                onReview={reviewDialog.open}
                                            />
                                        ))}
                                    </div>
                                    <PagingBar
                                        currentPage={pagination.currentPage}
                                        totalPages={pagination.totalPages}
                                        links={pagination.links}
                                        onPageChange={handlePageChange}
                                    />
                                </>
                            ) : (
                                <SwapsEmptyState
                                    icon={activeTabConfig.icon}
                                    message={t(activeTabConfig.emptyMessage)}
                                />
                            )}
                        </div>
                    </Card>
                </section>
            </main>
            <ContactActionDialog
                actionMode={actionMode}
                contact={selectedContact}
                requestedRoom={selectedRequestedRoom}
                offeredRoom={selectedOfferedRoom}
                selectedRange={selectedRange}
                datePopoverOpen={datePopoverOpen}
                actionError={actionError}
                isSubmitting={Boolean(selectedContact && submittingContactId === selectedContact.id)}
                onDatePopoverOpenChange={setDatePopoverOpen}
                onOpenChange={(open) => {
                    if (!open && !submittingContactId) {
                        closeActionDialog()
                    }
                }}
                onRangeChange={(range) => {
                    if (range?.from && range?.to && !isRangeSelectable(range, selectedOfferedRoom?.availabilityCalendar)) {
                        setSelectedRange(undefined)
                        setActionError(t("roomDetails.request.errors.unavailableDates"))
                        return
                    }

                    setSelectedRange(range)
                    setActionError(null)
                    if (range?.from && range?.to && range.from.getTime() !== range.to.getTime()) {
                        setTimeout(() => setDatePopoverOpen(false), 200)
                    }
                }}
                onAccept={submitAccept}
                onReject={submitReject}
                onCancel={submitCancel}
            />
            <ReviewDialog
                contact={selectedReviewContact}
                requestedRoom={selectedReviewContact ? roomsById[selectedReviewContact.roomRequestedId] : undefined}
                offeredRoom={selectedReviewContact?.roomOfferedId ? roomsById[selectedReviewContact.roomOfferedId] : undefined}
                userId={userId}
                rating={reviewDialog.rating}
                comment={reviewDialog.comment}
                error={reviewDialog.error}
                isSubmitting={Boolean(selectedReviewContact && reviewDialog.submittingContactId === selectedReviewContact.id)}
                onOpenChange={reviewDialog.handleOpenChange}
                onRatingChange={reviewDialog.handleRatingChange}
                onCommentChange={reviewDialog.handleCommentChange}
                onSubmit={reviewDialog.submit}
            />
        </div>
    )
}
