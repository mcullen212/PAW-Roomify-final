import type { ReactNode } from "react";
import type { AxiosInstance } from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { Contact, CreateContactPayload, UpdateContactPayload } from "~/lib/interfaces/contacts";
import { VndType } from "~/lib/api/vndTypes.ts";
import { contactKeys, useContacts } from "../services/useContacts.ts";

function setup() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });
    const wrapper = ({ children }: { children: ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    return { queryClient, wrapper };
}

const contact: Contact = {
    id: 12,
    contactDate: "2026-07-16",
    status: "PENDING",
    isSwap: true,
    moneyOffer: 0,
    requestedRange: { startDate: "2026-08-01", endDate: "2026-08-05" },
    offeredRange: null,
    offerUserId: 3,
    roomRequestedId: 4,
    roomOfferedId: 8,
};

const createPayload: CreateContactPayload = {
    roomRequestedId: 4,
    checkIn: "2026-08-01",
    checkOut: "2026-08-05",
    isSwap: true,
    roomOfferedId: 8,
};

describe("useContacts", () => {
    it("gets a paginated contact list with parsed links and a stable key", async () => {
        const get = vi.fn().mockResolvedValue({
            data: [contact],
            headers: {
                link: '</contacts?view=sent&page=2&pageSize=10>; rel="next", </contacts?view=sent&page=3&pageSize=10>; rel="last"',
            },
        });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useContacts(api).useGetContacts("sent", 1, 10), {
            wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [contact],
            pagination: {
                currentPage: 1,
                totalPages: 3,
                links: {
                    first: "",
                    prev: "",
                    next: "/contacts?view=sent&page=2&pageSize=10",
                    last: "/contacts?view=sent&page=3&pageSize=10",
                },
            },
        });
        expect(get).toHaveBeenCalledWith("/contacts", {
            headers: { Accept: VndType.APPLICATION_CONTACT },
            params: { view: "sent", page: 1, pageSize: 10 },
        });
        expect(contactKeys.list("sent", 1, 10)).not.toEqual(contactKeys.list("received", 1, 10));
    });

    it("returns single-page pagination defaults when no Link header exists", async () => {
        const get = vi.fn().mockResolvedValue({ data: [], headers: {} });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useContacts(api).useGetContacts("sent", 1, 10), {
            wrapper,
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({
            data: [],
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
        });
    });

    it("gets a contact detail with the detail media type", async () => {
        const get = vi.fn().mockResolvedValue({ data: contact });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useContacts(api).useGetContactById(12), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual(contact);
        expect(get).toHaveBeenCalledWith("/contacts/12", {
            headers: { Accept: VndType.APPLICATION_CONTACT_DETAIL },
        });
    });

    it("does not request a contact detail without an id", () => {
        const get = vi.fn();
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useContacts(api).useGetContactById(), { wrapper });

        expect(result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("creates a contact without a trip and preserves the Location response", async () => {
        const response = { data: undefined, headers: { location: "/contacts/12" } };
        const post = vi.fn().mockResolvedValue(response);
        const api = { post } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const { result } = renderHook(() => useContacts(api).useCreateContact(), { wrapper });

        const returned = await result.current.mutateAsync({ contactData: createPayload });

        expect(returned).toBe(response);
        expect(post).toHaveBeenCalledWith("/contacts", createPayload, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
            params: undefined,
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: contactKeys.all });
    });

    it("passes an optional trip id when creating a contact", async () => {
        const post = vi.fn().mockResolvedValue({ data: undefined, headers: {} });
        const api = { post } as unknown as AxiosInstance;
        const { wrapper } = setup();
        const { result } = renderHook(() => useContacts(api).useCreateContact(), { wrapper });

        await result.current.mutateAsync({ contactData: createPayload, tripId: 5 });

        expect(post).toHaveBeenCalledWith("/contacts", createPayload, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
            params: { tripId: 5 },
        });
    });

    it("updates a contact and invalidates general and detail queries", async () => {
        const updatedContact = { ...contact, status: "ACCEPTED" as const };
        const patch = vi.fn().mockResolvedValue({ data: updatedContact });
        const api = { patch } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const contactData: UpdateContactPayload = {
            status: "ACCEPTED",
            checkIn: "2026-08-02",
            checkOut: "2026-08-06",
        };
        const { result } = renderHook(() => useContacts(api).useUpdateContact(), { wrapper });

        const returned = await result.current.mutateAsync({ contactId: 12, contactData });

        expect(returned).toEqual(updatedContact);
        expect(patch).toHaveBeenCalledWith("/contacts/12", contactData, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: contactKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: contactKeys.detail(12) });
    });
});
