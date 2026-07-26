import type { ReactNode } from "react";
import type { AxiosInstance } from "axios";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, renderHook, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VndType } from "~/lib/api/vndTypes.ts";
import { userKeys, useUsers } from "../services/useUsers.ts";

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

describe("useUsers", () => {
    it("gets a user profile with the profile media type", async () => {
        const get = vi.fn().mockResolvedValue({ data: { id: 7, name: "Ada" } });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useUsers(api).useGetProfile(7), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({ id: 7, name: "Ada" });
        expect(get).toHaveBeenCalledWith("/users/7", {
            headers: {
                "Content-Type": VndType.APPLICATION_USER,
                Accept: VndType.APPLICATION_USER_PROFILE,
            },
        });
    });

    it("does not request a profile without a user id", () => {
        const get = vi.fn();
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useUsers(api).useGetProfile(), { wrapper });

        expect(result.current.fetchStatus).toBe("idle");
        expect(get).not.toHaveBeenCalled();
    });

    it("gets a public user with the public media type", async () => {
        const get = vi.fn().mockResolvedValue({ data: { id: 7, name: "Ada" } });
        const api = { get } as unknown as AxiosInstance;
        const { wrapper } = setup();

        const { result } = renderHook(() => useUsers(api).useGetPublicUser(7), { wrapper });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));
        expect(result.current.data).toEqual({ id: 7, name: "Ada" });
        expect(get).toHaveBeenCalledWith("/users/7", {
            headers: {
                Accept: VndType.APPLICATION_USER,
            },
        });
    });

    it("creates a user and invalidates user queries", async () => {
        const post = vi.fn().mockResolvedValue({ data: { id: 8 } });
        const api = { post } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const userData = { name: "Ada", email: "ada@example.com", password: "secret" };
        const { result } = renderHook(() => useUsers(api).useCreateUser(), { wrapper });

        await act(async () => {
            await result.current.mutateAsync(userData);
        });

        expect(post).toHaveBeenCalledWith("/users", userData, {
            headers: {
                "Content-Type": VndType.APPLICATION_USER,
                Accept: VndType.APPLICATION_USER,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: userKeys.all });
    });

    it("updates a profile and invalidates general and detail queries", async () => {
        const patch = vi.fn().mockResolvedValue({ data: { name: "Grace" } });
        const api = { patch } as unknown as AxiosInstance;
        const { queryClient, wrapper } = setup();
        const invalidateQueries = vi.spyOn(queryClient, "invalidateQueries");
        const profileData = { name: "Grace" };
        const { result } = renderHook(() => useUsers(api).useUpdateProfile(), { wrapper });

        await act(async () => {
            await result.current.mutateAsync({ userId: 7, profileData });
        });

        expect(patch).toHaveBeenCalledWith("/users/7", profileData, {
            headers: {
                "Content-Type": VndType.APPLICATION_USER,
                Accept: VndType.APPLICATION_USER_PROFILE,
            },
        });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: userKeys.all });
        expect(invalidateQueries).toHaveBeenCalledWith({ queryKey: userKeys.detail(7) });
    });
});
