import { describe, expect, it } from "vitest"
import {
  getPasswordRequirements,
  isPasswordFormValid,
  isPasswordValid,
} from "../password-policy"

describe("password policy", () => {
  it("accepts passwords with length, number, and uppercase letter", () => {
    expect(isPasswordValid("Password1")).toBe(true)
  })

  it("rejects passwords without an uppercase letter", () => {
    expect(isPasswordValid("password1")).toBe(false)
  })

  it("rejects passwords without a number", () => {
    expect(isPasswordValid("Password")).toBe(false)
  })

  it("requires confirmation to match when validating a form", () => {
    expect(isPasswordFormValid("Password1", "Password1")).toBe(true)
    expect(isPasswordFormValid("Password1", "Password2")).toBe(false)
  })

  it("reports each requirement independently", () => {
    const requirements = getPasswordRequirements("password1", "password1")

    expect(requirements).toEqual([
      {
        id: "length",
        translationKey: "signup.form.password_requirements.length",
        met: true,
      },
      {
        id: "number",
        translationKey: "signup.form.password_requirements.number",
        met: true,
      },
      {
        id: "uppercase",
        translationKey: "signup.form.password_requirements.uppercase",
        met: false,
      },
      {
        id: "match",
        translationKey: "signup.form.password_requirements.match",
        met: true,
      },
    ])
  })
})
