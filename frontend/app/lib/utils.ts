import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"
import {jwtDecode} from "jwt-decode";
import i18n from "~/i18n/i18n.ts";

export const APP_NAME = "Roomify"

export function pageTitle(title?: string) {
    return title ? `${APP_NAME} / ${title}` : APP_NAME
}

export function pageTitleKey(key?: string) {
    return pageTitle(key ? i18n.t(key) : undefined)
}

export async function syncPrimaryLanguage(locale: string) {
    const nextLocale = locale.startsWith("es") ? "es" : "en"

    if (i18n.language === nextLocale) {
        localStorage.setItem("i18nextLng", nextLocale)
        return
    }

    localStorage.setItem("i18nextLng", nextLocale)
    await i18n.changeLanguage(nextLocale)
}

export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs))
}

export function parseLinkHeader(linkHeader: string | undefined) {
    if (!linkHeader) return {};

    const links: any = {};
    const parts = linkHeader.split(',');

    parts.forEach(part => {
        const section = part.split(';');
        if (section.length !== 2) return;

        const url = section[0].replace(/<(.*)>/, '$1').trim();
        const name = section[1].replace(/rel="(.*)"/, '$1').trim();

        links[name] = url;
    });

    return links;
}

export function getAuthToken() {
    return sessionStorage.getItem("jwt");
}

export function getUserIdFromToken(): number | null {
    const token = getAuthToken();
    if (!token) return null;
    try {
        const decoded: any = jwtDecode(token);
        return decoded.userId || null;
    } catch (error) {
        console.error("Error decoding token:", error);
        return null;
    }
}

export function formatDate(date: Date): string {
    return new Intl.DateTimeFormat("en-GB", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
    }).format(date);
}

export function formatApiDate(date: string): string {
    if (!date) return "";

    const [year, month, day] = date.split("T")[0].split("-");
    if (year && month && day) {
        return `${day}/${month}/${year}`;
    }

    return formatDate(new Date(date));
}


