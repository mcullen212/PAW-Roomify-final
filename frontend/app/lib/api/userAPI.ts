import type { AxiosInstance } from "axios";
import api from "./api";
import {getUserIdFromToken} from "~/lib/utils.ts";
import {VndType} from "~/lib/api/vndTypes.ts";

export const createUserAPI = (client: AxiosInstance) => {
  const createUser = (userData: any) => {
    return client.post("/users", userData, {
      headers: {
        "Content-Type": VndType.APPLICATION_USER,
        Accept: VndType.APPLICATION_USER,
      },
    });
  };

  const getUserProfile = (id: number) => {
    return client.get(`/users/${id}`, {
      headers: {
        "Content-Type": VndType.APPLICATION_USER,
        Accept: VndType.APPLICATION_USER_PROFILE,
      },
    });
  };

  const getPublicUserByUrl = (url: string) => {
    return client.get(url, {
      headers: {
        Accept: VndType.APPLICATION_USER,
      },
    });
  };

  const getPublicUser = (id: number) => {
    return client.get(`/users/${id}`, {
      headers: {
        Accept: VndType.APPLICATION_USER,
      },
    });
  };

  const updateUserProfile = (id: number, profileData: any) => {
    return client.patch(`/users/${id}`, profileData, {
      headers: {
        "Content-Type": VndType.APPLICATION_USER,
        Accept: VndType.APPLICATION_USER_PROFILE,
      },
    });
  };

  const updateUserPassword = (
    id: number,
    passwordData: { oldPassword: string; newPassword: string },
  ) => {
    return client.patch(`/users/${id}`, passwordData, {
      headers: {
        "Content-Type": VndType.APPLICATION_USER,
        Accept: VndType.APPLICATION_USER_PROFILE,
      },
    });
  };

  const requestPasswordResetOtp = (email: string) => {
    return client.post(
      "/users",
      { email },
      {
        headers: {
          "Content-Type": VndType.APPLICATION_USER_PASSWORD_RESET,
        },
      },
    );
  };

  const resetPassword = (
    newPassword: string,
  ) => {
    const id = getUserIdFromToken()!;
    return client.patch(
      `/users/${id}`,
      { newPassword },
      {
        headers: {
          "Content-Type": VndType.APPLICATION_USER_PASSWORD_RESET,
        },
      },
    );
  };

  return {
    createUser,
    getUserProfile,
    getPublicUser,
    getPublicUserByUrl,
    updateUserProfile,
    updateUserPassword,
    requestPasswordResetOtp,
    resetPassword,
  };
};

const userApi = createUserAPI(api);

export default userApi;
