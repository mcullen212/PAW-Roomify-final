export interface RegisterData {
    name: string;
    email: string;
    password: string;
    locale?: string;
}

export interface AuthContextValue {
    authenticated: boolean;
    accessToken?: string;
    refreshToken?: string;
    email?: string;
    userId?: number;
    roles: string[];
    verified: boolean;
    loading: boolean;
    login: (email: string, password: string) => Promise<boolean>;
    validateOTP: (email: string, otpToken: string) => Promise<void>;
    logout: () => void;
    register: (data: RegisterData) => Promise<boolean>;
    handleTokensRefresh: (accessToken: string, refreshToken?: string) => void;
    syncAuthState: () => Promise<void>;
}

export type VerificationPopupCopyKey = "popUps.rooms" | "popUps.swaps" | "popUps.trips";
