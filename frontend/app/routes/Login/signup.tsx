import React, { useState } from "react"
import { Link, useNavigate } from "react-router"
import { Mail, Lock, Eye, EyeOff, User, Check } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

import {useTranslation} from "react-i18next";
import { HttpStatus } from "@/lib/api/httpStatus";
import { useAuth } from "@/lib/auth/useAuth";
import { getPasswordRequirements, isPasswordFormValid } from "@/lib/auth/password-policy";
import { pageTitleKey } from "@/lib/utils";
import i18n from "@/i18n/i18n";

export function meta() {
    return [
        { title: pageTitleKey("pageTitles.signup") },
        { name: "description", content: i18n.t("pageDescriptions.signup") },
    ]
}

export default function SignupPage() {
    const { t } = useTranslation();

    const navigate = useNavigate()
    const { register } = useAuth()

    const [name, setName] = useState("")
    const [email, setEmail] = useState("")
    const [password, setPassword] = useState("")
    const [confirmPassword, setConfirmPassword] = useState("")
    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState("")

    const passwordRequirements = getPasswordRequirements(password, confirmPassword);
    const isFormValid = isPasswordFormValid(password, confirmPassword);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        
        if (!isFormValid) {
            setError(t("signup.form.errors.not_match"))
            return
        }

        setIsLoading(true)
        setError("")

        try {
            const registered = await register({
                name,
                email,
                password
            });

            if (!registered) {
                setError(t("signup.form.errors.unexpected"))
                return
            }

            navigate("/profile", { replace: true })

        } catch (err: any) {
            switch(err.response?.status){
                case HttpStatus.CONFLICT:
                    setError(t("signup.form.errors.already_exists"))
                    break;
                default:
                    setError(t("signup.form.errors.unexpected"))
            }
        } finally {
            setIsLoading(false)
        }
    }

    return (
    <div className="min-h-screen bg-background flex">
      {/* Left Side - Image */}
      <div className="hidden lg:block lg:w-1/2 relative bg-muted">
        <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-primary/5" />
        <div className="absolute inset-0 flex items-center justify-center p-12">
          <div className="max-w-lg">
            <div className="bg-card/90 backdrop-blur rounded-2xl p-8 shadow-2xl border border-border">
              <h2 className="text-2xl font-bold text-foreground mb-6">
                  {t("signup.hero.title")}
              </h2>
              <ul className="space-y-4">
                  {(t("signup.hero.benefits", { returnObjects: true }) as string[]).map((benefit, index) => (
                  <li key={index} className="flex items-start gap-3">
                    <div className="w-6 h-6 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0 mt-0.5">
                      <Check className="w-4 h-4 text-primary" />
                    </div>
                    <span className="text-foreground">{benefit}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-8 pt-6 border-t border-border">
                <p className="text-sm text-muted-foreground italic">
                    "{t("signup.hero.testimonial.text")}"
                </p>
                <div className="mt-4 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center">
                    <User className="w-5 h-5 text-primary" />
                  </div>
                  <div>
                    <div className="font-medium text-foreground">{t("signup.hero.testimonial.author")}</div>
                    <div className="text-xs text-muted-foreground">{t("signup.hero.testimonial.date")}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Right Side - Form */}
      <div className="flex-1 flex flex-col justify-center px-4 sm:px-6 lg:px-8 xl:px-12 py-12">
        <div className="w-full max-w-md mx-auto">
            <Link to="/" className="flex items-center gap-2 text-2xl font-bold text-foreground mb-8">
                <img src={`${import.meta.env.BASE_URL}favicon.png`} alt={t("signup.logo_alt")} className="w-8 h-8 object-contain" />
                <span>Roomify</span>
            </Link>

          {/* Header */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-foreground mb-2">{t("signup.form.title")}</h1>
            <p className="text-muted-foreground">
                {t("signup.form.subtitle")}
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="name" className="text-sm font-medium">
                  {t("signup.form.name_label")}
              </Label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="name"
                  type="text"
                  placeholder={t("signup.form.name_placeholder")}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="pl-11 h-12"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="email" className="text-sm font-medium">
                  {t("signup.form.email_label")}
              </Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder={t("signup.form.email_placeholder")}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="pl-11 h-12"
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="password" className="text-sm font-medium">
                  {t("signup.form.password_label")}
              </Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder={t("signup.form.password_placeholder")}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="pl-11 pr-11 h-12"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-label={showPassword ? t("signup.form.hide_password") : t("signup.form.show_password")}
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

            <div className="space-y-2">
              <Label htmlFor="confirmPassword" className="text-sm font-medium">
                  {t("signup.form.confirm_password_label")}
              </Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
                <Input
                  id="confirmPassword"
                  type={showConfirmPassword ? "text" : "password"}
                  placeholder={t("signup.form.confirm_password_placeholder")}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="pl-11 pr-11 h-12"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  aria-label={showConfirmPassword ? t("signup.form.hide_confirm_password") : t("signup.form.show_confirm_password")}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                >
                  {showConfirmPassword ? (
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

            {/* Password Requirements */}
            {password.length > 0 && (
              <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide mb-2">
                    {t("signup.form.password_requirements_title")}
                </p>
                <div className="grid grid-cols-2 gap-2">
                  {passwordRequirements.map((req, index) => (
                    <div key={index} className="flex items-center gap-2">
                      <div
                        className={`w-4 h-4 rounded-full flex items-center justify-center transition-colors ${
                          req.met ? "bg-green-500" : "bg-muted-foreground/30"
                        }`}
                      >
                        {req.met && <Check className="w-3 h-3 text-white" />}
                      </div>
                      <span
                        className={`text-xs transition-colors ${
                          req.met ? "text-foreground" : "text-muted-foreground"
                        }`}
                      >
                        {t(req.translationKey)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <Button
              type="submit"
              className="w-full h-12 text-base"
              disabled={isLoading || !isFormValid}
            >
                {isLoading ? t("signup.form.button_loading") : t("signup.form.button_submit")}
            </Button>
          </form>

          {/* Login link */}
          <p className="mt-8 text-center text-sm text-muted-foreground">
              {t("signup.form.already_have_account")}{" "}
              <Link to="/login" className="font-semibold text-primary hover:text-primary/80 transition-colors">
                  {t("signup.form.login_link")}
            </Link>
          </p>
        </div>
      </div>
    </div>
    )
}
