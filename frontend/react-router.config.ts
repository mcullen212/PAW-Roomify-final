import type { Config } from "@react-router/dev/config";

export default {
    ssr: false,
    basename: process.env.VITE_PUBLIC_URL || "/webapp/",
    appDirectory: "app",
} satisfies Config