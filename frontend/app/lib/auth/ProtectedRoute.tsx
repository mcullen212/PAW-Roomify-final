import { Navigate, Outlet, useLocation } from "react-router"
import { useAuth } from "./useAuth"

export default function ProtectedRoute() {
  const { authenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return null
  }

  if (!authenticated) {
    const returnTo = `${location.pathname}${location.search}`
    return <Navigate to="/login" replace state={{ returnTo }} />
  }

  return <Outlet />
}
