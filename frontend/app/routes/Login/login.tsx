import React, { useState } from "react"
import { Link, useLocation, useNavigate } from "react-router"
import { Mail, Lock, Eye, EyeOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Checkbox } from "@/components/ui/checkbox"
import type { Route } from "../+types/login.ts"

import { useTranslation } from "react-i18next";
import { HttpStatus } from "@/lib/api/httpStatus";
import { useAuth } from "~/lib/auth/useAuth";
import { pageTitleKey } from "@/lib/utils";
import { toast } from "sonner";


export function meta({}: Route.MetaArgs) {
    return [
        { title: pageTitleKey("pageTitles.login") },
        { name: "description", content: pageTitleKey("pageDescriptions.login") },
    ]
}

export default function LoginPage() {
    const { t } = useTranslation();

    const navigate = useNavigate()
    const location = useLocation()
    const { login } = useAuth()
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [showPassword, setShowPassword] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState("")

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setIsLoading(true)
        setError("")

        try {
            const verificationEmailResent = await login(email, password)
            if (verificationEmailResent) {
                toast.info(t("login.verification_email_resent"), { duration: 10000 })
            }
            const returnTo = (location.state as { returnTo?: string } | null)?.returnTo
            navigate(returnTo || "/profile", { replace: true })
        } catch (err: any) {
            if (err.response) {
                switch(err.response.status){
                    case HttpStatus.TOO_MANY_REQUESTS:
                        setError(t("login.errors.too_many_requests"))
                        break;
                    default:
                        setError(t("login.errors.invalid_credentials"))
                }

            }
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-background flex">
            {/* Left Side - Form */}
            <div className="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-8 xl:px-12">
                <div className="w-full max-w-md mx-auto">
                    {/* Logo */}
                    <Link to="/" className="flex items-center gap-2 text-2xl font-bold text-foreground mb-8">
                        <img src={`${import.meta.env.BASE_URL}favicon.png`} alt="Logo" className="w-8 h-8 object-contain" />
                        <span>Roomify</span>
                    </Link>

                    {/* Header */}
                    <div className="mb-8">
                        <h1 className="text-3xl font-bold text-foreground mb-2">{t("login.title")}</h1>
                        <p className="text-muted-foreground">
                            {t("login.subtitle")}
                        </p>
                    </div>

                    {/* Form */}
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div className="space-y-2">
                            <Label htmlFor="email" className="text-sm font-medium">
                                {t("login.email_label")}
                            </Label>
                            <div className="relative">
                                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                                <Input
                                    id="email"
                                    type="email"
                                    placeholder={t("login.email_placeholder")}
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    className="pl-11 h-12"
                                    required
                                />
                            </div>
                        </div>

                        <div className="space-y-2">
                            <div className="flex items-center justify-between">
                                <Label htmlFor="password" className="text-sm font-medium">
                                    {t("login.password_label")}
                                </Label>
                                <Link
                                    to="/login/forgot-password"
                                    className="text-sm text-primary hover:text-primary/80 transition-colors"
                                >
                                    {t("login.forgot_password")}
                                </Link>
                            </div>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    placeholder={t("login.password_placeholder")}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="pl-11 pr-11 h-12"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                                >
                                    {showPassword ? (
                                        <EyeOff className="w-5 h-5" />
                                    ) : (
                                        <Eye className="w-5 h-5" />
                                    )}
                                </button>
                            </div>
                        </div>

                        {error && (
                            <div className="text-red-600 text-sm bg-red-50 p-3 rounded-md border border-red-200">
                                {error}
                            </div>
                        )}

                        <Button type="submit" className="w-full h-12 text-base" disabled={isLoading}>
                            {isLoading ? t("login.button_loading") : t("login.button_submit")}
                        </Button>
                    </form>

                    {/* Sign up link */}
                    <p className="mt-8 text-center text-sm text-muted-foreground">
                        {t("login.no_account")}{" "}
                        <Link to="/signup" className="font-semibold text-primary hover:text-primary/80 transition-colors">
                            {t("login.signup_link")}
                        </Link>
                    </p>
                </div>
            </div>

            {/* Right Side - Image */}
            <div className="hidden lg:block lg:w-1/2 relative bg-muted">
                <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-primary/5" />
                <div className="absolute inset-0 flex items-center justify-center p-12">
                    <div className="max-w-lg text-center">
                        <div className="bg-card/90 backdrop-blur rounded-2xl p-8 shadow-2xl border border-border">
                            <h2 className="text-2xl font-bold text-foreground mb-4">
                                {t("login.hero.title")}
                            </h2>
                            <p className="text-muted-foreground mb-6">
                                {t("login.hero.description")}
                            </p>
                            <div className="flex items-center justify-center gap-4 text-sm">
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">10K+</div>
                                    <div className="text-muted-foreground">{t("login.hero.stats.users")}</div>
                                </div>
                                <div className="h-12 w-px bg-border" />
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">50+</div>
                                    <div className="text-muted-foreground">{t("login.hero.stats.countries")}</div>
                                </div>
                                <div className="h-12 w-px bg-border" />
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">5K+</div>
                                    <div className="text-muted-foreground">{t("login.hero.stats.exchanges")}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
