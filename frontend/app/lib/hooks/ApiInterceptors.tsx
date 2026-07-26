import type { ReactNode } from "react";
import { useApi } from "~/lib/hooks/useApi";

export function ApiInterceptors({ children }: { children: ReactNode }) {
    useApi();

    return <>{children}</>;
}
