import React from "react"

import { useState } from "react"
import {Link, useNavigate} from "react-router"
import { Search, Mail, ArrowLeft, CheckCircle2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { pageTitleKey } from "@/lib/utils";
import { useApiServices } from "~/lib/hooks/useApiServices";
import { HttpStatus } from "@/lib/api/httpStatus";
import { useTranslation } from "react-i18next";
import i18n from "@/i18n/i18n";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.forgotPassword") },
        { name: "description", content: i18n.t("pageDescriptions.forgotPassword") },
    ]
}

export default function ForgotPasswordPage() {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const { userService } = useApiServices()
    const requestPasswordResetOtpMutation = userService.useRequestPasswordResetOtp()
    const [email, setEmail] = useState("")
    const [isLoading, setIsLoading] = useState(false)
    const [isSubmitted, setIsSubmitted] = useState(false)
    const [error, setError] = useState("")

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setIsLoading(true)
        setError("")

        try {
            await requestPasswordResetOtpMutation.mutateAsync(email)
            const params = new URLSearchParams({ type: "reset", email })
            navigate(`/verify-token?${params.toString()}`)
            setIsSubmitted(true)
        } catch (err: any) {
            console.error("Error requesting OTP:", err)
            switch (err.response?.status) {
                case HttpStatus.NOT_FOUND:
                    setError(t("forgot_password.errors.email_not_found"))
                    break
                default:
                    setError(t("forgot_password.errors.unexpected"))
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
                        <Search className="w-8 h-8 text-primary" />
                        <span>Roomify</span>
                    </Link>

                    {/* Back to login */}
                    <Link
                        to="/login"
                        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors mb-8"
                    >
                        <ArrowLeft className="w-4 h-4" />
                        {t("forgot_password.back_to_login")}
                    </Link>

                    {!isSubmitted ? (
                        <>
                            {/* Header */}
                            <div className="mb-8">
                                <h1 className="text-3xl font-bold text-foreground mb-2">{t("forgot_password.title")}</h1>
                                <p className="text-muted-foreground">
                                    {t("forgot_password.subtitle")}
                                </p>
                            </div>

                            {/* Form */}
                            <form onSubmit={handleSubmit} className="space-y-6">
                                <div className="space-y-2">
                                    <Label htmlFor="email" className="text-sm font-medium">
                                        {t("forgot_password.email_label")}
                                    </Label>
                                    <div className="relative">
                                        <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                                        <Input
                                            id="email"
                                            type="email"
                                            placeholder={t("forgot_password.email_placeholder")}
                                            value={email}
                                            onChange={(e) => setEmail(e.target.value)}
                                            className="pl-11 h-12"
                                            required
                                        />
                                    </div>
                                </div>

                                {error && (
                                    <div className="text-red-600 text-sm bg-red-50 p-3 rounded-md border border-red-200">
                                        {error}
                                    </div>
                                )}

                                <Button type="submit" className="w-full h-12 text-base" disabled={isLoading}>
                                    {isLoading ? t("forgot_password.button_loading") : t("forgot_password.button_submit")}
                                </Button>
                            </form>
                        </>
                    ) : (
                        <>
                            {/* Success State */}
                            <div className="text-center">
                                <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
                                    <CheckCircle2 className="w-8 h-8 text-primary" />
                                </div>
                                <h1 className="text-3xl font-bold text-foreground mb-2">{t("forgot_password.success.title")}</h1>
                                <p className="text-muted-foreground mb-8">
                                    {t("forgot_password.success.sent_to")} <span className="font-medium text-foreground">{email}</span>
                                </p>

                                <Link to="/verify-token">
                                    <Button className="w-full h-12 text-base mb-4">
                                        {t("forgot_password.success.enter_code")}
                                    </Button>
                                </Link>

                                <p className="text-sm text-muted-foreground">
                                    {t("forgot_password.success.didnt_receive")}{" "}
                                    <button
                                        type="button"
                                        onClick={() => setIsSubmitted(false)}
                                        className="font-semibold text-primary hover:text-primary/80 transition-colors"
                                    >
                                        {t("forgot_password.success.resend")}
                                    </button>
                                </p>
                            </div>
                        </>
                    )}

                    {/* Sign up link */}
                    <p className="mt-8 text-center text-sm text-muted-foreground">
                        {t("forgot_password.remember_password")}{" "}
                        <Link to="/login" className="font-semibold text-primary hover:text-primary/80 transition-colors">
                            {t("forgot_password.sign_in")}
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
                            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
                                <Mail className="w-8 h-8 text-primary" />
                            </div>
                            <h2 className="text-2xl font-bold text-foreground mb-4">
                                {t("forgot_password.hero.title")}
                            </h2>
                            <p className="text-muted-foreground mb-6">
                                {t("forgot_password.hero.description")}
                            </p>
                            <div className="flex items-center justify-center gap-4 text-sm">
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">256-bit</div>
                                    <div className="text-muted-foreground">{t("forgot_password.hero.stats.encryption")}</div>
                                </div>
                                <div className="h-12 w-px bg-border" />
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">2FA</div>
                                    <div className="text-muted-foreground">{t("forgot_password.hero.stats.protected")}</div>
                                </div>
                                <div className="h-12 w-px bg-border" />
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">24/7</div>
                                    <div className="text-muted-foreground">{t("forgot_password.hero.stats.support")}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
