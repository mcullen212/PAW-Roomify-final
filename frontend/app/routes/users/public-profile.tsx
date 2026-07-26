import { Compass, Home, Users, type LucideIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router";
import { Navbar } from "@/components/Navbar";
import { ProfileHeader } from "@/components/profile/profile-header";
import { PublicUserReviewsSection } from "@/components/profile/public-user-reviews-section";
import { Card } from "@/components/ui/card";
import { pageTitleKey } from "@/lib/utils";
import { useApiServices } from "~/lib/hooks/useApiServices";
import i18n from "@/i18n/i18n";
import { getApiErrorMessage } from "~/lib/api/api-error-message";
import NotFound from "~/routes/errors/not-found-page";
import { getApiErrorPage } from "~/routes/errors/api-error-page";

type PublicUser = {
    id: number;
    name?: string | null;
    bio?: string | null;
    travelPreferences?: string | null;
    totalReviewsReceived?: number;
    averageRating?: number;
    totalRooms?: number;
};

type PublicInfoCardProps = {
    icon: LucideIcon;
    title: string;
    emptyMessage: string;
    children?: string | null;
};

function PublicInfoCard({ icon: Icon, title, emptyMessage, children }: PublicInfoCardProps) {
    const hasContent = Boolean(children?.trim());

    return (
        <Card className="p-6">
            <div className="mb-4 flex items-center gap-2 border-b border-border pb-4">
                <Icon className="h-5 w-5 text-primary" />
                <h2 className="text-lg font-semibold text-foreground">{title}</h2>
            </div>
            <p className={hasContent ? "leading-relaxed text-foreground" : "italic text-muted-foreground"}>
                {hasContent ? children : emptyMessage}
            </p>
        </Card>
    );
}

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.publicProfile") },
        { name: "description", content: i18n.t("pageDescriptions.publicProfile") },
    ];
}

export default function PublicProfilePage() {
    const { t } = useTranslation();
    const { id } = useParams();
    const { userService } = useApiServices();
    const parsedUserId = Number.parseInt(id || "", 10);
    const userId = Number.isFinite(parsedUserId) && parsedUserId > 0 ? parsedUserId : undefined;
    const userQuery = userService.useGetPublicUser(userId);
    const user = userQuery.data as PublicUser | undefined;

    if (!userId) {
        const apiErrorPage = getApiErrorPage(userQuery.error, {
            badRequest: true,
            notFoundTitleKey: "profile.userNotFound",
            notFoundDescriptionKey: "error404.userDescription",
        });

        return apiErrorPage;
    }

    const apiErrorPage = getApiErrorPage(userQuery.error, {
        notFoundTitleKey: "profile.userNotFound",
        notFoundDescriptionKey: "error404.userDescription",
    });
    if (apiErrorPage) return apiErrorPage;

    if (userQuery.isLoading) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex h-64 items-center justify-center text-primary">{t("profile.loading")}</div>
            </div>
        );
    }

    if (userQuery.isError) {
        return (
            <div className="min-h-screen bg-background">
                <Navbar />
                <div className="flex h-64 items-center justify-center px-4 text-center text-red-500">
                    {getApiErrorMessage(userQuery.error, t("profile.errors.load"))}
                </div>
            </div>
        );
    }
    if (!user) return <NotFound titleKey="profile.userNotFound" descriptionKey="error404.userDescription" />;

    const displayName = user.name || t("profile.fallbackName");
    const averageRating = user.averageRating ?? 0;
    const totalReviews = user.totalReviewsReceived ?? 0;
    const totalRooms = user.totalRooms ?? 0;

    return (
        <div className="min-h-screen bg-background">
            <Navbar />
            <main className="container mx-auto px-4 py-8">
                <div className="mx-auto max-w-5xl space-y-6">
                    <ProfileHeader
                        name={displayName}
                        title={displayName}
                        rating={averageRating}
                        reviewCount={totalReviews}
                        extraMetrics={[
                            {
                                icon: Home,
                                label: t("publicProfile.stats.rooms"),
                                value: totalRooms,
                            },
                        ]}
                    />

                    <div className="grid gap-6 md:grid-cols-2">
                        <PublicInfoCard
                            icon={Users}
                            title={t("profile.biography.title")}
                            emptyMessage={t("publicProfile.biography.empty")}
                        >
                            {user.bio}
                        </PublicInfoCard>
                        <PublicInfoCard
                            icon={Compass}
                            title={t("profile.travelPreferences.title")}
                            emptyMessage={t("publicProfile.travelPreferences.empty")}
                        >
                            {user.travelPreferences}
                        </PublicInfoCard>
                    </div>

                    <PublicUserReviewsSection userId={userId} />
                </div>
            </main>
        </div>
    );
}
