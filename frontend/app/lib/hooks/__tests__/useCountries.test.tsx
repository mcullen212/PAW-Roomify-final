import { renderHook, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import countriesAPI from "@/lib/api/countriesAPI"
import { useCountries } from "../useCountries"

vi.mock("@/lib/api/countriesAPI", () => ({
    default: {
        getCountries: vi.fn(),
    },
}))

describe("useCountries", () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it("loads countries once and reuses the in-memory cache", async () => {
        vi.mocked(countriesAPI.getCountries).mockResolvedValueOnce(["Argentina", "Brazil"])

        const firstHook = renderHook(() => useCountries())

        await waitFor(() => {
            expect(firstHook.result.current.countries).toEqual(["Argentina", "Brazil"])
        })

        const secondHook = renderHook(() => useCountries())

        expect(secondHook.result.current.countries).toEqual(["Argentina", "Brazil"])
        expect(secondHook.result.current.loading).toBe(false)
        expect(countriesAPI.getCountries).toHaveBeenCalledTimes(1)
    })
})
