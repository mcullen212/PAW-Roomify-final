import { Link } from "react-router-dom"
import { Home, ShieldX } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useTranslation } from "react-i18next";
import { Navbar } from "@/components/Navbar.tsx";
import { pageTitleKey } from "@/lib/utils";

export function meta() {
    return [{ title: pageTitleKey("pageTitles.forbidden") }]
}

export default function Forbidden() {
    const { t } = useTranslation();

    return (
        <div className="min-h-screen flex flex-col bg-white">
            <Navbar/>

            <main className="flex-1 flex items-center justify-center px-4 py-8">
                <div className="w-full max-w-md text-center">
                    {/* Icon */}
                    <div className="mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-full bg-red-100">
                        <ShieldX className="h-12 w-12 text-red-500" />
                    </div>

                    {/* Title */}
                    <h1 className="mb-3 text-4xl font-bold text-red-500">
                        {t("error403.title")}
                    </h1>

                    {/* Description */}
                    <p className="mb-8 text-gray-600">
                        {t("error403.description")}
                    </p>

                    {/* Button */}
                    <Button asChild className="bg-blue-700 hover:bg-blue-800 text-white px-6 py-2 h-auto">
                        <Link to="/">
                            <Home className="mr-2 h-4 w-4" />
                            {t("error403.buttonHome")}
                        </Link>
                    </Button>
                </div>
            </main>
        </div>
    )
}
