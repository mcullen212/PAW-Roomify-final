import { beforeEach, describe, expect, it, vi } from "vitest"
import { createCountriesAPI } from "../countriesAPI"
import { VndType } from "../vndTypes"

const client = {
    get: vi.fn(),
}

describe("countriesAPI", () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it("loads countries with the countries media type", async () => {
        client.get.mockResolvedValueOnce({ data: ["Argentina", "Brazil"] })
        const countriesAPI = createCountriesAPI(client as any)

        await expect(countriesAPI.getCountries()).resolves.toEqual(["Argentina", "Brazil"])
        expect(client.get).toHaveBeenCalledWith("/countries", {
            headers: {
                Accept: VndType.APPLICATION_COUNTRIES,
            },
        })
    })

    it("falls back to an empty list when the response is not an array", async () => {
        client.get.mockResolvedValueOnce({ data: { countries: ["Argentina"] } })
        const countriesAPI = createCountriesAPI(client as any)

        await expect(countriesAPI.getCountries()).resolves.toEqual([])
    })
})
