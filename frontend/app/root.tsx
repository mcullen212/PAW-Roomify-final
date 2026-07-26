import type React from "react"
import { Links, Meta, Outlet, Scripts, ScrollRestoration } from "react-router"
import type { LinksFunction } from "react-router"
import { Toaster } from "sonner"
import "./globals.css"
import i18n from '@/i18n/i18n.ts'
import { QueryClientProvider } from "@tanstack/react-query"
import { AuthProvider } from "@/lib/auth/AuthProvider"
import { ApiInterceptors } from "@/lib/hooks/ApiInterceptors"
import { queryClient } from "@/lib/query"
import { pageTitle } from "@/lib/utils"


export const links: LinksFunction = () => [
    { rel: "icon", type: "image/png", href: `${import.meta.env.BASE_URL}favicon.png` },
    { rel: "preconnect", href: "https://fonts.googleapis.com" },
    {
        rel: "preconnect",
        href: "https://fonts.gstatic.com",
        crossOrigin: "anonymous",
    },
]

export function meta() {
    return [
        { title: pageTitle() },
        { name: "description", content: i18n.t("pageDescriptions.home") },
    ]
}

export function Layout({ children }: { children: React.ReactNode }) {
    return (
        <html lang="en">
        <head>
            <meta charSet="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <Meta />
            <Links />
        </head>
        <body className="font-sans antialiased">
        {children}
        <ScrollRestoration />
        <Scripts />
        </body>
        </html>
    )
}

export default function Root() {
    return (
        <AuthProvider>
            <QueryClientProvider client={queryClient}>
                <ApiInterceptors>
                    <Outlet />
                </ApiInterceptors>
            </QueryClientProvider>
            <Toaster richColors position="bottom-right" />
        </AuthProvider>
    )
}
