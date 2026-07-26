import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { AccountCard } from "../account-card"

vi.mock("react-i18next", async (importOriginal) => {
  const actual = await importOriginal<typeof import("react-i18next")>()

  return {
    ...actual,
    useTranslation: () => ({
      t: (key: string) => key,
    }),
  }
})

describe("AccountCard", () => {
  it("requires the shared password policy before saving a new password", async () => {
    const onPasswordSave = vi.fn().mockResolvedValue(undefined)

    render(
      <AccountCard
        email="ada@example.com"
        verified
        onPasswordSave={onPasswordSave}
      />,
    )

    fireEvent.click(screen.getByRole("button", { name: "profile.account.editPassword" }))

    const saveButton = screen.getByRole("button", { name: "profile.account.savePassword" }) as HTMLButtonElement
    const currentPasswordInput = screen.getByPlaceholderText("profile.account.currentPassword")
    const newPasswordInput = screen.getByPlaceholderText("profile.account.newPassword")

    fireEvent.change(currentPasswordInput, { target: { value: "CurrentPassword1" } })
    fireEvent.change(newPasswordInput, { target: { value: "password1" } })

    expect(saveButton.disabled).toBe(true)

    fireEvent.change(newPasswordInput, { target: { value: "Password1" } })

    expect(saveButton.disabled).toBe(false)

    fireEvent.click(saveButton)

    await waitFor(() => {
      expect(onPasswordSave).toHaveBeenCalledWith("CurrentPassword1", "Password1")
    })
  })
})
