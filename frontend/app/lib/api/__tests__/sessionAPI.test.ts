import { beforeEach, describe, expect, it, vi } from "vitest";
import { VndType } from "../vndTypes";

vi.mock("../api", () => ({
    default: {
        head: vi.fn(),
    },
}));

import api from "../api";
import sessionAPI from "../sessionAPI";

describe("Session API", () => {
    beforeEach(() => {
        vi.clearAllMocks();
        sessionStorage.clear();
    });

    it("should login with basic auth headers", () => {
        const email = "user@example.com";
        const password = "password";

        sessionAPI.login({ email, password });

        expect(api.head).toHaveBeenCalledWith("/", {
            headers: {
                Authorization: `Basic ${btoa(`${email}:${password}`)}`,
                Accept: VndType.APPLICATION_API,
            },
        });
    });

    it("should validate otp with otp auth headers", () => {
        const otp = "ABC123";

        sessionAPI.validateOTP(otp);

        expect(api.head).toHaveBeenCalledWith("/", {
            headers: {
                Authorization: `OTP ${otp}`,
                Accept: VndType.APPLICATION_API,
            },
        });
    });

    it("should logout clearing stored tokens and dispatching auth change", () => {
        const authChangeListener = vi.fn();
        sessionStorage.setItem("jwt", "access-token");
        sessionStorage.setItem("refresh", "refresh-token");
        window.addEventListener("auth-change", authChangeListener);

        sessionAPI.logout();

        expect(sessionStorage.getItem("jwt")).toBeNull();
        expect(sessionStorage.getItem("refresh")).toBeNull();
        expect(authChangeListener).toHaveBeenCalledTimes(1);

        window.removeEventListener("auth-change", authChangeListener);
    });
});
