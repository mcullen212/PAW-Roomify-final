import { Link } from "react-router-dom"
import { Home } from "lucide-react"
import { Button } from "@/components/ui/button"
import { useTranslation } from "react-i18next";
import {Navbar} from "@/components/Navbar.tsx";
import { pageTitleKey } from "@/lib/utils";

export function meta() {
    return [{ title: pageTitleKey("pageTitles.notFound") }]
}

type NotFoundProps = {
    titleKey?: string
    descriptionKey?: string
}

export default function NotFound({
    titleKey = "error404.title",
    descriptionKey = "error404.description",
}: NotFoundProps) {
    const { t } = useTranslation();

    return (
        <div className="min-h-screen flex flex-col bg-white">
            <Navbar/>

            <main className="flex-1 flex items-center justify-center px-4 py-8">
                <div className="w-full max-w-md text-center">
                    {/* Icon */}
                    <div className="mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-full bg-red-100">
                        <span className="text-5xl font-light text-red-400">?</span>
                    </div>

                    {/* Title */}
                    <h1 className="mb-3 text-4xl font-bold text-red-500">
                        {t(titleKey)}
                    </h1>

                    {/* Description */}
                    <p className="mb-8 text-gray-600">
                        {t(descriptionKey)}
                    </p>

                    {/* Button */}
                    <Button asChild className="bg-blue-700 hover:bg-blue-800 text-white px-6 py-2 h-auto">
                        <Link to="/">
                            <Home className="mr-2 h-4 w-4" />
                            {t("error404.buttonHome")}
                        </Link>
                    </Button>
                </div>
            </main>
        </div>
    )
}
