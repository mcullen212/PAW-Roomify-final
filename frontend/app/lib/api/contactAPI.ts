import type { AxiosInstance } from "axios";
import api from "./api";
import { VndType } from "./vndTypes";
import type { Contact, CreateContactPayload, UpdateContactPayload } from "~/lib/interfaces/contacts";

export type {
    Contact,
    ContactDateRange,
    ContactStatus,
    CreateContactPayload,
    UpdateContactPayload,
} from "~/lib/interfaces/contacts";

export const createContactAPI = (client: AxiosInstance) => {
    const getContacts = (view: string, page: number, pageSize = 10) => {
        return client.get<Contact[]>("/contacts", {
            headers: {
                Accept: VndType.APPLICATION_CONTACT,
            },
            params: {
                view,
                page,
                pageSize,
            },
        });
    };

    const getContactById = (id: number) => {
        return client.get<Contact>(`/contacts/${id}`, {
            headers: {
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        });
    };

    const createContact = (payload: CreateContactPayload, tripId?: number) => {
        return client.post<void>("/contacts", payload, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
            params: tripId !== undefined ? { tripId } : undefined,
        });
    };

    const updateContact = (contactId: number, payload: UpdateContactPayload) => {
        return client.patch<Contact>(`/contacts/${contactId}`, payload, {
            headers: {
                "Content-Type": VndType.APPLICATION_CONTACT_DETAIL,
                Accept: VndType.APPLICATION_CONTACT_DETAIL,
            },
        });
    };

    return {
        getContacts,
        getContactById,
        createContact,
        updateContact,
    };
};

const contactApi = createContactAPI(api);

export default contactApi;
