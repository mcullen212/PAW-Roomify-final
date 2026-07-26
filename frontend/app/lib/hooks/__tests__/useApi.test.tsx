import type { ReactNode } from "react";
import type { AxiosAdapter, AxiosResponse } from "axios";
import { renderHook, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import api from "~/lib/api/api";
import { AuthContext } from "~/lib/auth/auth-context";
import type { AuthContextValue } from "~/lib/interfaces/auth";
import { useApi } from "../useApi";

const authValue: AuthContextValue = {
    authenticated: true,
    accessToken: "access-token",
    refreshToken: "refresh-token",
    userId: 1,
    roles: [],
    verified: true,
    loading: false,
    login: vi.fn(),
    validateOTP: vi.fn(),
    logout: vi.fn(),
    register: vi.fn(),
    handleTokensRefresh: vi.fn(),
    syncAuthState: vi.fn(),
};

function createAuthValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
    return {
        ...authValue,
        login: vi.fn(),
        logout: vi.fn(),
        register: vi.fn(),
        handleTokensRefresh: vi.fn(),
        syncAuthState: vi.fn(),
        ...overrides,
    };
}

function createWrapper(value: AuthContextValue = authValue) {
    return function Wrapper({ children }: { children: ReactNode }) {
        return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
    };
}

describe("useApi", () => {
    const originalAdapter = api.defaults.adapter;

    beforeEach(() => {
        sessionStorage.clear();
        vi.clearAllMocks();
    });

    afterEach(() => {
        api.defaults.adapter = originalAdapter;
        sessionStorage.clear();
    });

    it("adds bearer authorization when no explicit auth header exists", async () => {
        const adapter = vi.fn<AxiosAdapter>(async (config) => ({
            data: {},
            status: 200,
            statusText: "OK",
            headers: {},
            config,
            request: {},
        } satisfies AxiosResponse));
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), { wrapper: createWrapper() });

        await waitFor(async () => {
            await api.get("/users/1");
            expect(adapter).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    headers: expect.objectContaining({
                        Authorization: "Bearer access-token",
                    }),
                }),
            );
        });
    });

    it("does not replace an explicit Basic auth header", async () => {
        const adapter = vi.fn<AxiosAdapter>(async (config) => ({
            data: {},
            status: 200,
            statusText: "OK",
            headers: {},
            config,
            request: {},
        } satisfies AxiosResponse));
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), { wrapper: createWrapper() });

        await waitFor(async () => {
            await api.get("/", {
                headers: {
                    Authorization: "Basic abc123",
                },
            });
            expect(adapter).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    headers: expect.objectContaining({
                        Authorization: "Basic abc123",
                    }),
                }),
            );
        });
    });

    it("passes refreshed access-token headers to auth context", async () => {
        const handleTokensRefresh = vi.fn();
        const adapter = vi.fn<AxiosAdapter>(async (config) => ({
            data: {},
            status: 200,
            statusText: "OK",
            headers: {
                "access-token": "new-access-token",
                "refresh-token": "new-refresh-token",
            },
            config,
            request: {},
        } satisfies AxiosResponse));
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), {
            wrapper: createWrapper(createAuthValue({ handleTokensRefresh })),
        });

        await waitFor(async () => {
            await api.get("/users/1");
            expect(handleTokensRefresh).toHaveBeenCalledWith("new-access-token", "new-refresh-token");
        });
    });

    it("falls back to access-token headers for refreshed tokens", async () => {
        const handleTokensRefresh = vi.fn();
        const adapter = vi.fn<AxiosAdapter>(async (config) => ({
            data: {},
            status: 200,
            statusText: "OK",
            headers: {
                "access-token": "fallback-access-token",
                "refresh-token": "fallback-refresh-token",
            },
            config,
            request: {},
        } satisfies AxiosResponse));
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), {
            wrapper: createWrapper(createAuthValue({ handleTokensRefresh })),
        });

        await waitFor(async () => {
            await api.get("/users/1");
            expect(handleTokensRefresh).toHaveBeenCalledWith("fallback-access-token", "fallback-refresh-token");
        });
    });

    it("refreshes tokens and retries a 401 once with the new access token", async () => {
        const adapter = vi.fn<AxiosAdapter>(async (config) => {
            if (adapter.mock.calls.length === 1) {
                return Promise.reject({
                    response: { status: 401 },
                    config,
                });
            }

            if (config.method === "head" && config.url === "/") {
                return {
                    data: undefined,
                    status: 204,
                    statusText: "No Content",
                    headers: {
                        "access-token": "new-access-token",
                        "refresh-token": "new-refresh-token",
                    },
                    config,
                    request: {},
                } satisfies AxiosResponse;
            }

            return {
                data: { ok: true },
                status: 200,
                statusText: "OK",
                headers: {},
                config,
                request: {},
            } satisfies AxiosResponse;
        });
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), {
            wrapper: createWrapper(createAuthValue({ refreshToken: "refresh-token" })),
        });

        await expect(api.get("/protected")).resolves.toMatchObject({
            data: { ok: true },
        });
        expect(adapter).toHaveBeenCalledTimes(3);
        expect(adapter).toHaveBeenNthCalledWith(
            2,
            expect.objectContaining({
                method: "head",
                url: "/",
                headers: expect.objectContaining({
                    Authorization: "Bearer refresh-token",
                }),
            }),
        );
        expect(adapter).toHaveBeenLastCalledWith(
            expect.objectContaining({
                url: "/protected",
                headers: expect.objectContaining({
                    Authorization: "Bearer new-access-token",
                }),
            }),
        );
    });

    it("shares one token refresh across concurrent 401 responses", async () => {
        const requests: Array<{
            method?: string;
            url?: string;
            authorization?: string;
            retry?: boolean;
        }> = [];
        const adapter = vi.fn<AxiosAdapter>(async (config) => {
            requests.push({
                method: config.method,
                url: config.url,
                authorization: config.headers?.Authorization?.toString(),
                retry: Boolean((config as any)._retry),
            });

            if (config.method === "head" && config.url === "/") {
                return {
                    data: undefined,
                    status: 204,
                    statusText: "No Content",
                    headers: {
                        "access-token": "shared-access-token",
                        "refresh-token": "shared-refresh-token",
                    },
                    config,
                    request: {},
                } satisfies AxiosResponse;
            }

            const isRetried = config.headers?.Authorization === "Bearer shared-access-token";

            if (!isRetried) {
                return Promise.reject({
                    response: { status: 401 },
                    config,
                });
            }

            return {
                data: { ok: true },
                status: 200,
                statusText: "OK",
                headers: {},
                config,
                request: {},
            } satisfies AxiosResponse;
        });
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), {
            wrapper: createWrapper(createAuthValue({ refreshToken: "refresh-token" })),
        });

        await expect(Promise.all([
            api.get("/protected-a"),
            api.get("/protected-b"),
        ])).resolves.toEqual([
            expect.objectContaining({ data: { ok: true } }),
            expect.objectContaining({ data: { ok: true } }),
        ]);

        const refreshCalls = requests.filter((config) => (
            config.method === "head" && config.url === "/"
        ));
        const retriedProtectedCalls = requests.filter((config) => (
            config.url?.startsWith("/protected") &&
            config.retry &&
            config.authorization === "Bearer shared-access-token"
        ));

        expect(refreshCalls).toHaveLength(1);
        expect(retriedProtectedCalls).toHaveLength(2);
    });

    it("logs out when refresh retry fails", async () => {
        const logout = vi.fn();
        const adapter = vi.fn<AxiosAdapter>(async (config) => Promise.reject({
            response: { status: 401 },
            config,
        }));
        api.defaults.adapter = adapter;

        renderHook(() => useApi(), {
            wrapper: createWrapper(createAuthValue({ refreshToken: "refresh-token", logout })),
        });

        await expect(api.get("/protected")).rejects.toBeTruthy();
        expect(logout).toHaveBeenCalled();
    });
});
