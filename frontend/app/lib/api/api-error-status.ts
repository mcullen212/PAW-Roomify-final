import { isAxiosError } from "axios"

export function getApiErrorStatus(error: unknown) {
    return isAxiosError(error) ? error.response?.status : undefined
}

export function hasApiErrorStatus(status: number, ...errors: unknown[]) {
    return errors.some((error) => getApiErrorStatus(error) === status)
}
