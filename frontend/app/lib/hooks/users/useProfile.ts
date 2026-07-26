import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { useAuth } from "~/lib/auth/useAuth.ts";
import { HttpStatus } from "~/lib/api/httpStatus";
import { getApiErrorStatus } from "~/lib/api/api-error-status";
import { syncPrimaryLanguage } from "~/lib/utils.ts";
import i18n from "~/i18n/i18n.ts";
import { useApiServices } from "~/lib/hooks/useApiServices";
import { toast } from "sonner";

type SavingField = "bio" | "travelPreferences" | "locale" | "password" | null;

type ProfileUpdateData = {
    bio?: string;
    travelPreferences?: string;
    locale?: string;
};

export function useProfile() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { userId, logout, verified } = useAuth();
    const { userService } = useApiServices();
    const profileQuery = userService.useGetProfile(userId);
    const updateProfileMutation = userService.useUpdateProfile();
    const updatePasswordMutation = userService.useUpdatePassword();

    const [user, setUser] = useState<any>(null);
    const [isLoading, setIsLoading] = useState<boolean>(true);
    const [savingField, setSavingField] = useState<SavingField>(null);
    const [error, setError] = useState<string>("");
    const [saveError, setSaveError] = useState<string>("");
    const [saveSuccess, setSaveSuccess] = useState<string>("");

    const notifyProfileUpdated = () => {
        const message = t("profile.success.profile");
        setSaveSuccess(message);
        toast.success(message);
    };

    useEffect(() => {
        if (!userId) {
            navigate("/login");
        }
    }, [navigate, userId]);

    useEffect(() => {
        if (profileQuery.data?.locale) {
            syncPrimaryLanguage(profileQuery.data.locale);
        }
    }, [profileQuery.data?.locale]);

    useEffect(() => {
        const status = getApiErrorStatus(profileQuery.error);

        if (status === HttpStatus.UNAUTHORIZED) {
            logout();
            navigate("/login");
            return;
        }

        if (profileQuery.isError) {
            console.error("Error al cargar el perfil:", profileQuery.error);
            setError(i18n.t("profile.errors.load"));
        }
    }, [logout, navigate, profileQuery.error, profileQuery.isError]);

    const handleProfileUpdate = async (profileData: ProfileUpdateData) => {
        if (!userId) {
            navigate("/login");
            return;
        }

        await updateProfileMutation.mutateAsync({
            userId,
            profileData,
        });
    };

    const handleBiographySave = async (biography: string) => {
        try {
            setSaveError("");
            setSaveSuccess("");
            setSavingField("bio");
            await handleProfileUpdate({ bio: biography });
            notifyProfileUpdated();
        } catch (err) {
            console.error("Error al actualizar la biografía:", err);
            setSaveError(t("profile.errors.biography"));
        } finally {
            setSavingField(null);
        }
    };

    const handleTravelPreferencesSave = async (travelPreferences: string) => {
        try {
            setSaveError("");
            setSaveSuccess("");
            setSavingField("travelPreferences");
            await handleProfileUpdate({ travelPreferences });
            notifyProfileUpdated();
        } catch (err) {
            console.error("Error al actualizar las preferencias de viaje:", err);
            setSaveError(t("profile.errors.travelPreferences"));
        } finally {
            setSavingField(null);
        }
    };

    const handleLocaleSave = async (locale: string) => {
        try {
            setSaveError("");
            setSaveSuccess("");
            setSavingField("locale");
            await handleProfileUpdate({ locale });
            await syncPrimaryLanguage(locale);
            setUser((currentUser: any) => ({
                ...currentUser,
                locale,
            }));
            notifyProfileUpdated();
        } catch (err) {
            console.error("Error al actualizar el idioma:", err);
            setSaveError(t("profile.errors.locale"));
        } finally {
            setSavingField(null);
        }
    };

    const handlePasswordSave = async (oldPassword: string, newPassword: string) => {
        try {
            setSaveError("");
            setSaveSuccess("");
            setSavingField("password");
            if (!userId) {
                navigate("/login");
                return;
            }

            await updatePasswordMutation.mutateAsync({
                userId,
                passwordData: {
                    oldPassword,
                    newPassword,
                },
            });
            const message = t("profile.success.password");
            setSaveSuccess(message);
            toast.success(message);
        } catch (err) {
            console.error("Error al actualizar la contraseña:", err);
            setSaveError(t("profile.errors.password"));
            throw err;
        } finally {
            setSavingField(null);
        }
    };

    return {
        user: profileQuery.data,
        isLoading: profileQuery.isLoading,
        isError: profileQuery.isError,
        loadError: profileQuery.error,
        savingField,
        error,
        saveError,
        saveSuccess,
        verified,
        handleBiographySave,
        handleTravelPreferencesSave,
        handleLocaleSave,
        handlePasswordSave,
    };
}
