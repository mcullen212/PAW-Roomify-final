import type { AxiosInstance } from "axios";
import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { createUserAPI } from "~/lib/api/userAPI";

export const userKeys = {
    all: ["users"] as const,
    detail: (userId?: number) => ["users", userId] as const,
    publicDetail: (userId?: number) => ["users", "public", userId] as const,
    public: (url?: string) => ["users", "public", url] as const,
};

export function useUsers(api: AxiosInstance) {
    const userApi = createUserAPI(api);

    function useGetProfile(userId?: number) {
        return useQuery({
            queryKey: userKeys.detail(userId),
            queryFn: async () => {
                const response = await userApi.getUserProfile(userId!);
                return response.data;
            },
            enabled: !!userId,
        });
    }

    function useGetPublicUserByUrl(url?: string) {
        return useQuery({
            queryKey: userKeys.public(url),
            queryFn: async () => {
                const response = await userApi.getPublicUserByUrl(url!);
                return response.data;
            },
            enabled: !!url,
        });
    }

    function useGetPublicUser(userId?: number) {
        return useQuery({
            queryKey: userKeys.publicDetail(userId),
            queryFn: async () => {
                const response = await userApi.getPublicUser(userId!);
                return response.data;
            },
            enabled: !!userId,
        });
    }

    function useGetPublicUsersByUrls(urls: (string | undefined)[]) {
        return useQueries({
            queries: urls.map((url) => ({
                queryKey: userKeys.public(url),
                queryFn: async () => {
                    const response = await userApi.getPublicUserByUrl(url!);
                    return response.data;
                },
                enabled: !!url,
            })),
        });
    }

    function useCreateUser() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: async (userData: unknown) => {
                const response = await userApi.createUser(userData);
                return response.data;
            },
            onSuccess: () => {
                queryClient.invalidateQueries({ queryKey: userKeys.all });
            },
        });
    }

    function useUpdateProfile() {
        const queryClient = useQueryClient();

        return useMutation({
            mutationFn: async ({
                userId,
                profileData,
            }: {
                userId: number;
                profileData: unknown;
            }) => {
                const response = await userApi.updateUserProfile(userId, profileData);
                return response.data;
            },
            onSuccess: (_, variables) => {
                queryClient.invalidateQueries({ queryKey: userKeys.all });
                queryClient.invalidateQueries({ queryKey: userKeys.detail(variables.userId) });
            },
        });
    }

    function useUpdatePassword() {
        return useMutation({
            mutationFn: ({
                userId,
                passwordData,
            }: {
                userId: number;
                passwordData: { oldPassword: string; newPassword: string };
            }) => userApi.updateUserPassword(userId, passwordData),
        });
    }

    function useRequestPasswordResetOtp() {
        return useMutation({
            mutationFn: (email: string) => userApi.requestPasswordResetOtp(email),
        });
    }

    function useResetPassword() {
        return useMutation({
            mutationFn: (newPassword: string) => userApi.resetPassword(newPassword),
        });
    }

    return {
        useGetProfile,
        useGetPublicUser,
        useGetPublicUserByUrl,
        useGetPublicUsersByUrls,
        useCreateUser,
        useUpdateProfile,
        useUpdatePassword,
        useRequestPasswordResetOtp,
        useResetPassword,
    };
}
