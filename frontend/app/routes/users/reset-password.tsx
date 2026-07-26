import React, { useState } from "react"
import { Link, useNavigate } from "react-router"
import { Lock, Eye, EyeOff, Check, ShieldCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { useAuth } from "@/lib/auth/useAuth"
import { pageTitleKey } from "@/lib/utils";
import { useApiServices } from "~/lib/hooks/useApiServices";
import { useTranslation } from "react-i18next";
import i18n from "@/i18n/i18n";
import { getPasswordRequirements, isPasswordFormValid } from "@/lib/auth/password-policy";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.resetPassword") },
        { name: "description", content: i18n.t("pageDescriptions.resetPassword") },
    ]
}

export default function ResetPasswordPage() {
    const { t } = useTranslation()
    const navigate = useNavigate()
    const { email, login } = useAuth()
    const { userService } = useApiServices()
    const resetPasswordMutation = userService.useResetPassword()

    const [password, setPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [showPassword, setShowPassword] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState("")

    const passwordRequirements = getPasswordRequirements(password, confirmPassword)
    const isFormValid = isPasswordFormValid(password, confirmPassword)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        if (!isFormValid) return

        setIsLoading(true)
        setError("")

        try {
            await resetPasswordMutation.mutateAsync(password)

            if (!email) {
                throw new Error(t("resetPassword.errors.missingEmail"))
            }

            await login(email, password)

            navigate("/?reset=success")
        } catch (err) {
            console.error("Error resetting password:", err)
            setError(t("resetPassword.errors.sessionExpired"))
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <div className="min-h-screen bg-background flex">
            <div className="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-8 xl:px-12 py-12">
                <div className="w-full max-w-md mx-auto">
                    <Link to="/" className="flex items-center gap-2 text-2xl font-bold text-foreground mb-8">
                        <img src={`${import.meta.env.BASE_URL}favicon.png`} alt="Logo" className="w-8 h-8 object-contain" />
                        <span>Roomify</span>
                    </Link>

                    <div className="mb-8">
                        <h1 className="text-3xl font-bold text-foreground mb-2">{t("resetPassword.title")}</h1>
                        <p className="text-muted-foreground">
                            {t("resetPassword.subtitle")}
                        </p>
                    </div>

                    <form onSubmit={handleSubmit} className="space-y-5">
                        <div className="space-y-2">
                            <Label htmlFor="password" className="text-sm font-medium">{t("resetPassword.newPassword")}</Label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                                <Input
                                    id="password"
                                    type={showPassword ? "text" : "password"}
                                    placeholder={t("resetPassword.newPasswordPlaceholder")}
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    className="pl-11 pr-11 h-12"
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword(!showPassword)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground"
                                >
                                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                </button>
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="confirmPassword" className="text-sm font-medium">{t("resetPassword.confirmNewPassword")}</Label>
                            <div className="relative">
                                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                                <Input
                                    id="confirmPassword"
                                    type={showPassword ? "text" : "password"}
                                    placeholder={t("resetPassword.confirmNewPasswordPlaceholder")}
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    className="pl-11 h-12"
                                    required
                                />
                            </div>
                        </div>

                        {/* Password Requirements (Estética del Signup) */}
                        <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                            <div className="grid grid-cols-2 gap-2">
                                {passwordRequirements.map((req, index) => (
                                    <div key={index} className="flex items-center gap-2">
                                        <div className={`w-4 h-4 rounded-full flex items-center justify-center ${req.met ? "bg-green-500" : "bg-muted-foreground/30"}`}>
                                            {req.met && <Check className="w-3 h-3 text-white" />}
                                        </div>
                                        <span className={`text-xs ${req.met ? "text-foreground" : "text-muted-foreground"}`}>
                                            {t(req.translationKey)}
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {error && <p className="text-sm text-destructive bg-destructive/10 p-3 rounded-md">{error}</p>}

                        <Button type="submit" className="w-full h-12 text-base" disabled={isLoading || !isFormValid}>
                            {isLoading ? t("resetPassword.updating") : t("resetPassword.submit")}
                        </Button>
                    </form>
                </div>
            </div>

            {/* Right Side - Image Background */}
            <div className="hidden lg:block lg:w-1/2 relative bg-muted">
                <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-primary/5" />
                <div className="absolute inset-0 flex items-center justify-center p-12">
                    <div className="max-w-lg text-center bg-card/90 backdrop-blur rounded-2xl p-8 shadow-2xl border border-border">
                        <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
                            <ShieldCheck className="w-8 h-8 text-primary" />
                        </div>
                        <h2 className="text-2xl font-bold text-foreground mb-4">{t("resetPassword.heroTitle")}</h2>
                        <p className="text-muted-foreground">
                            {t("resetPassword.heroDescription")}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    )
}
