import { beforeEach, describe, expect, it, vi } from "vitest"
import type { AxiosInstance } from "axios"
import contactApi, { createContactAPI } from "../contactAPI"
import type { CreateContactPayload } from "~/lib/interfaces/contacts"
import { VndType } from "../vndTypes"

const apiMock = vi.hoisted(() => ({
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
}))

vi.mock("../api", () => ({
    default: apiMock,
}))

describe("contactApi", () => {
    beforeEach(() => {
        apiMock.get.mockReset()
        apiMock.post.mockReset()
        apiMock.patch.mockReset()
    })

    it("keeps the default singleton export wired to the shared api client", () => {
        contactApi.updateContact(12, { status: "ACCEPTED" })

        expect(apiMock.patch).toHaveBeenCalledWith("/contacts/12", { status: "ACCEPTED" }, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        })
    })
})

describe("createContactAPI", () => {
    const createPayload: CreateContactPayload = {
        roomRequestedId: 4,
        checkIn: "2026-08-01",
        checkOut: "2026-08-05",
        isSwap: true,
        roomOfferedId: 8,
    }

    function setupApi() {
        const client = {
            get: vi.fn(),
            post: vi.fn(),
            patch: vi.fn(),
        }
        return {
            api: createContactAPI(client as unknown as AxiosInstance),
            client,
        }
    }

    it("gets paginated contacts from the injected client", () => {
        const { api, client } = setupApi()

        api.getContacts("sent", 2, 20)

        expect(client.get).toHaveBeenCalledWith("/contacts", {
            headers: {
                Accept: VndType.APPLICATION_CONTACT,
            },
            params: {
                view: "sent",
                page: 2,
                pageSize: 20,
            },
        })
    })

    it("gets contact detail from the injected client", () => {
        const { api, client } = setupApi()

        api.getContactById(12)

        expect(client.get).toHaveBeenCalledWith("/contacts/12", {
            headers: {
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        })
    })

    it("creates a contact without a trip id through the injected client", () => {
        const { api, client } = setupApi()

        api.createContact(createPayload)

        expect(client.post).toHaveBeenCalledWith("/contacts", createPayload, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
            params: undefined,
        })
    })

    it("creates a contact with a trip id through the injected client", () => {
        const { api, client } = setupApi()

        api.createContact(createPayload, 5)

        expect(client.post).toHaveBeenCalledWith("/contacts", createPayload, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
            params: { tripId: 5 },
        })
    })

    it("sends accept money updates without dates", () => {
        const { api, client } = setupApi()

        api.updateContact(12, { status: "ACCEPTED" })

        expect(client.patch).toHaveBeenCalledWith("/contacts/12", { status: "ACCEPTED" }, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        })
    })

    it("sends accept swap updates with selected dates", () => {
        const { api, client } = setupApi()

        api.updateContact(
            12,
            {
                status: "ACCEPTED",
                checkIn: "2026-08-03",
                checkOut: "2026-08-06",
            },
        )

        expect(client.patch).toHaveBeenCalledWith(
            "/contacts/12",
            {
                status: "ACCEPTED",
                checkIn: "2026-08-03",
                checkOut: "2026-08-06",
            },
            {
                headers: {
                    "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                    Accept: VndType.APPLICATION_CONTACT_DETAIL,
                },
            },
        )
    })

    it("sends reject updates without dates", () => {
        const { api, client } = setupApi()

        api.updateContact(12, { status: "REJECTED" })

        expect(client.patch).toHaveBeenCalledWith("/contacts/12", { status: "REJECTED" }, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        })
    })

    it("sends cancel updates without dates", () => {
        const { api, client } = setupApi()

        api.updateContact(12, { status: "CANCELED" })

        expect(client.patch).toHaveBeenCalledWith("/contacts/12", { status: "CANCELED" }, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        })
    })
})
