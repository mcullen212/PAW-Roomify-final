import { Navbar } from "@/components/Navbar.tsx"
import { ProfileHeader } from "@/components/profile/profile-header.tsx"
import { SwapStats } from "@/components/profile/swap-stats.tsx"
import { BiographyCard } from "@/components/profile/biography-card.tsx"
import { LanguagesCard } from "@/components/profile/languages-card.tsx"
import { TravelPreferencesCard } from "@/components/profile/travel-preferences-card.tsx"
import { AccountCard } from "@/components/profile/account-card.tsx"
import { Card } from "@/components/ui/card"
import { pageTitleKey } from "@/lib/utils";
import { ArrowRight, MessageSquare } from "lucide-react";
import { Link } from "react-router";
import { useTranslation } from "react-i18next";
import i18n from "@/i18n/i18n";
import { useProfile } from "~/lib/hooks/users/useProfile";
import NotFound from "~/routes/errors/not-found-page";
import { getApiErrorPage } from "~/routes/errors/api-error-page";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.profile") },
        { name: "description", content: i18n.t("pageDescriptions.profile") },
    ]
}

export default function ProfilePage() {
    const { t } = useTranslation()
    const {
        user,
        isLoading,
        isError,
        loadError,
        savingField,
        error,
        saveError,
        saveSuccess,
        verified,
        handleBiographySave,
        handleTravelPreferencesSave,
        handleLocaleSave,
        handlePasswordSave,
    } = useProfile()

    if (isLoading) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex justify-center items-center h-64 text-primary">{t("profile.loading")}</div>
            </div>
        )
    }

    const apiErrorPage = getApiErrorPage(loadError, {
        notFoundTitleKey: "profile.userNotFound",
        notFoundDescriptionKey: "error404.userDescription",
    })
    if (apiErrorPage) return apiErrorPage
    if (isError || error) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex h-64 items-center justify-center px-4 text-center text-red-500">
                    {error || t("profile.errors.load")}
                </div>
            </div>
        )
    }
    if (!user) return <NotFound titleKey="profile.userNotFound" descriptionKey="error404.userDescription" />

    return (
        <div className="min-h-screen bg-background">
            <Navbar />
            <main className="container mx-auto px-4 py-8">
                <div className="mx-auto max-w-5xl space-y-6">
                    <ProfileHeader
                        name={user.name || user.email || t("profile.fallbackName")}
                        rating={user.reviewAvg ?? 0}
                    />

                    <Link
                        to="/profile/reviews?view=received"
                        className="block rounded-xl focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#2563eb] focus-visible:ring-offset-2"
                    >
                        <Card className="border border-border bg-card p-5 shadow-sm transition-colors hover:border-[#2563eb]/50 hover:bg-[#2563eb]/5 hover:shadow-md">
                            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                                <div className="flex items-start gap-3">
                                    <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-primary/10">
                                        <MessageSquare className="h-5 w-5 text-primary" />
                                    </div>
                                    <div>
                                        <h2 className="text-lg font-semibold text-foreground">
                                            {t("profile.reviewsCard.title")}
                                        </h2>
                                        <p className="text-sm text-muted-foreground">
                                            {t("profile.reviewsCard.description")}
                                        </p>
                                    </div>
                                </div>
                                <span className="inline-flex items-center gap-2 text-sm font-semibold text-[#2563eb]">
                                    {t("profile.reviewsCard.action")}
                                    <ArrowRight className="h-4 w-4" />
                                </span>
                            </div>
                        </Card>
                    </Link>

                    <SwapStats
                        totalEarned={user.totalEarned || 0}
                        totalSpent={user.totalSpent || 0}
                        completedSwaps={user.totalSwaps || 0}
                    />

                    {saveError && (
                        <div className="rounded-md border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
                            {saveError}
                        </div>
                    )}

                    {saveSuccess && (
                        <div className="rounded-md border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
                            {saveSuccess}
                        </div>
                    )}

                    <div className="grid gap-6 md:grid-cols-2">
                        <BiographyCard
                            biography={user.bio}
                            isSaving={savingField === "bio"}
                            onSave={handleBiographySave}
                        />
                        <LanguagesCard
                            primaryLanguage={user.locale || "en"}
                            isSaving={savingField === "locale"}
                            onSave={handleLocaleSave}
                        />
                    </div>

                    <div className="grid gap-6 md:grid-cols-2">
                        <TravelPreferencesCard
                            preferences={user.travelPreferences}
                            isSaving={savingField === "travelPreferences"}
                            onSave={handleTravelPreferencesSave}
                        />
                        <AccountCard
                            email={user.email || t("profile.account.email")}
                            verified={verified}
                            isSaving={savingField === "password"}
                            onPasswordSave={handlePasswordSave}
                        />
                    </div>
                </div>
            </main>
        </div>
    )
}
