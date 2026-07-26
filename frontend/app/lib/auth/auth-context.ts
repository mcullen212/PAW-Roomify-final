import { createContext } from "react"
import type { AuthContextValue } from "~/lib/interfaces/auth"

export type { AuthContextValue, RegisterData } from "~/lib/interfaces/auth"

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
