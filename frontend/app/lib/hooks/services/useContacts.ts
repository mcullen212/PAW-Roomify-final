import type { AxiosInstance } from "axios";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createContactAPI } from "~/lib/api/contactAPI";
import type { CreateContactPayload, UpdateContactPayload } from "~/lib/interfaces/contacts";
import { getPaginationFromLinkHeader } from "~/lib/pagination";

export const contactKeys = {
    all: ["contacts"] as const,
    list: (view?: string, page?: number, pageSize?: number) =>
        ["contacts", "list", view, page, pageSize] as const,
    detail: (contactId?: number) => ["contacts", contactId] as const,
};

export function useContacts(api: AxiosInstance) {
    const contactApi = createContactAPI(api);

    function useGetContacts(view: string, page: number, pageSize = 10) {
        return useQuery({
            queryKey: contactKeys.list(view, page, pageSize),
            queryFn: async () => {
                const response = await contactApi.getContacts(view, page, pageSize);

                return {
                    data: response.data,
                    pagination: getPaginationFromLinkHeader(response.headers?.link, page),
                };
            },
        });
    }

    function useGetContactById(contactId?: number) {
        return useQuery({
            queryKey: contactKeys.detail(contactId),
            queryFn: async () => {
                const response = await contactApi.getContactById(contactId!);
                return response.data;
            },
            enabled: !!contactId,
        });
    }

    function useCreateContact() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: ({
                contactData,
                tripId,
            }: {
                contactData: CreateContactPayload;
                tripId?: number;
            }) => contactApi.createContact(contactData, tripId),
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: contactKeys.all });
            },
        });
    }

    function useUpdateContact() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: async ({
                contactId,
                contactData,
            }: {
                contactId: number;
                contactData: UpdateContactPayload;
            }) => {
                const response = await contactApi.updateContact(contactId, contactData);
                return response.data;
            },
            onSuccess: (_, variables) => {
                queryClient.invalidateQueries({ queryKey: contactKeys.all });
                queryClient.invalidateQueries({ queryKey: contactKeys.detail(variables.contactId) });
            },
        });
    }

    return {
        useGetContacts,
        useGetContactById,
        useCreateContact,
        useUpdateContact,
    };
}
