import { Link, useNavigate, useSearchParams } from "react-router";
import { ArrowLeft, Inbox, PencilLine, type LucideIcon } from "lucide-react";
import { useCallback, useEffect } from "react";
import { useTranslation } from "react-i18next";
import i18n from "@/i18n/i18n";
import { Navbar } from "@/components/Navbar";
import { ProfileHeader } from "@/components/profile/profile-header";
import { UserReviewsSection } from "@/components/profile/user-reviews-section";
import { Button } from "@/components/ui/button";
import { cn, pageTitleKey } from "@/lib/utils";
import { useAuth } from "~/lib/auth/useAuth";
import { useApiServices } from "~/lib/hooks/useApiServices";
import NotFound from "~/routes/errors/not-found-page";
import { getApiErrorPage } from "~/routes/errors/api-error-page";

type ProfileReviewsView = "received" | "written";

type ProfileReviewsTab = {
    id: ProfileReviewsView;
    labelKey: string;
    icon: LucideIcon;
};

const profileReviewsTabs: ProfileReviewsTab[] = [
    {
        id: "received",
        labelKey: "profileReviews.tabs.received.label",
        icon: Inbox,
    },
    {
        id: "written",
        labelKey: "profileReviews.tabs.written.label",
        icon: PencilLine,
    },
];

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.profileReviews") },
        { name: "description", content: i18n.t("pageDescriptions.profileReviews") },
    ];
}

export default function ProfileReviewsPage() {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { userId } = useAuth();
    const { userService } = useApiServices();
    const [searchParams, setSearchParams] = useSearchParams();
    const profileQuery = userService.useGetProfile(userId);
    const user = profileQuery.data;
    const viewParam = searchParams.get("view");
    const activeView: ProfileReviewsView = viewParam === "written" ? "written" : "received";
    const activeTab = profileReviewsTabs.find((tab) => tab.id === activeView) ?? profileReviewsTabs[0];

    const updateView = useCallback((view: ProfileReviewsView, replace = false, resetPage = true) => {
        setSearchParams((currentParams) => {
            const nextParams = new URLSearchParams(currentParams);
            nextParams.set("view", view);
            if (resetPage) {
                nextParams.delete("page");
            }
            return nextParams;
        }, { replace });
    }, [setSearchParams]);

    useEffect(() => {
        if (!userId) {
            navigate("/login");
        }
    }, [navigate, userId]);

    useEffect(() => {
        if (viewParam !== activeView) {
            updateView(activeView, true, false);
        }
    }, [activeView, updateView, viewParam]);

    if (profileQuery.isLoading) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex h-64 items-center justify-center text-primary">{t("profile.loading")}</div>
            </div>
        );
    }

    const apiErrorPage = getApiErrorPage(profileQuery.error, {
        notFoundTitleKey: "profile.userNotFound",
        notFoundDescriptionKey: "error404.userDescription",
    });
    if (apiErrorPage) return apiErrorPage;
    if (profileQuery.isError) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex h-64 items-center justify-center px-4 text-center text-red-500">{t("profile.errors.load")}</div>
            </div>
        );
    }
    if (!user || !userId) return <NotFound titleKey="profile.userNotFound" descriptionKey="error404.userDescription" />;

    return (
        <div className="min-h-screen bg-background">
            <Navbar />
            <main className="container mx-auto px-4 py-8">
                <div className="mx-auto max-w-5xl space-y-6">
                    <Button variant="ghost" asChild className="gap-2">
                        <Link to="/profile">
                            <ArrowLeft className="h-4 w-4" />
                            {t("profileReviews.backToProfile")}
                        </Link>
                    </Button>

                    <ProfileHeader
                        name={user.name || user.email || t("profile.fallbackName")}
                        rating={user.reviewAvg || 0}
                        reviewCount={user.totalReviews || 0}
                        writtenReviewCount={user.totalWrittenReviews || 0}
                    />

                    <div
                        aria-label={t("profileReviews.tabs.ariaLabel")}
                        className="flex gap-2 overflow-x-auto rounded-xl bg-muted/40 p-1"
                        role="tablist"
                    >
                        {profileReviewsTabs.map((tab) => {
                            const isActive = activeView === tab.id;
                            const Icon = tab.icon;

                            return (
                                <button
                                    aria-controls="profile-reviews-panel"
                                    aria-selected={isActive}
                                    className={cn(
                                        "flex min-w-fit cursor-pointer items-center justify-center gap-2 rounded-lg border px-4 py-2.5 text-sm font-semibold transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
                                        isActive
                                            ? "border-[#2563eb] bg-[#2563eb] text-white shadow-sm hover:bg-[#1d4ed8] hover:shadow-md"
                                            : "border-transparent bg-transparent text-muted-foreground hover:border-[#2563eb]/20 hover:bg-background hover:text-[#2563eb] hover:shadow-sm",
                                    )}
                                    id={`profile-reviews-tab-${tab.id}`}
                                    key={tab.id}
                                    onClick={() => updateView(tab.id)}
                                    role="tab"
                                    type="button"
                                >
                                    <Icon className="h-4 w-4" aria-hidden="true" />
                                    <span>{t(tab.labelKey)}</span>
                                </button>
                            );
                        })}
                    </div>

                    <UserReviewsSection
                        userId={userId}
                        currentUserId={userId}
                        source={activeView}
                        allowDeleteOwnReviews={activeView === "written"}
                        pageParamName="page"
                        panelId="profile-reviews-panel"
                        labelledBy={`profile-reviews-tab-${activeTab.id}`}
                        title={t(`profileReviews.tabs.${activeTab.id}.title`)}
                        description={t(`profileReviews.tabs.${activeTab.id}.description`)}
                        emptyMessage={t(`profileReviews.tabs.${activeTab.id}.empty`)}
                        errorMessage={t("profileReviews.error")}
                    />
                </div>
            </main>
        </div>
    );
}
