import { HttpStatus } from "@/lib/api/httpStatus"
import { hasApiErrorStatus } from "@/lib/api/api-error-status"
import BadRequest from "~/routes/errors/bad-request"
import NotFound from "~/routes/errors/not-found-page"
import Forbidden from "~/routes/errors/forbidden-page"
import InternalServerError from "~/routes/errors/internal-error-page"

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
    if (badRequest || hasApiErrorStatus(HttpStatus.BAD_REQUEST, error)) return <BadRequest />
    if (hasApiErrorStatus(HttpStatus.NOT_FOUND, error)) {
        return <NotFound titleKey={notFoundTitleKey} descriptionKey={notFoundDescriptionKey} />
    }
    if (hasApiErrorStatus(HttpStatus.FORBIDDEN, error)) return <Forbidden />
    if (hasApiErrorStatus(HttpStatus.INTERNAL_SERVER_ERROR, error)) return <InternalServerError />

    return null
}
