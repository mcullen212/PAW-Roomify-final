import api from "./api";
import {VndType} from "~/lib/api/vndTypes.ts";

const sessionAPI = (() => {
  const login =  ({ email, password }: any) => {
    const basicAuth = btoa(`${email}:${password}`);

    return api.head("/", {
      headers: {
        Authorization: `Basic ${basicAuth}`,
        Accept: VndType.APPLICATION_API
      },
    });
  };

  const logout = () => {
      sessionStorage.removeItem("jwt");
      sessionStorage.removeItem("refresh");
      window.dispatchEvent(new Event("auth-change"));
  };

  const validateOTP = (otpToken: string) => {
      return api.head("/", {
          headers: {
              Authorization: `OTP ${otpToken}`,
              Accept: VndType.APPLICATION_API
          },
      });
  };

  return { login, logout, validateOTP };
})();

export default sessionAPI;
