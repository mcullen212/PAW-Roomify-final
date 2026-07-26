import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react"
import { jwtDecode } from "jwt-decode"
import { useNavigate } from "react-router"
import api from "~/lib/api/api"
import userApi from "~/lib/api/userAPI"
import { VndType } from "~/lib/api/vndTypes"
import { queryClient } from "~/lib/query"
import { AuthContext } from "./auth-context"
import type { RegisterData } from "~/lib/interfaces/auth"

interface JwtClaims {
  sub?: string
  userId?: number
  role?: string | string[]
  roles?: string[]
  scope?: string
  exp?: number
}

interface AuthState {
  authenticated: boolean
  accessToken?: string
  refreshToken?: string
  email?: string
  userId?: number
  roles: string[]
  verified: boolean
}

const EMPTY_AUTH_STATE: AuthState = {
  authenticated: false,
  roles: [],
  verified: false,
}

function clearTokens(dispatchAuthChange = true) {
  sessionStorage.removeItem("jwt")
  sessionStorage.removeItem("refresh")
  if (dispatchAuthChange) {
    window.dispatchEvent(new Event("auth-change"))
  }
}

function getHeaderValue(headers: unknown, names: string[]) {
  for (const name of names) {
    const value =
      (headers as any)?.[name] ??
      (headers as any)?.[name.toLowerCase()] ??
      (typeof (headers as any)?.get === "function" ? (headers as any).get(name) : undefined)

    if (Array.isArray(value)) {
      return value[0]?.toString()
    }

    if (value) {
      return value.toString()
    }
  }

  return undefined
}

function storeTokens(accessToken: string, refreshToken?: string) {
  sessionStorage.setItem("jwt", accessToken)
  if (refreshToken) {
    sessionStorage.setItem("refresh", refreshToken)
  }
}

function applyTokenResponse(
  headers: unknown,
  setAuthState: (authState: AuthState) => void,
) {
  const accessToken = getHeaderValue(headers, ["access-token", "access-token"])
  const refreshToken = getHeaderValue(headers, ["refresh-token"])

  if (!accessToken) {
    throw new Error("Missing access token in auth response")
  }

  const storedRefreshToken = sessionStorage.getItem("refresh") ?? undefined
  const nextRefreshToken = refreshToken ?? storedRefreshToken
  const nextAuthState = decodeAuthState(accessToken, nextRefreshToken)

  storeTokens(accessToken, nextRefreshToken)
  setAuthState(nextAuthState)
  return nextAuthState
}

async function refreshStoredAuthState(refreshToken: string): Promise<AuthState> {
  const response = await api.head("/", {
    headers: {
      Authorization: `Bearer ${refreshToken}`,
      Accept: VndType.APPLICATION_API,
    },
  })

  const accessToken = getHeaderValue(response.headers, ["access-token"])
  const nextRefreshToken = getHeaderValue(response.headers, ["refresh-token"]) ?? refreshToken

  if (!accessToken) {
    throw new Error("Missing access token in refresh response")
  }

  storeTokens(accessToken, nextRefreshToken)
  return decodeAuthState(accessToken, nextRefreshToken)
}

function decodeAuthState(accessToken: string, refreshToken?: string): AuthState {
  try {
    const claims = jwtDecode<JwtClaims>(accessToken)
    if (claims.exp && claims.exp * 1000 <= Date.now() && !refreshToken) {
      clearTokens()
      return EMPTY_AUTH_STATE
    }

    const roleClaim = claims.roles ?? claims.role ?? []
    const roles = Array.isArray(roleClaim) ? roleClaim : [roleClaim]

    return {
      authenticated: true,
      accessToken,
      refreshToken,
      email: claims.sub,
      userId: claims.userId,
      roles,
      verified: roles.includes("ROLE_VERIFIED_USER"),
    }
  } catch {
      clearTokens(false)
      return EMPTY_AUTH_STATE
  }
}

function readStoredAuth(): AuthState {
  const accessToken = sessionStorage.getItem("jwt") ?? undefined
  const refreshToken = sessionStorage.getItem("refresh") ?? undefined

  if (!accessToken) {
    clearTokens(false)
    return EMPTY_AUTH_STATE
  }

  return decodeAuthState(accessToken, refreshToken)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [authState, setAuthState] = useState<AuthState>(EMPTY_AUTH_STATE)
  const [loading, setLoading] = useState(true)

  const syncAuthState = useCallback(async () => {
    const storedAuthState = readStoredAuth()
    setAuthState(storedAuthState)

    if (!storedAuthState.authenticated || storedAuthState.verified || !storedAuthState.refreshToken) {
      return
    }

    try {
      setAuthState(await refreshStoredAuthState(storedAuthState.refreshToken))
    } catch {
      setAuthState(readStoredAuth())
    }
  }, [])

  useEffect(() => {
    let mounted = true

    const initializeAuthState = async () => {
      await syncAuthState()
      if (mounted) {
        setLoading(false)
      }
    }

    void initializeAuthState()

    const handleStorageChange = (event: StorageEvent) => {
      if (event.key === "jwt" || event.key === "refresh" || event.key === null) {
        void syncAuthState()
      }
    }

    const handleAuthChange = () => {
      void syncAuthState()
    }

    window.addEventListener("storage", handleStorageChange)
    window.addEventListener("auth-change", handleAuthChange)

    return () => {
      mounted = false
      window.removeEventListener("storage", handleStorageChange)
      window.removeEventListener("auth-change", handleAuthChange)
    }
  }, [syncAuthState])

  const login = useCallback(async (email: string, password: string) => {
    const basicAuth = btoa(`${email}:${password}`)
    const response = await api.head("/", {
      headers: {
        Authorization: `Basic ${basicAuth}`,
        Accept: VndType.APPLICATION_API,
      },
    })
    applyTokenResponse(response.headers, setAuthState)
    return false
  }, [])

  const validateOTP = useCallback(async (email: string, otpToken: string) => {
    const basicAuth = btoa(`${email}:${otpToken}`)
    const response = await api.head("/", {
      headers: {
        Authorization: `Basic ${basicAuth}`,
        Accept: VndType.APPLICATION_API,
      },
    })
    applyTokenResponse(response.headers, setAuthState)
  }, [])

  const logout = useCallback(() => {
    queryClient.clear()
    clearTokens()
    setAuthState(EMPTY_AUTH_STATE)
    navigate("/login")
  }, [navigate])

  const register = useCallback(async (data: RegisterData) => {
    await userApi.createUser(data)
    await login(data.email, data.password)
    return true
  }, [login])

  const handleTokensRefresh = useCallback((accessToken: string, refreshToken?: string) => {
    const storedAccessToken = sessionStorage.getItem("jwt")
    const storedRefreshToken = sessionStorage.getItem("refresh")

    if (
      accessToken === storedAccessToken &&
      (!refreshToken || refreshToken === storedRefreshToken)
    ) {
      return
    }

    storeTokens(accessToken, refreshToken)
    setAuthState(decodeAuthState(accessToken, refreshToken ?? storedRefreshToken ?? undefined))
  }, [])

  const value = useMemo(() => ({
    ...authState,
    loading,
    login,
    validateOTP,
    logout,
    register,
    handleTokensRefresh,
    syncAuthState,
  }), [authState, loading, login, validateOTP, logout, register, handleTokensRefresh, syncAuthState])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
