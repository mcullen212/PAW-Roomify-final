import { describe, expect, it } from "vitest"
import {
    countryCodeToFlagEmoji,
    countryFlagEmoji,
    countryNameToCountryCode,
} from "../countries"

describe("country helpers", () => {
    it("converts ISO alpha-2 country codes to flag emojis", () => {
        expect(countryCodeToFlagEmoji("ar")).toBe("🇦🇷")
        expect(countryCodeToFlagEmoji("US")).toBe("🇺🇸")
    })

    it("ignores invalid country codes", () => {
        expect(countryCodeToFlagEmoji("ARG")).toBe("")
        expect(countryCodeToFlagEmoji("1A")).toBe("")
        expect(countryCodeToFlagEmoji("DD")).toBe("")
        expect(countryCodeToFlagEmoji("")).toBe("")
    })

    it("finds a country code from a localized country name", () => {
        expect(countryNameToCountryCode("Argentina", ["en"])).toBe("AR")
        expect(countryNameToCountryCode("Brasil", ["es"])).toBe("BR")
        expect(countryNameToCountryCode("Alemania", ["es"])).toBe("DE")
    })

    it("prefers an explicit country code when building a flag", () => {
        expect(countryFlagEmoji("France", "CA")).toBe("🇨🇦")
        expect(countryFlagEmoji("Alemania", "DD")).toBe("🇩🇪")
        expect(countryFlagEmoji("France")).toBe("🇫🇷")
    })
})
