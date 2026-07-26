export const PASSWORD_MIN_LENGTH = 8

export type PasswordRequirementId = "length" | "number" | "uppercase" | "match"

export interface PasswordRequirement {
  id: PasswordRequirementId
  translationKey: string
  met: boolean
}

export function hasMinimumPasswordLength(password: string) {
  return password.length >= PASSWORD_MIN_LENGTH
}

export function hasPasswordNumber(password: string) {
  return /\d/.test(password)
}

export function hasPasswordUppercase(password: string) {
  return /[A-Z]/.test(password)
}

export function isPasswordValid(password: string) {
  return hasMinimumPasswordLength(password) && hasPasswordNumber(password) && hasPasswordUppercase(password)
}

export function doPasswordsMatch(password: string, confirmPassword: string) {
  return password === confirmPassword && confirmPassword.length > 0
}

export function getPasswordRequirements(password: string, confirmPassword?: string): PasswordRequirement[] {
  const requirements: PasswordRequirement[] = [
    {
      id: "length",
      translationKey: "signup.form.password_requirements.length",
      met: hasMinimumPasswordLength(password),
    },
    {
      id: "number",
      translationKey: "signup.form.password_requirements.number",
      met: hasPasswordNumber(password),
    },
    {
      id: "uppercase",
      translationKey: "signup.form.password_requirements.uppercase",
      met: hasPasswordUppercase(password),
    },
  ]

  if (confirmPassword !== undefined) {
    requirements.push({
      id: "match",
      translationKey: "signup.form.password_requirements.match",
      met: doPasswordsMatch(password, confirmPassword),
    })
  }

  return requirements
}

export function isPasswordFormValid(password: string, confirmPassword: string) {
  return isPasswordValid(password) && doPasswordsMatch(password, confirmPassword)
}
