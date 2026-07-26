import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react"
import { MemoryRouter } from "react-router"
import { beforeEach, describe, expect, it, vi } from "vitest"
import type { DateRange } from "react-day-picker"
import type { Contact } from "@/lib/interfaces/contacts"
import SwapsPage from "../swaps"
import {
    canCancelContact,
    canReviewContact,
    createFallbackRoomSummary,
    formatDateForApi,
    getEarliestContactStartDate,
    getOtherUser,
    getSwapRoomIds,
    getRequestedNights,
    isContactParticipant,
    isMoreThanSevenDaysAway,
    isReceivedPendingOwner,
    mapRoomSummary,
    mapContactActionError,
    validateSwapAcceptanceRange,
} from "~/lib/swaps/swaps-utils"

const apiServicesMock = vi.hoisted(() => ({
    apiGet: vi.fn(),
    contactsRefetch: vi.fn(),
    updateContactMutateAsync: vi.fn(),
    createReviewMutateAsync: vi.fn(),
    contactsData: [] as Contact[],
}))

const authMock = vi.hoisted(() => ({
    userId: 22 as number | undefined,
}))

const toastMock = vi.hoisted(() => ({
    success: vi.fn(),
    error: vi.fn(),
}))

vi.mock("~/lib/hooks/useApiServices", () => ({
    useApiServices: () => ({
        api: {
            get: apiServicesMock.apiGet,
        },
        contactService: {
            useGetContacts: () => ({
                data: {
                    data: apiServicesMock.contactsData,
                    pagination: {
                        currentPage: 1,
                        totalPages: 1,
                        links: {
                            first: "",
                            prev: "",
                            next: "",
                            last: "",
                        },
                    },
                },
                error: null,
                isError: false,
                isLoading: false,
                isFetching: false,
                refetch: apiServicesMock.contactsRefetch,
            }),
            useUpdateContact: () => ({
                mutateAsync: apiServicesMock.updateContactMutateAsync,
            }),
        },
        reviewService: {
            useCreateReview: () => ({
                mutateAsync: apiServicesMock.createReviewMutateAsync,
            }),
        },
    }),
}))

vi.mock("@/lib/auth/useAuth", () => ({
    useAuth: () => authMock,
}))

vi.mock("sonner", () => ({
    toast: toastMock,
}))

vi.mock("@/components/Navbar", () => ({
    Navbar: () => <nav aria-label="Main navigation" />,
}))

const baseContact: Contact = {
    id: 1,
    contactDate: "2026-07-01T12:00:00",
    status: "PENDING",
    isSwap: true,
    moneyOffer: 0,
    requestedRange: {
        startDate: "2026-08-10",
        endDate: "2026-08-15",
    },
    offeredRange: null,
    offerUserId: 22,
    offerUserName: "Requester",
    roomRequestedId: 10,
    roomRequestedOwnerId: 7,
    roomRequestedOwnerName: "Owner",
    roomOfferedId: 20,
    roomOfferedOwnerId: 22,
    roomOfferedOwnerName: "Requester",
}

const offeredRoom = {
    id: 20,
    title: "Offered Room",
    location: "Buenos Aires, AR",
    imageUrl: "",
    ownerId: 22,
    availabilityCalendar: {
        roomId: 20,
        availabilityRanges: [
            {
                startDate: "2026-08-01",
                endDate: "2026-08-31",
            },
        ],
        bookedRanges: [
            {
                startDate: "2026-08-08",
                endDate: "2026-08-09",
            },
        ],
        selectableRanges: [
            {
                startDate: "2026-08-01",
                endDate: "2026-08-07",
            },
            {
                startDate: "2026-08-10",
                endDate: "2026-08-31",
            },
        ],
        firstSelectableDate: "2026-08-01",
        hasSelectableStay: true,
    },
}

const requestedRoom = {
    id: 10,
    title: "Requested Room",
    location: "Buenos Aires, AR",
    imageUrl: "",
    ownerId: 7,
    availabilityCalendar: null,
}

function dateDaysFromNow(days: number) {
    const date = new Date()
    date.setDate(date.getDate() + days)
    date.setHours(0, 0, 0, 0)

    return formatDateForApi(date)
}

function contactWithStartDate(contact: Contact, daysFromNow: number): Contact {
    const startDate = dateDaysFromNow(daysFromNow)
    const endDate = dateDaysFromNow(daysFromNow + 4)

    return {
        ...contact,
        requestedRange: {
            startDate,
            endDate,
        },
    }
}

function setupSwapsPage(
    contact: Contact,
    initialEntry = "/swaps?view=sent&page=1",
    userId = 22,
    roomOwnersById: Record<number, unknown> = {},
) {
    authMock.userId = userId
    apiServicesMock.contactsData = [contact]
    apiServicesMock.contactsRefetch.mockResolvedValue({})
    apiServicesMock.apiGet.mockImplementation((url: string) => {
        if (url.endsWith("/availabilities")) {
            const roomId = Number(url.split("/").at(-2))
            return Promise.resolve({
                data: roomId === 20
                    ? offeredRoom.availabilityCalendar
                    : requestedRoom.availabilityCalendar,
            })
        }

        const roomId = Number(url.split("/").pop())
        const owner = Object.prototype.hasOwnProperty.call(roomOwnersById, roomId)
            ? roomOwnersById[roomId]
            : `/users/${roomId === 10 ? 7 : 22}`

        return Promise.resolve({
            data: {
                title: roomId === 10 ? "Requested Room" : "Offered Room",
                city: "Buenos Aires",
                country: "Argentina",
                imageUrl: "",
                owner,
            },
        })
    })

    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })
    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <SwapsPage />
            </MemoryRouter>
        </QueryClientProvider>,
    )
}

beforeEach(() => {
    vi.clearAllMocks()
    authMock.userId = 22
    apiServicesMock.contactsData = []
})

describe("swap participant profile links", () => {
    it("resolves the offer user when the current user owns the requested room", () => {
        expect(getOtherUser(baseContact, requestedRoom, 7)).toEqual({
            id: 22,
            name: "Requester",
        })
    })

    it("resolves the requested room owner when the current user made the offer", () => {
        expect(getOtherUser(baseContact, requestedRoom, 22)).toEqual({
            id: 7,
            name: "Owner",
        })
    })

    it("renders With user as a link to the public profile when the other user resolves", async () => {
        setupSwapsPage(baseContact, "/swaps?view=sent&page=1", 22)

        const participantLink = await screen.findByRole("link", { name: "With Owner" })

        expect(participantLink.getAttribute("href")).toBe("/users/7")
    })

    it("keeps participant unavailable as plain text when the other user cannot be resolved", () => {
        setupSwapsPage({
            ...baseContact,
            offerUserId: null,
            offerUserName: null,
            roomRequestedOwnerId: null,
            roomRequestedOwnerName: null,
            roomOfferedId: null,
            roomOfferedOwnerId: null,
            roomOfferedOwnerName: null,
        }, "/swaps?view=sent&page=1", 22, { 10: null })

        expect(screen.getByText("Participant unavailable")).toBeTruthy()
        expect(screen.queryByRole("link", { name: "Participant unavailable" })).toBeNull()
    })
})

describe("swaps accept/reject helpers", () => {
    const t = (key: string, options?: Record<string, unknown>) => {
        const messages: Record<string, string> = {
            "swapActions.validation.selectOfferedRoomDates": "Select check-in and check-out dates for the offered room.",
            "swapActions.validation.unavailableDates": "Choose dates inside availability and outside booked ranges.",
            "swapActions.validation.nightLimit": `Choose ${options?.count} nights or fewer for the offered room.`,
        }

        return messages[key] ?? key
    }

    it("shows actions only for received pending requests owned by the current user", () => {
        expect(isReceivedPendingOwner(baseContact, undefined, "received", 7)).toBe(true)
        expect(isReceivedPendingOwner(baseContact, undefined, "sent", 7)).toBe(false)
        expect(isReceivedPendingOwner({ ...baseContact, status: "ACCEPTED" }, undefined, "received", 7)).toBe(false)
        expect(isReceivedPendingOwner(baseContact, undefined, "received", 99)).toBe(false)
        expect(isReceivedPendingOwner({ ...baseContact, roomRequestedOwnerId: undefined }, { ownerId: 7 }, "received", 7)).toBe(true)
    })

    it("formats API dates and computes requested stay duration", () => {
        expect(formatDateForApi(new Date("2026-08-03T12:00:00"))).toBe("2026-08-03")
        expect(getRequestedNights(baseContact)).toBe(5)
    })

    it("validates offered-room availability for selected swap acceptance dates", () => {
        const validRange: DateRange = {
            from: new Date("2026-08-03T00:00:00"),
            to: new Date("2026-08-06T00:00:00"),
        }
        const unavailableRange: DateRange = {
            from: new Date("2026-07-30T00:00:00"),
            to: new Date("2026-08-02T00:00:00"),
        }
        const tooLongRange: DateRange = {
            from: new Date("2026-08-10T00:00:00"),
            to: new Date("2026-08-18T00:00:00"),
        }
        const bookedRange: DateRange = {
            from: new Date("2026-08-07T00:00:00"),
            to: new Date("2026-08-10T00:00:00"),
        }

        expect(validateSwapAcceptanceRange(baseContact, validRange, offeredRoom, t)).toBeNull()
        expect(validateSwapAcceptanceRange(baseContact, undefined, offeredRoom, t)).toBe("Select check-in and check-out dates for the offered room.")
        expect(validateSwapAcceptanceRange(baseContact, unavailableRange, offeredRoom, t)).toBe("Choose dates inside availability and outside booked ranges.")
        expect(validateSwapAcceptanceRange(baseContact, bookedRange, offeredRoom, t)).toBe("Choose dates inside availability and outside booked ranges.")
        expect(validateSwapAcceptanceRange(baseContact, tooLongRange, offeredRoom, t)).toBe("Choose 5 nights or fewer for the offered room.")
    })

    it("maps HTTP errors to specific user-facing behavior", () => {
        expect(mapContactActionError({ response: { status: 403 } }, "reject")).toEqual({
            message: "Only the requested room owner can manage this request.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 404 } }, "reject")).toEqual({
            message: "This request no longer exists.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 409, data: { message: "This contact is not pending" } } }, "reject")).toEqual({
            message: "This request was already handled.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 409, data: { message: "Dates selected are already booked." } } }, "accept", true)).toEqual({
            message: "Those dates are no longer available. Choose a different range.",
            closeDialog: false,
            refetch: false,
        })
        expect(mapContactActionError({ response: { status: 422, data: { message: "checkOut must be after checkIn" } } }, "accept", true)).toEqual({
            message: "checkOut must be after checkIn",
            closeDialog: false,
            refetch: false,
        })
    })
})

describe("swaps cancel helpers", () => {
    it("detects participants from offer user and requested room owner fallback", () => {
        expect(isContactParticipant(baseContact, requestedRoom, 22)).toBe(true)
        expect(isContactParticipant({ ...baseContact, roomRequestedOwnerId: undefined }, requestedRoom, 7)).toBe(true)
        expect(isContactParticipant(baseContact, requestedRoom, 99)).toBe(false)
        expect(isContactParticipant(baseContact, requestedRoom, undefined)).toBe(false)
    })

    it("computes the earliest contact start date", () => {
        expect(getEarliestContactStartDate({
            ...baseContact,
            requestedRange: { startDate: "2026-08-10", endDate: "2026-08-15" },
            offeredRange: { startDate: "2026-08-05", endDate: "2026-08-08" },
        })?.toISOString().slice(0, 10)).toBe("2026-08-05")
        expect(getEarliestContactStartDate({
            ...baseContact,
            requestedRange: null,
            offeredRange: null,
        })).toBeNull()
    })

    it("checks whether a date is more than seven days away", () => {
        expect(isMoreThanSevenDaysAway(new Date(`${dateDaysFromNow(8)}T00:00:00`))).toBe(true)
        expect(isMoreThanSevenDaysAway(new Date(`${dateDaysFromNow(7)}T00:00:00`))).toBe(false)
    })

    it("does not show cancel for sent pending requests", () => {
        expect(canCancelContact(baseContact, requestedRoom, "sent", 22)).toBe(false)
        expect(canCancelContact(baseContact, requestedRoom, "sent", 7)).toBe(false)
        expect(canCancelContact(baseContact, requestedRoom, "received", 7)).toBe(false)
    })

    it("shows cancel for active accepted participants only when the stay is more than seven days away", () => {
        const acceptedContact = contactWithStartDate({
            ...baseContact,
            status: "ACCEPTED",
            offeredRange: {
                startDate: dateDaysFromNow(10),
                endDate: dateDaysFromNow(14),
            },
        }, 12)
        const soonAcceptedContact = contactWithStartDate({
            ...acceptedContact,
            offeredRange: {
                startDate: dateDaysFromNow(4),
                endDate: dateDaysFromNow(8),
            },
        }, 6)

        expect(canCancelContact(acceptedContact, requestedRoom, "active", 22)).toBe(true)
        expect(canCancelContact(acceptedContact, requestedRoom, "active", 7)).toBe(true)
        expect(canCancelContact(acceptedContact, requestedRoom, "active", 99)).toBe(false)
        expect(canCancelContact(soonAcceptedContact, requestedRoom, "active", 22)).toBe(false)
    })

    it("does not show cancel for terminal or unsupported tabs and statuses", () => {
        ;(["CANCELED", "EXPIRED", "REJECTED"] as const).forEach((status) => {
            expect(canCancelContact({ ...baseContact, status }, requestedRoom, "sent", 22)).toBe(false)
        })

        expect(canCancelContact({ ...baseContact, status: "ACCEPTED" }, requestedRoom, "canceled", 22)).toBe(false)
        expect(canCancelContact({ ...baseContact, status: "ACCEPTED" }, requestedRoom, "expired", 22)).toBe(false)
        expect(canCancelContact({ ...baseContact, status: "ACCEPTED" }, requestedRoom, "past", 22)).toBe(false)
    })

    it("does not show cancel for active accepted swaps whose earliest stay date is in the past", () => {
        expect(canCancelContact({
            ...baseContact,
            status: "ACCEPTED",
            requestedRange: {
                startDate: "2025-08-10",
                endDate: "2025-08-15",
            },
            offeredRange: {
                startDate: "2025-08-12",
                endDate: "2025-08-16",
            },
        }, requestedRoom, "active", 22)).toBe(false)
    })

    it("shows review only for pending accepted past contacts where the current user participates", () => {
        const reviewableContact = {
            ...baseContact,
            status: "ACCEPTED",
            pendingReview: true,
        } as Contact

        expect(canReviewContact(reviewableContact, requestedRoom, "past", 22)).toBe(true)
        expect(canReviewContact({ ...reviewableContact, pendingReview: false }, requestedRoom, "past", 22)).toBe(false)
        expect(canReviewContact({ ...reviewableContact, status: "PENDING" }, requestedRoom, "past", 22)).toBe(false)
        expect(canReviewContact(reviewableContact, requestedRoom, "active", 22)).toBe(false)
        expect(canReviewContact(reviewableContact, requestedRoom, "past", 99)).toBe(false)
    })

    it("maps cancel HTTP errors to specific user-facing behavior", () => {
        expect(mapContactActionError({ response: { status: 400 } }, "cancel")).toEqual({
            message: "We could not process that cancellation request. Please try again.",
            closeDialog: false,
            refetch: false,
        })
        expect(mapContactActionError({ response: { status: 403 } }, "cancel")).toEqual({
            message: "Only swap participants can cancel this request.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 404 } }, "cancel")).toEqual({
            message: "This request no longer exists.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 409, data: { message: "Cancellations can only be made more than 7 days in advance." } } }, "cancel")).toEqual({
            message: "Cancellations are only available more than 7 days before the stay.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 409, data: { message: "Contact is already canceled" } } }, "cancel")).toEqual({
            message: "This request was already handled.",
            closeDialog: true,
            refetch: true,
        })
        expect(mapContactActionError({ response: { status: 422 } }, "cancel")).toEqual({
            message: "This request could not be canceled.",
            closeDialog: false,
            refetch: false,
        })
    })
})

describe("swap room summary helpers", () => {
    it("extracts unique requested and offered room ids from contacts", () => {
        expect(getSwapRoomIds([
            baseContact,
            {
                ...baseContact,
                id: 2,
                roomRequestedId: 10,
                roomOfferedId: null,
            },
            {
                ...baseContact,
                id: 3,
                roomRequestedId: 30,
                roomOfferedId: 20,
            },
        ])).toEqual([10, 20, 30])
    })

    it("maps room API data into a room summary", () => {
        expect(mapRoomSummary(20, {
            title: "Offered Room",
            city: "Buenos Aires",
            country: "Argentina",
            imageUrl: "/images/20.jpg",
            owner: "/users/22",
        }, offeredRoom.availabilityCalendar)).toEqual({
            id: 20,
            title: "Offered Room",
            location: "Buenos Aires, Argentina",
            imageUrl: "/images/20.jpg",
            ownerId: 22,
            availabilityCalendar: offeredRoom.availabilityCalendar,
        })
    })

    it("creates a fallback summary when a room fetch fails", () => {
        expect(createFallbackRoomSummary(99)).toEqual({
            id: 99,
            title: "Room #99",
            location: "",
            imageUrl: "",
            ownerId: null,
            availabilityCalendar: null,
        })
        expect(getSwapRoomIds([])).toEqual([])
    })
})

describe("SwapsPage cancel flow", () => {
    it("opens the cancel dialog and patches with canceled status only", async () => {
        const contact = contactWithStartDate(baseContact, 12)
        apiServicesMock.updateContactMutateAsync.mockResolvedValue({ ...contact, status: "CANCELED" })

        setupSwapsPage({ ...contact, status: "ACCEPTED" }, "/swaps?view=active&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Cancel swap" }))
        expect((await screen.findByRole("dialog")).textContent).toContain("Confirm that you want to cancel this request or active swap.")

        const cancelButtons = screen.getAllByRole("button", { name: "Cancel swap" })
        fireEvent.click(cancelButtons[cancelButtons.length - 1])

        await waitFor(() => {
            expect(apiServicesMock.updateContactMutateAsync).toHaveBeenCalledWith({
                contactId: contact.id,
                contactData: { status: "CANCELED" },
            })
        })
        expect(apiServicesMock.updateContactMutateAsync.mock.calls[0][0].contactData).not.toHaveProperty("checkIn")
        expect(apiServicesMock.updateContactMutateAsync.mock.calls[0][0].contactData).not.toHaveProperty("checkOut")
    })

    it("disables cancel submit while sending and prevents double submission", async () => {
        const contact = contactWithStartDate(baseContact, 12)
        apiServicesMock.updateContactMutateAsync.mockImplementation(() => new Promise(() => undefined))

        setupSwapsPage({ ...contact, status: "ACCEPTED" }, "/swaps?view=active&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Cancel swap" }))
        const cancelButtons = await screen.findAllByRole("button", { name: "Cancel swap" })
        const submitButton = cancelButtons[cancelButtons.length - 1]

        fireEvent.click(submitButton)
        fireEvent.click(submitButton)

        await waitFor(() => expect(apiServicesMock.updateContactMutateAsync).toHaveBeenCalledTimes(1))
        expect((submitButton as HTMLButtonElement).disabled).toBe(true)
    })

    it("shows success toast and refetches the current tab after cancel", async () => {
        const contact = contactWithStartDate(baseContact, 12)
        apiServicesMock.updateContactMutateAsync.mockResolvedValue({ ...contact, status: "CANCELED" })

        setupSwapsPage({ ...contact, status: "ACCEPTED" }, "/swaps?view=active&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Cancel swap" }))
        const cancelButtons = screen.getAllByRole("button", { name: "Cancel swap" })
        fireEvent.click(cancelButtons[cancelButtons.length - 1])

        await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith("Swap canceled successfully."))
        await waitFor(() => expect(apiServicesMock.contactsRefetch).toHaveBeenCalledTimes(1))
    })

    it("shows cancel-specific errors and refetches when backend rejects cancellation", async () => {
        const contact = contactWithStartDate(baseContact, 12)
        apiServicesMock.updateContactMutateAsync.mockRejectedValue({ response: { status: 403 } })

        setupSwapsPage({ ...contact, status: "ACCEPTED" }, "/swaps?view=active&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Cancel swap" }))
        const cancelButtons = screen.getAllByRole("button", { name: "Cancel swap" })
        fireEvent.click(cancelButtons[cancelButtons.length - 1])

        await waitFor(() => {
            expect(toastMock.error).toHaveBeenCalledWith("Only swap participants can cancel this request.")
        })
        await waitFor(() => expect(apiServicesMock.contactsRefetch).toHaveBeenCalledTimes(1))
    })
})

describe("SwapsPage review flow", () => {
    const pastContact: Contact = {
        ...baseContact,
        status: "ACCEPTED",
        pendingReview: true,
        requestedRange: {
            startDate: "2025-08-10",
            endDate: "2025-08-15",
        },
        offeredRange: {
            startDate: "2025-08-12",
            endDate: "2025-08-16",
        },
    }

    it("shows leave review only in the past tab when backend marks it pending", async () => {
        const { unmount } = setupSwapsPage(pastContact, "/swaps?view=past&page=1")

        expect(await screen.findByRole("button", { name: "Leave review" })).toBeTruthy()

        unmount()
        setupSwapsPage({ ...pastContact, pendingReview: false }, "/swaps?view=past&page=1")
        expect(screen.queryByRole("button", { name: "Leave review" })).toBeNull()
    })

    it("does not show leave review outside the past tab", () => {
        setupSwapsPage(pastContact, "/swaps?view=active&page=1")

        expect(screen.queryByRole("button", { name: "Leave review" })).toBeNull()
    })

    it("submits a review, shows success toast, and refetches contacts", async () => {
        apiServicesMock.createReviewMutateAsync.mockResolvedValue({})
        setupSwapsPage(pastContact, "/swaps?view=past&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Leave review" }))
        fireEvent.click(await screen.findByRole("radio", { name: "5 stars" }))
        fireEvent.change(screen.getByLabelText("Comment"), {
            target: { value: "Very good experience." },
        })
        fireEvent.click(screen.getByRole("button", { name: "Submit review" }))

        await waitFor(() => {
            expect(apiServicesMock.createReviewMutateAsync).toHaveBeenCalledWith({
                contactId: pastContact.id,
                reviewerId: 22,
                rating: 5,
                comment: "Very good experience.",
            })
        })
        await waitFor(() => expect(toastMock.success).toHaveBeenCalledWith("Review submitted successfully."))
        await waitFor(() => expect(apiServicesMock.contactsRefetch).toHaveBeenCalledTimes(1))
    })

    it("allows common punctuation in review comments", async () => {
        const comment = "This couldn’t have gone better. Smooth and hassle-free process all around."

        apiServicesMock.createReviewMutateAsync.mockResolvedValue({})
        setupSwapsPage(pastContact, "/swaps?view=past&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Leave review" }))
        fireEvent.click(await screen.findByRole("radio", { name: "4 stars" }))
        fireEvent.change(screen.getByLabelText("Comment"), {
            target: { value: comment },
        })
        fireEvent.click(screen.getByRole("button", { name: "Submit review" }))

        await waitFor(() => {
            expect(apiServicesMock.createReviewMutateAsync).toHaveBeenCalledWith({
                contactId: pastContact.id,
                reviewerId: 22,
                rating: 4,
                comment,
            })
        })
        expect(screen.queryByText("Comment may only contain letters, numbers, spaces, and punctuation.")).toBeNull()
    })

    it("reviews the requested room when the current user owns the offered room", async () => {
        setupSwapsPage(pastContact, "/swaps?view=past&page=1", 22)

        fireEvent.click(await screen.findByRole("button", { name: "Leave review" }))
        const dialog = await screen.findByRole("dialog")

        expect(within(dialog).getByText("Requested Room")).toBeTruthy()
        expect(within(dialog).queryByText("Offered Room")).toBeNull()
    })

    it("reviews the offered room when the current user owns the requested room", async () => {
        setupSwapsPage(pastContact, "/swaps?view=past&page=1", 7)

        fireEvent.click(await screen.findByRole("button", { name: "Leave review" }))
        const dialog = await screen.findByRole("dialog")

        expect(within(dialog).getByText("Offered Room")).toBeTruthy()
        expect(within(dialog).queryByText("Requested Room")).toBeNull()
    })

    it("validates review rating before submitting", async () => {
        setupSwapsPage(pastContact, "/swaps?view=past&page=1")

        fireEvent.click(await screen.findByRole("button", { name: "Leave review" }))
        fireEvent.change(screen.getByLabelText("Comment"), {
            target: { value: "Very good experience." },
        })
        fireEvent.click(screen.getByRole("button", { name: "Submit review" }))

        expect(await screen.findByText("Select a rating from 1 to 5.")).toBeTruthy()
        expect(apiServicesMock.createReviewMutateAsync).not.toHaveBeenCalled()
    })
})
