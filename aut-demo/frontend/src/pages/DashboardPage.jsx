import { useEffect, useState } from 'react'
import { api, ApiError } from '../api'

export default function DashboardPage() {
  const [stats, setStats] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    api
      .dashboard()
      .then((data) => {
        if (active) setStats(data)
      })
      .catch((err) => {
        if (active) setError(err instanceof ApiError ? err.message : 'Failed to load dashboard')
      })
    return () => {
      active = false
    }
  }, [])

  return (
    <div>
      <h1>Dashboard</h1>
      {error && <p role="alert" className="form-error">{error}</p>}
      {stats && (
        <ul className="stats">
          <li>Total: {stats.total}</li>
          <li>To Do: {stats.todo}</li>
          <li>In Progress: {stats.inProgress}</li>
          <li>Done: {stats.done}</li>
        </ul>
      )}
    </div>
  )
}
