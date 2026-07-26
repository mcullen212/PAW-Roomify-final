function looksLikeHtml(value: string) {
    const trimmed = value.trim().toLowerCase();
    return trimmed.startsWith("<!doctype") || trimmed.startsWith("<html") || trimmed.includes("<body");
}

function getResponseData(error: unknown) {
    return (error as { response?: { data?: unknown } })?.response?.data;
}

function getDetailMessages(details: unknown) {
    if (!Array.isArray(details)) {
        return [];
    }

    return details
        .map((detail) => {
            if (typeof detail === "string") return detail;
            if (detail && typeof detail === "object" && "message" in detail) {
                const message = (detail as { message?: unknown }).message;
                return typeof message === "string" ? message : "";
            }
            return "";
        })
        .filter(Boolean);
}

export function getApiErrorMessage(error: unknown, fallback: string): string;
export function getApiErrorMessage(error: unknown): string | null;
export function getApiErrorMessage(error: unknown, fallback?: string) {
    const data = getResponseData(error);

    if (typeof data === "string") {
        return looksLikeHtml(data) ? fallback ?? null : data;
    }

    if (data && typeof data === "object") {
        const message = (data as { message?: unknown }).message;
        if (typeof message === "string" && message.trim()) {
            return message;
        }

        const errorMessage = (data as { error?: unknown }).error;
        if (typeof errorMessage === "string" && errorMessage.trim()) {
            return errorMessage;
        }

        const detailMessages = getDetailMessages((data as { details?: unknown }).details);
        if (detailMessages.length > 0) {
            return detailMessages.join(" ");
        }
    }

    if (!data && error instanceof Error && error.message) {
        return error.message;
    }

    return fallback ?? null;
}
