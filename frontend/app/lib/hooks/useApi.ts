import { useEffect } from "react";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import api from "~/lib/api/api";
import { HttpStatus } from "~/lib/api/httpStatus";
import { useAuth } from "~/lib/auth/useAuth";
import type { AuthContextValue } from "~/lib/interfaces/auth";
import { VndType } from "~/lib/api/vndTypes";

type RetriableRequestConfig = InternalAxiosRequestConfig & {
    _retry?: boolean;
    _skipAuthRefresh?: boolean;
};

let mountedApiHooks = 0;
let requestInterceptorId: number | undefined;
let responseInterceptorId: number | undefined;
let latestAuth: AuthContextValue | undefined;
let refreshPromise: Promise<string> | undefined;

function getAuthorizationHeader(config?: InternalAxiosRequestConfig) {
    const headers = config?.headers;
    const authHeader = headers?.Authorization ?? headers?.authorization;

    return typeof authHeader === "string" ? authHeader : undefined;
}

function isLoginAttempt(config?: InternalAxiosRequestConfig) {
    const authHeader = getAuthorizationHeader(config);

    return authHeader?.startsWith("Basic ");
}

function shouldSkipAuthRefresh(config?: RetriableRequestConfig) {
    return Boolean(config?._skipAuthRefresh);
}

function getHeaderValue(value: unknown) {
    if (Array.isArray(value)) {
        return value[0]?.toString();
    }

    return value?.toString();
}

async function refreshAccessToken(refreshToken: string) {
    const response = await api.head("/", {
        _skipAuthRefresh: true,
        headers: {
            Authorization: `Bearer ${refreshToken}`,
            Accept: VndType.APPLICATION_API,
        },
    } as RetriableRequestConfig);

    const accessToken = getHeaderValue(response.headers["access-token"]);

    if (!accessToken) {
        throw new Error("Missing access token in refresh response");
    }

    return accessToken;
}

export function useApi() {
    const auth = useAuth();

    useEffect(() => {
        latestAuth = auth;
    }, [auth]);

    useEffect(() => {
        mountedApiHooks += 1;

        if (requestInterceptorId === undefined) {
            requestInterceptorId = api.interceptors.request.use((config) => {
                const accessToken = sessionStorage.getItem("jwt") ?? latestAuth?.accessToken;

                if (accessToken && !getAuthorizationHeader(config)) {
                    config.headers.Authorization = `Bearer ${accessToken}`;
                }

                return config;
            });
        }

        if (responseInterceptorId === undefined) {
            responseInterceptorId = api.interceptors.response.use(
                (response) => {
                    const accessToken = getHeaderValue(response.headers["access-token"]);
                    const refreshToken = getHeaderValue(response.headers["refresh-token"]);

                    if (accessToken) {
                        latestAuth?.handleTokensRefresh(accessToken, refreshToken);
                    }

                    return response;
                },
                async (error: AxiosError) => {
                    const originalRequest = error.config as RetriableRequestConfig | undefined;
                    const refreshToken = sessionStorage.getItem("refresh") ?? latestAuth?.refreshToken;

                    if (
                        error.response?.status === HttpStatus.UNAUTHORIZED &&
                        originalRequest &&
                        !originalRequest._retry &&
                        !shouldSkipAuthRefresh(originalRequest) &&
                        !isLoginAttempt(originalRequest) &&
                        refreshToken
                    ) {
                        originalRequest._retry = true;

                        try {
                            refreshPromise = refreshPromise ?? refreshAccessToken(refreshToken);
                            const accessToken = await refreshPromise;

                            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
                            return await api.request(originalRequest);
                        } catch (refreshError) {
                            latestAuth?.logout();
                            return Promise.reject(refreshError);
                        } finally {
                            refreshPromise = undefined;
                        }
                    }

                    if (
                        error.response?.status === HttpStatus.UNAUTHORIZED &&
                        !shouldSkipAuthRefresh(originalRequest) &&
                        !isLoginAttempt(originalRequest) &&
                        !originalRequest?._retry &&
                        typeof window !== "undefined" &&
                        !window.location.pathname.endsWith("/login")
                    ) {
                        latestAuth?.logout();
                    }

                    return Promise.reject(error);
                },
            );
        }

        return () => {
            mountedApiHooks -= 1;

            if (mountedApiHooks === 0) {
                if (requestInterceptorId !== undefined) {
                    api.interceptors.request.eject(requestInterceptorId);
                    requestInterceptorId = undefined;
                }

                if (responseInterceptorId !== undefined) {
                    api.interceptors.response.eject(responseInterceptorId);
                    responseInterceptorId = undefined;
                }

                latestAuth = undefined;
            }
        };
    }, []);

    return api;
}
