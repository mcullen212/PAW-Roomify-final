import type { AxiosInstance } from "axios"
import api from "./api"
import { VndType } from "./vndTypes"

export const createCountriesAPI = (client: AxiosInstance) => {
    const getCountries = async (): Promise<string[]> => {
        const response = await client.get("/countries", {
            headers: {
                Accept: VndType.APPLICATION_COUNTRIES,
            },
        })

        return Array.isArray(response.data) ? response.data : []
    }

    return {
        getCountries,
    }
}

const countriesAPI = createCountriesAPI(api)

export default countriesAPI
