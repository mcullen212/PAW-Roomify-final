import React from "react"

import { useState, useRef } from "react"
import {Link, useNavigate, useSearchParams} from "react-router"
import { jwtDecode } from "jwt-decode"
import { ArrowLeft, ShieldCheck, KeyRound } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {useTranslation} from "react-i18next";
import { useAuth } from "@/lib/auth/useAuth";
import { pageTitleKey } from "@/lib/utils";
import i18n from "@/i18n/i18n";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.verifyAccount") },
        { name: "description", content: i18n.t("pageDescriptions.verifyAccount") },
    ]
}

function resolveOtpEmail(queryEmail: string | null) {
    if (queryEmail) return queryEmail

    const accessToken = sessionStorage.getItem("jwt")
    if (!accessToken) return ""

    try {
        return jwtDecode<{ sub?: string }>(accessToken).sub ?? ""
    } catch {
        return ""
    }
}

export default function VerifyTokenPage() {
    const { t } = useTranslation();
    const navigate = useNavigate()
    const { validateOTP, syncAuthState } = useAuth()
    const [searchParams] = useSearchParams()
    const [email] = useState(() => resolveOtpEmail(searchParams.get("email")))
    const [code, setCode] = useState<string[]>(["", "", "", "", "", ""])
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState("")
    const [success, setSuccess] = useState("");

    const inputRefs = useRef<(HTMLInputElement | null)[]>([])

    const type = searchParams.get("type") || "verify"

    const handleChange = (index: number, value: string) => {
        if (value && !/^[a-zA-Z0-9]$/.test(value)) return

        const newCode = [...code]
        newCode[index] = value
        setCode(newCode)
        setError("")

        if (value && index < 5) {
            inputRefs.current[index + 1]?.focus()
        }
    }

    const handleKeyDown = (index: number, e: React.KeyboardEvent<HTMLInputElement>) => {
        if (e.key === "Backspace" && !code[index] && index > 0) {
            inputRefs.current[index - 1]?.focus()
        }

        if (e.key === "ArrowLeft" && index > 0) {
            inputRefs.current[index - 1]?.focus()
        }
        if (e.key === "ArrowRight" && index < 5) {
            inputRefs.current[index + 1]?.focus()
        }
    }

    const handlePaste = (e: React.ClipboardEvent) => {
        e.preventDefault()
        const pastedData = e.clipboardData
            .getData("text")
            .replace(/[^a-zA-Z0-9]/g, "")
            .toUpperCase()
            .slice(0, 6)

        if (pastedData) {
            const newCode = [...code]
            for (let i = 0; i < pastedData.length; i++) {
                newCode[i] = pastedData[i]
            }
            setCode(newCode)

            const nextEmptyIndex = newCode.findIndex((c) => !c)
            inputRefs.current[nextEmptyIndex === -1 ? 5 : nextEmptyIndex]?.focus()
        }
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()

        const token = code.join("")
        if (token.length !== 6) {
            setError(t("token.enter_6_digits"))
            return
        }

        setIsLoading(true)
        setError("")

        try {
            await validateOTP(email, token)

            if (type === "reset") {
                navigate("/login/reset-password")
            } else {
                await syncAuthState()
                setSuccess(t("token.verification_success"))
                setTimeout(() => navigate("/profile", { replace: true }), 3000)
            }
        } catch (err) {
            setError(t("token.token_invalid"))
        } finally {
            setIsLoading(false)
        }
    }

    const isCodeComplete = code.every((digit) => digit !== "")

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

                    {/* Back to forgot password */}
                    <Link
                        to="../login/forgot-password.tsx"
                        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors mb-8"
                    >
                        <ArrowLeft className="w-4 h-4" />
                        {t("button.back")}
                    </Link>

                    {/* Header */}
                    <div className="mb-8">
                        <h1 className="text-3xl font-bold text-foreground mb-2">
                            {t("verify.title")}
                        </h1>
                        <p className="text-muted-foreground">
                            {t("verify.subtitle")}
                        </p>
                    </div>

                    {/* Form */}
                    <form onSubmit={handleSubmit} className="space-y-6">
                        <div className="space-y-4">
                            <div className="flex items-center justify-center gap-2 sm:gap-3">
                                {code.map((digit, index) => (
                                    <Input
                                        key={index}
                                        ref={(el) => { inputRefs.current[index] = el }}
                                        type="text"
                                        inputMode="numeric"
                                        maxLength={1}
                                        value={digit}
                                        onChange={(e) => handleChange(index, e.target.value)}
                                        onKeyDown={(e) => handleKeyDown(index, e)}
                                        onPaste={handlePaste}
                                        className={`w-12 h-14 sm:w-14 sm:h-16 text-center text-2xl font-bold ${
                                            error ? "border-destructive focus-visible:ring-destructive" : ""
                                        } ${digit ? "border-primary" : ""}`}
                                        aria-label={t("verify.digit_label", { number: index + 1 })}
                                    />
                                ))}
                            </div>

                            {error && (
                                <p className="text-sm text-destructive text-center">{error}</p>
                            )}

                            {success && (
                                <div className="text-emerald-700 text-sm bg-emerald-50 p-3 rounded-md border border-emerald-200 text-center mb-4">
                                    {success}
                                </div>
                            )}
                        </div>

                        <Button
                            type="submit"
                            className="w-full h-12 text-base"
                            disabled={isLoading || !isCodeComplete || !email}
                        >
                            {isLoading ? t("verify.button_loading") : t("verify.button_submit")}
                        </Button>
                    </form>

                </div>
            </div>

            {/* Right Side - Image */}
            <div className="hidden lg:block lg:w-1/2 relative bg-muted">
                <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-primary/5" />
                <div className="absolute inset-0 flex items-center justify-center p-12">
                    <div className="max-w-lg text-center">
                        <div className="bg-card/90 backdrop-blur rounded-2xl p-8 shadow-2xl border border-border">
                            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-6">
                                <ShieldCheck className="w-8 h-8 text-primary" />
                            </div>
                            <h2 className="text-2xl font-bold text-foreground mb-4">
                                {t("verify.hero_title")}
                            </h2>
                            <p className="text-muted-foreground mb-6">
                                {t("verify.hero_description")}
                            </p>

                            {/* Visual code example */}
                            <div className="flex items-center justify-center gap-2 mb-6">
                                {[1, 2, 3, 4, 5, 6].map((num) => (
                                    <div
                                        key={num}
                                        className="w-10 h-12 bg-primary/10 rounded-lg flex items-center justify-center"
                                    >
                                        <KeyRound className="w-5 h-5 text-primary/50" />
                                    </div>
                                ))}
                            </div>

                            <div className="flex items-center justify-center gap-4 text-sm">
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">10 min</div>
                                    <div className="text-muted-foreground">{t("verify.validity")}</div>
                                </div>
                                <div className="h-12 w-px bg-border" />
                                <div className="text-center">
                                    <div className="text-2xl font-bold text-primary">{t("verify.secure")}</div>
                                    <div className="text-muted-foreground">{t("verify.one_time_use")}</div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}
