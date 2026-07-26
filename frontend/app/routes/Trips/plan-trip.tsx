import { Link } from "react-router"
import { useTranslation } from "react-i18next"
import { Navbar } from "@/components/Navbar"
import { Button } from "@/components/ui/button"
import { pageTitleKey } from "@/lib/utils";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.planTrip") },
        { name: "description", content: pageTitleKey("pageDescriptions.planTrip") },
    ]
}

export default function PlanTripPage() {
    const { t } = useTranslation()

    return (
        <div className="min-h-screen bg-background">
            <Navbar />
            <main className="mx-auto flex max-w-3xl flex-col items-center px-4 py-20 text-center sm:px-6 lg:px-8">
                <h1 className="text-3xl font-bold text-foreground">{t("trips.actions.planTrip")}</h1>
                <p className="mt-4 text-muted-foreground">
                    {t("planTripPage.description")}
                </p>
                <Button className="mt-8" asChild>
                    <Link to="/trips">{t("trips.actions.backToTrips")}</Link>
                </Button>
            </main>
        </div>
    )
}
