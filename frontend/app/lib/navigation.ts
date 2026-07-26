export const RETURN_TO_PARAM = "returnTo"

export function currentPath(location: { pathname: string; search: string; hash?: string }) {
    return `${location.pathname}${location.search}${location.hash || ""}`
}

export function withReturnTo(destination: string, returnTo: string) {
    const [path, hash = ""] = destination.split("#", 2)
    const [pathname, query = ""] = path.split("?", 2)
    const params = new URLSearchParams(query)
    params.set(RETURN_TO_PARAM, returnTo)
    const search = params.toString()

    return `${pathname}${search ? `?${search}` : ""}${hash ? `#${hash}` : ""}`
}

export function readReturnTo(searchParams: URLSearchParams, fallback = "/") {
    const returnTo = searchParams.get(RETURN_TO_PARAM)

    // Only allow internal application paths.
    if (!returnTo || !returnTo.startsWith("/") || returnTo.startsWith("//")) {
        return fallback
    }

    return returnTo
}
