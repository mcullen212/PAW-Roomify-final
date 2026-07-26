import { act, render, waitFor } from "@testing-library/react";
import { useContext } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { jwtDecode } from "jwt-decode";
import { MemoryRouter, useLocation } from "react-router";
import { AuthProvider } from "../AuthProvider";
import { AuthContext } from "../auth-context";
import type { AuthContextValue } from "~/lib/interfaces/auth";
import api from "~/lib/api/api";
import userApi from "~/lib/api/userAPI";
import { queryClient } from "~/lib/query";
import { VndType } from "~/lib/api/vndTypes";

vi.mock("jwt-decode", () => ({
    jwtDecode: vi.fn(),
}));

vi.mock("~/lib/api/api", () => ({
    default: {
        head: vi.fn(),
    },
}));

vi.mock("~/lib/api/userAPI", () => ({
    default: {
        createUser: vi.fn(),
    },
}));

function renderWithAuthProvider() {
    let contextValue: AuthContextValue | undefined;
    let pathname = "";

    const TestComponent = () => {
        contextValue = useContext(AuthContext);
        pathname = useLocation().pathname;
        return null;
    };

    render(
        <MemoryRouter initialEntries={["/profile"]}>
            <AuthProvider>
                <TestComponent />
            </AuthProvider>
        </MemoryRouter>
    );

    return {
        getContext: () => contextValue,
        getPathname: () => pathname,
    };
}

describe("AuthProvider", () => {
    beforeEach(() => {
        sessionStorage.clear();
        queryClient.clear();
        vi.clearAllMocks();
        vi.mocked(jwtDecode).mockReturnValue({});
    });

    it("should initialize with empty state when no tokens exist", async () => {
        const { getContext } = renderWithAuthProvider();

        await waitFor(() => expect(getContext()!.loading).toBe(false));

        expect(getContext()).toBeDefined();
        expect(getContext()!.authenticated).toBe(false);
        expect(getContext()!.accessToken).toBeUndefined();
        expect(getContext()!.refreshToken).toBeUndefined();
        expect(getContext()!.userId).toBeUndefined();
        expect(getContext()!.roles).toEqual([]);
        expect(getContext()!.verified).toBe(false);
    });

    it("should initialize with stored tokens and decode them", async () => {
        sessionStorage.setItem("jwt", "stored-access-token");
        sessionStorage.setItem("refresh", "stored-refresh-token");
        vi.mocked(jwtDecode).mockReturnValue({
            sub: "stored@example.com",
            userId: 7,
            roles: ["ROLE_USER", "ROLE_VERIFIED_USER"],
        });

        const { getContext } = renderWithAuthProvider();

        await waitFor(() => expect(getContext()!.loading).toBe(false));

        expect(getContext()!.authenticated).toBe(true);
        expect(getContext()!.accessToken).toBe("stored-access-token");
        expect(getContext()!.refreshToken).toBe("stored-refresh-token");
        expect(getContext()!.email).toBe("stored@example.com");
        expect(getContext()!.userId).toBe(7);
        expect(getContext()!.roles).toEqual(["ROLE_USER", "ROLE_VERIFIED_USER"]);
        expect(getContext()!.verified).toBe(true);
    });

    it("should refresh an unverified stored session so verification changes appear after reload", async () => {
        sessionStorage.setItem("jwt", "unverified-access-token");
        sessionStorage.setItem("refresh", "stored-refresh-token");
        vi.mocked(jwtDecode)
            .mockReturnValueOnce({
                sub: "stored@example.com",
                userId: 7,
                roles: ["ROLE_USER"],
            })
            .mockReturnValue({
                sub: "stored@example.com",
                userId: 7,
                roles: ["ROLE_USER", "ROLE_VERIFIED_USER"],
            });
        vi.mocked(api.head).mockResolvedValue({
            data: undefined,
            status: 204,
            statusText: "No Content",
            headers: {
                "access-token": "verified-access-token",
                "refresh-token": "verified-refresh-token",
            },
            config: {},
        } as any);

        const { getContext } = renderWithAuthProvider();

        await waitFor(() => expect(getContext()!.loading).toBe(false));
        await waitFor(() => expect(getContext()!.verified).toBe(true));

        expect(api.head).toHaveBeenCalledWith("/", {
            headers: {
                Authorization: "Bearer stored-refresh-token",
                Accept: VndType.APPLICATION_API,
            },
        });
        expect(getContext()!.accessToken).toBe("verified-access-token");
        expect(getContext()!.refreshToken).toBe("verified-refresh-token");
        expect(sessionStorage.getItem("jwt")).toBe("verified-access-token");
        expect(sessionStorage.getItem("refresh")).toBe("verified-refresh-token");
    });

    it("should keep an expired stored access token when a refresh token exists", async () => {
        sessionStorage.setItem("jwt", "expired-access-token");
        sessionStorage.setItem("refresh", "stored-refresh-token");
        vi.mocked(jwtDecode).mockReturnValue({
            sub: "stored@example.com",
            userId: 7,
            roles: ["ROLE_USER", "ROLE_VERIFIED_USER"],
            exp: Math.floor(Date.now() / 1000) - 60,
        });

        const { getContext } = renderWithAuthProvider();

        await waitFor(() => expect(getContext()!.loading).toBe(false));

        expect(getContext()!.authenticated).toBe(true);
        expect(getContext()!.accessToken).toBe("expired-access-token");
        expect(getContext()!.refreshToken).toBe("stored-refresh-token");
        expect(sessionStorage.getItem("jwt")).toBe("expired-access-token");
        expect(sessionStorage.getItem("refresh")).toBe("stored-refresh-token");
    });

    it("should remove an expired stored access token when no refresh token exists", async () => {
        sessionStorage.setItem("jwt", "expired-access-token");
        vi.mocked(jwtDecode).mockReturnValue({
            exp: Math.floor(Date.now() / 1000) - 60,
        });

        const { getContext } = renderWithAuthProvider();

        await waitFor(() => expect(getContext()!.loading).toBe(false));

        expect(getContext()!.authenticated).toBe(false);
        expect(getContext()!.accessToken).toBeUndefined();
        expect(getContext()!.refreshToken).toBeUndefined();
        expect(sessionStorage.getItem("jwt")).toBeNull();
        expect(sessionStorage.getItem("refresh")).toBeNull();
    });

    it("should remove tokens when stored jwt cannot be decoded", async () => {
        sessionStorage.setItem("jwt", "invalid-token");
        sessionStorage.setItem("refresh", "stored-refresh-token");
        vi.mocked(jwtDecode).mockImplementation(() => {
            throw new Error("Invalid token");
        });

        const { getContext } = renderWithAuthProvider();

        await waitFor(() => expect(getContext()!.loading).toBe(false));

        expect(getContext()!.authenticated).toBe(false);
        expect(getContext()!.accessToken).toBeUndefined();
        expect(getContext()!.refreshToken).toBeUndefined();
        expect(sessionStorage.getItem("jwt")).toBeNull();
        expect(sessionStorage.getItem("refresh")).toBeNull();
    });

    it("should handle successful login", async () => {
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 8,
            roles: ["ROLE_USER"],
        });
        vi.mocked(api.head).mockResolvedValue({
            data: undefined,
            status: 200,
            statusText: "OK",
            headers: {
                "access-token": "new-access-token",
                "refresh-token": "new-refresh-token",
            },
            config: {},
        } as any);

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));

        let verificationEmailResent = false;
        await act(async () => {
            verificationEmailResent = await getContext()!.login("user@example.com", "password");
        });

        expect(api.head).toHaveBeenCalledWith("/", {
            headers: {
                Authorization: `Basic ${btoa("user@example.com:password")}`,
                Accept: VndType.APPLICATION_API,
            },
        });
        expect(getContext()!.authenticated).toBe(true);
        expect(getContext()!.accessToken).toBe("new-access-token");
        expect(getContext()!.refreshToken).toBe("new-refresh-token");
        expect(getContext()!.userId).toBe(8);
        expect(verificationEmailResent).toBe(false);
        expect(getContext()!.roles).toEqual(["ROLE_USER"]);
        expect(getContext()!.verified).toBe(false);
        expect(sessionStorage.getItem("jwt")).toBe("new-access-token");
        expect(sessionStorage.getItem("refresh")).toBe("new-refresh-token");
    });

    it("should support access-token as login access token fallback", async () => {
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 10,
            roles: ["ROLE_USER"],
        });
        vi.mocked(api.head).mockResolvedValue({
            data: undefined,
            status: 200,
            statusText: "OK",
            headers: {
                "access-token": "fallback-access-token",
                "refresh-token": "fallback-refresh-token",
            },
            config: {},
        } as any);

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));

        await act(async () => {
            await getContext()!.login("user@example.com", "password");
        });

        expect(getContext()!.accessToken).toBe("fallback-access-token");
        expect(getContext()!.refreshToken).toBe("fallback-refresh-token");
    });

    it("should validate verification OTP and update auth tokens", async () => {
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 12,
            roles: ["ROLE_USER", "ROLE_VERIFIED_USER"],
        });
        vi.mocked(api.head).mockResolvedValue({
            data: undefined,
            status: 200,
            statusText: "OK",
            headers: {
                "access-token": "otp-access-token",
                "refresh-token": "otp-refresh-token",
            },
            config: {},
        } as any);

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));

        await act(async () => {
            await getContext()!.validateOTP("user@example.com", "ABC123");
        });

        expect(api.head).toHaveBeenCalledWith("/", {
            headers: {
                Authorization: `Basic ${btoa("user@example.com:ABC123")}`,
                Accept: VndType.APPLICATION_API,
            },
        });
        expect(getContext()!.authenticated).toBe(true);
        expect(getContext()!.accessToken).toBe("otp-access-token");
        expect(getContext()!.refreshToken).toBe("otp-refresh-token");
        expect(getContext()!.verified).toBe(true);
        expect(sessionStorage.getItem("jwt")).toBe("otp-access-token");
        expect(sessionStorage.getItem("refresh")).toBe("otp-refresh-token");
    });

    it("should register, log in, and return true", async () => {
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 11,
            roles: ["ROLE_USER"],
        });
        vi.mocked(userApi.createUser).mockResolvedValue({} as any);
        vi.mocked(api.head).mockResolvedValue({
            data: undefined,
            status: 200,
            statusText: "OK",
            headers: {
                "access-token": "registered-access-token",
                "refresh-token": "registered-refresh-token",
            },
            config: {},
        } as any);

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));

        let result = false;
        await act(async () => {
            result = await getContext()!.register({
                name: "User",
                email: "user@example.com",
                password: "password",
            });
        });

        expect(result).toBe(true);
        expect(userApi.createUser).toHaveBeenCalledWith({
            name: "User",
            email: "user@example.com",
            password: "password",
        });
        expect(api.head).toHaveBeenCalledWith("/", {
            headers: {
                Authorization: `Basic ${btoa("user@example.com:password")}`,
                Accept: VndType.APPLICATION_API,
            },
        });
        expect(getContext()!.authenticated).toBe(true);
        expect(getContext()!.accessToken).toBe("registered-access-token");
    });

    it("should propagate registration errors so the form can handle them", async () => {
        const conflictError = new Error("Conflict");
        vi.mocked(userApi.createUser).mockRejectedValue(conflictError);

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));

        await expect(act(async () => {
            await getContext()!.register({
                name: "User",
                email: "user@example.com",
                password: "password",
            });
        })).rejects.toBe(conflictError);

        expect(api.head).not.toHaveBeenCalled();
        expect(getContext()!.authenticated).toBe(false);
    });

    it("should handle logout correctly", async () => {
        sessionStorage.setItem("jwt", "stored-access-token");
        sessionStorage.setItem("refresh", "stored-refresh-token");
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 7,
            roles: ["ROLE_USER"],
        });

        const clearSpy = vi.spyOn(queryClient, "clear");
        const { getContext, getPathname } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.authenticated).toBe(true));

        act(() => {
            getContext()!.logout();
        });

        expect(clearSpy).toHaveBeenCalled();
        expect(sessionStorage.getItem("jwt")).toBeNull();
        expect(sessionStorage.getItem("refresh")).toBeNull();
        expect(getContext()!.authenticated).toBe(false);
        expect(getContext()!.accessToken).toBeUndefined();
        expect(getContext()!.userId).toBeUndefined();
        expect(getContext()!.roles).toEqual([]);
        expect(getContext()!.verified).toBe(false);
        expect(getPathname()).toBe("/login");
    });

    it("should handle token refresh correctly", async () => {
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 9,
            roles: ["ROLE_USER", "ROLE_VERIFIED_USER"],
        });

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));

        act(() => {
            getContext()!.handleTokensRefresh("new-access-token", "new-refresh-token");
        });

        expect(getContext()!.authenticated).toBe(true);
        expect(getContext()!.accessToken).toBe("new-access-token");
        expect(getContext()!.refreshToken).toBe("new-refresh-token");
        expect(getContext()!.userId).toBe(9);
        expect(getContext()!.verified).toBe(true);
        expect(sessionStorage.getItem("jwt")).toBe("new-access-token");
        expect(sessionStorage.getItem("refresh")).toBe("new-refresh-token");
    });

    it("should ignore unchanged refreshed tokens", async () => {
        sessionStorage.setItem("jwt", "same-access-token");
        sessionStorage.setItem("refresh", "same-refresh-token");
        vi.mocked(jwtDecode).mockReturnValue({
            userId: 9,
            roles: ["ROLE_USER", "ROLE_VERIFIED_USER"],
        });

        const { getContext } = renderWithAuthProvider();
        await waitFor(() => expect(getContext()!.loading).toBe(false));
        vi.mocked(jwtDecode).mockClear();

        act(() => {
            getContext()!.handleTokensRefresh("same-access-token", "same-refresh-token");
        });

        expect(jwtDecode).not.toHaveBeenCalled();
        expect(getContext()!.accessToken).toBe("same-access-token");
    });

});
