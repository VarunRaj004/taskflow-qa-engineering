import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../AuthContext'

export default function NavBar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="nav-bar">
      <div className="nav-links">
        <Link to="/dashboard">Dashboard</Link>
        <Link to="/tasks">Tasks</Link>
      </div>
      <div className="nav-user">
        {user && <span>{user.name}</span>}
        <button type="button" onClick={handleLogout}>
          Log out
        </button>
      </div>
    </nav>
  )
}
