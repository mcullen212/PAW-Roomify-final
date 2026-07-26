import { useEffect, useState } from "react"
import countriesAPI from "@/lib/api/countriesAPI"
import i18n from "@/i18n/i18n"

let cachedCountries: string[] | null = null

export function useCountries() {
    const [countries, setCountries] = useState<string[]>(cachedCountries ?? [])
    const [loading, setLoading] = useState(cachedCountries === null)
    const [error, setError] = useState("")

    useEffect(() => {
        if (cachedCountries !== null) {
            setCountries(cachedCountries)
            setLoading(false)
            return
        }

        let active = true

        setLoading(true)
        setError("")

        countriesAPI.getCountries()
            .then((data) => {
                if (!active) return
                cachedCountries = data
                setCountries(data)
            })
            .catch(() => {
                if (!active) return
                setError(i18n.t("countries.errors.load"))
            })
            .finally(() => {
                if (active) {
                    setLoading(false)
                }
            })

        return () => {
            active = false
        }
    }, [])

    return {
        countries,
        loading,
        error,
    }
}
