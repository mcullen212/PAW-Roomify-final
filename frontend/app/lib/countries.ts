const REGIONAL_INDICATOR_OFFSET = 127397
const DEFAULT_COUNTRY_NAME_LOCALES = ["en", "en-US", "es", "es-AR"]
const ISO_ALPHA_2_COUNTRY_CODES = new Set([
    "AD", "AE", "AF", "AG", "AI", "AL", "AM", "AO", "AQ", "AR", "AS", "AT", "AU", "AW", "AX", "AZ",
    "BA", "BB", "BD", "BE", "BF", "BG", "BH", "BI", "BJ", "BL", "BM", "BN", "BO", "BQ", "BR", "BS",
    "BT", "BV", "BW", "BY", "BZ", "CA", "CC", "CD", "CF", "CG", "CH", "CI", "CK", "CL", "CM", "CN",
    "CO", "CR", "CU", "CV", "CW", "CX", "CY", "CZ", "DE", "DJ", "DK", "DM", "DO", "DZ", "EC", "EE",
    "EG", "EH", "ER", "ES", "ET", "FI", "FJ", "FK", "FM", "FO", "FR", "GA", "GB", "GD", "GE", "GF",
    "GG", "GH", "GI", "GL", "GM", "GN", "GP", "GQ", "GR", "GS", "GT", "GU", "GW", "GY", "HK", "HM",
    "HN", "HR", "HT", "HU", "ID", "IE", "IL", "IM", "IN", "IO", "IQ", "IR", "IS", "IT", "JE", "JM",
    "JO", "JP", "KE", "KG", "KH", "KI", "KM", "KN", "KP", "KR", "KW", "KY", "KZ", "LA", "LB", "LC",
    "LI", "LK", "LR", "LS", "LT", "LU", "LV", "LY", "MA", "MC", "MD", "ME", "MF", "MG", "MH", "MK",
    "ML", "MM", "MN", "MO", "MP", "MQ", "MR", "MS", "MT", "MU", "MV", "MW", "MX", "MY", "MZ", "NA",
    "NC", "NE", "NF", "NG", "NI", "NL", "NO", "NP", "NR", "NU", "NZ", "OM", "PA", "PE", "PF", "PG",
    "PH", "PK", "PL", "PM", "PN", "PR", "PS", "PT", "PW", "PY", "QA", "RE", "RO", "RS", "RU", "RW",
    "SA", "SB", "SC", "SD", "SE", "SG", "SH", "SI", "SJ", "SK", "SL", "SM", "SN", "SO", "SR", "SS",
    "ST", "SV", "SX", "SY", "SZ", "TC", "TD", "TF", "TG", "TH", "TJ", "TK", "TL", "TM", "TN", "TO",
    "TR", "TT", "TV", "TW", "TZ", "UA", "UG", "UM", "US", "UY", "UZ", "VA", "VC", "VE", "VG", "VI",
    "VN", "VU", "WF", "WS", "YE", "YT", "ZA", "ZM", "ZW",
])

const regionCodeCandidates = Array.from(ISO_ALPHA_2_COUNTRY_CODES)

const countryNameMaps = new Map<string, Map<string, string>>()

export function countryCodeToFlagEmoji(code?: string | null) {
    const normalizedCode = code?.trim().toUpperCase()
    if (!normalizedCode || !/^[A-Z]{2}$/.test(normalizedCode)) return ""
    if (!ISO_ALPHA_2_COUNTRY_CODES.has(normalizedCode)) return ""

    return String.fromCodePoint(
        ...Array.from(normalizedCode).map((letter) => letter.charCodeAt(0) + REGIONAL_INDICATOR_OFFSET)
    )
}

function normalizeCountryName(name: string) {
    return name
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/['`]/g, "")
        .replace(/&/g, "and")
        .replace(/[^a-zA-Z0-9]+/g, " ")
        .trim()
        .toLowerCase()
}

function getCountryNameMap(locale: string) {
    const cached = countryNameMaps.get(locale)
    if (cached) return cached

    const names = new Map<string, string>()
    if (typeof Intl.DisplayNames === "undefined") {
        countryNameMaps.set(locale, names)
        return names
    }

    const displayNames = new Intl.DisplayNames([locale], { type: "region" })
    regionCodeCandidates.forEach((code) => {
        const displayName = displayNames.of(code)
        const normalizedDisplayName = displayName ? normalizeCountryName(displayName) : ""
        if (displayName && displayName !== code && !names.has(normalizedDisplayName)) {
            names.set(normalizedDisplayName, code)
        }
    })

    countryNameMaps.set(locale, names)
    return names
}

function getPreferredCountryNameLocales() {
    const browserLocales = typeof navigator === "undefined"
        ? []
        : [navigator.language, ...(navigator.languages || [])].filter(Boolean)

    return Array.from(new Set([...browserLocales, ...DEFAULT_COUNTRY_NAME_LOCALES]))
}

export function countryNameToCountryCode(countryName?: string | null, locales = getPreferredCountryNameLocales()) {
    const normalizedName = normalizeCountryName(countryName || "")
    if (!normalizedName) return ""

    for (const locale of locales) {
        const code = getCountryNameMap(locale).get(normalizedName)
        if (code) return code
    }

    return ""
}

export function countryFlagEmoji(countryName?: string | null, countryCode?: string | null) {
    return countryCodeToFlagEmoji(countryCode) || countryCodeToFlagEmoji(countryNameToCountryCode(countryName))
}
