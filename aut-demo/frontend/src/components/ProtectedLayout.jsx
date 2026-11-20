import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../AuthContext'
import NavBar from './NavBar'

export default function ProtectedLayout() {
  const { isAuthenticated } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return (
    <>
      <NavBar />
      <main className="page">
        <Outlet />
      </main>
    </>
  )
}
