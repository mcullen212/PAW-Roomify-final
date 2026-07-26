import { reactRouter } from "@react-router/dev/vite";
import { loadEnv } from "vite";
import { defineConfig } from "vitest/config";
import tsconfigPaths from "vite-tsconfig-paths";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
    const env = loadEnv(mode, process.cwd(), '');
    const basePath = env.VITE_PUBLIC_URL || "/paw-2025b-10/";

    return {
        base: basePath,
        plugins: [tailwindcss(), mode === "test" ? undefined : reactRouter(), tsconfigPaths()],
        test: {
            environment: "jsdom",
            globals: true,
        },
    };
});
