import { useTranslation } from "react-i18next";
import { UserReviewsSection } from "@/components/profile/user-reviews-section";

type PublicUserReviewsSectionProps = {
    userId: number;
};

export function PublicUserReviewsSection({ userId }: PublicUserReviewsSectionProps) {
    const { t } = useTranslation();

    return (
        <UserReviewsSection
            userId={userId}
            title={t("publicProfile.reviews.title")}
            emptyMessage={t("publicProfile.reviews.empty")}
            errorMessage={t("publicProfile.reviews.error")}
        />
    );
}
