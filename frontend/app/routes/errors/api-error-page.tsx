import { HttpStatus } from "@/lib/api/httpStatus"
import { hasApiErrorStatus } from "@/lib/api/api-error-status"
import { Navigate } from "react-router-dom"

type ApiErrorPageOptions = {
    badRequest?: boolean
    notFoundTitleKey: string
    notFoundDescriptionKey: string
}

export function getApiErrorPage(
    error: unknown,
    {
        badRequest = false,
        notFoundTitleKey,
        notFoundDescriptionKey,
    }: ApiErrorPageOptions,
) {
    if (badRequest || hasApiErrorStatus(HttpStatus.BAD_REQUEST, error)) return <Navigate to="/400" replace />
    if (hasApiErrorStatus(HttpStatus.NOT_FOUND, error)) {
        return (
            <Navigate
                to="/404"
                replace
                state={{ titleKey: notFoundTitleKey, descriptionKey: notFoundDescriptionKey }}
            />
        )
    }
    if (hasApiErrorStatus(HttpStatus.FORBIDDEN, error)) return <Navigate to="/403" replace />
    if (hasApiErrorStatus(HttpStatus.INTERNAL_SERVER_ERROR, error)) return <Navigate to="/500" replace />

    return null
}
