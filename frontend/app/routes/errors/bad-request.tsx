import { Link } from "react-router-dom"
import { AlertTriangle, Home } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Button } from "@/components/ui/button"
import { Navbar } from "@/components/Navbar.tsx"
import { pageTitleKey } from "@/lib/utils"

export function meta() {
    return [{ title: pageTitleKey("pageTitles.badRequest") }]
}

export default function BadRequest() {
    const { t } = useTranslation()

    return (
        <div className="flex min-h-screen flex-col bg-white">
            <Navbar />

            <main className="flex flex-1 items-center justify-center px-4 py-8">
                <div className="w-full max-w-md text-center">
                    <div className="mx-auto mb-8 flex h-24 w-24 items-center justify-center rounded-full bg-amber-100">
                        <AlertTriangle className="h-12 w-12 text-amber-500" />
                    </div>

                    <h1 className="mb-3 text-4xl font-bold text-amber-600">
                        {t("error400.title")}
                    </h1>

                    <p className="mb-8 text-gray-600">
                        {t("error400.description")}
                    </p>

                    <Button asChild className="h-auto bg-blue-700 px-6 py-2 text-white hover:bg-blue-800">
                        <Link to="/">
                            <Home className="mr-2 h-4 w-4" />
                            {t("error400.buttonHome")}
                        </Link>
                    </Button>
                </div>
            </main>
        </div>
    )
}
