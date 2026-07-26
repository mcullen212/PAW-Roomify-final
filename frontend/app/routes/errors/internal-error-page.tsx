import { Link } from "react-router-dom"
import { Home, RefreshCw, ServerCrash } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useTranslation } from "react-i18next";
import { Navbar } from "@/components/Navbar.tsx";
import { pageTitleKey } from "@/lib/utils";

export function meta() {
    return [{ title: pageTitleKey("pageTitles.serverError") }]
}

export default function InternalServerError() {
    const { t } = useTranslation();

    const handleRefresh = () => {
        window.location.reload();
    };

    return (
        <div className="min-h-screen flex flex-col bg-white">
            <Navbar/>

            <main className="flex-1 flex items-center justify-center px-4 py-8">
                <div className="w-full max-w-md text-center">
                    {/* Icon */}
                    <div className="mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-full bg-red-100">
                        <ServerCrash className="h-12 w-12 text-red-500" />
                    </div>

                    {/* Title */}
                    <h1 className="mb-3 text-4xl font-bold text-red-500">
                        {t("error500.title")}
                    </h1>

                    {/* Description */}
                    <p className="mb-8 text-gray-600">
                        {t("error500.desc")}
                    </p>

                    {/* Buttons */}
                    <div className="flex flex-col sm:flex-row gap-4 justify-center">
                        <Button
                            onClick={handleRefresh}
                            className="bg-blue-700 hover:bg-blue-800 text-white px-6 py-2 h-auto"
                        >
                            <RefreshCw className="mr-2 h-4 w-4" />
                            {t("error500.buttonRetry")}
                        </Button>

                        <Button
                            variant="outline"
                            asChild
                            className="px-6 py-2 h-auto"
                        >
                            <Link to="/">
                                <Home className="mr-2 h-4 w-4" />
                                {t("error500.buttonHome")}
                            </Link>
                        </Button>
                    </div>
                </div>
            </main>
        </div>
    )
}
